# Anexo técnico - Saga OS, Billing e Inventory na AWS

Complementa a [proposta para a equipe](proposta-equipe.md), atualizada em 31/10/2026. Nomes de recursos, estados e contratos são propostos; não representam implementação/provisionamento concluídos. Os exemplos mantêm Java/Spring como referência, permitindo outras tecnologias que respeitem os contratos.

## 1. Limites e contratos

- **OS:** clientes/veículos, catálogo de serviços/mão de obra, diagnóstico, execução, conclusão, entrega e Saga em `service_order_db`, PostgreSQL exclusivo.
- **Billing:** composição comercial, cálculo, orçamento, cobrança, pagamento e operações externas em `billing_db`, PostgreSQL exclusivo.
- **Inventory:** peças/insumos, fornecedores, saldo, reserva, consumo e movimentos em `inventory_db`, PostgreSQL exclusivo.
- **Histórico:** tabela DynamoDB `vehicle_maintenance_history`, de propriedade da OS; segundo armazenamento da OS, sem novo microsserviço.
- Entradas humanas e consultas usam HTTP; comandos/resultados transacionais entre serviços usam SQS. Diagnóstico e execução são chamadas locais da OS.
- `@Transactional` protege apenas um PostgreSQL local. Não existe transação conjunta entre bancos, RDS/DynamoDB, SQS ou Mercado Pago.
- IDs/snapshots substituem dependências JPA externas. Não compartilhar entidades nem exigir mesma linguagem/framework.

| Comando | Consumidor | Resultado de negócio |
| --- | --- | --- |
| `CreateBudget` | Billing | `BudgetSent`, ou falha de criação/notificação; depois `BudgetApproved`/`BudgetRejected` |
| `ReserveMaterials` | Inventory | `MaterialsReserved` ou `MaterialsReservationFailed` |
| `RequestPayment` | Billing | `PaymentPending`, depois `PaymentConfirmed`/`PaymentFailed` |
| `ConsumeMaterials` | Inventory | `MaterialsConsumed` ou `MaterialsConsumptionFailed` |
| `ReleaseMaterials` | Inventory | `MaterialsReleased` ou `MaterialsAlreadyConsumed` |
| `CancelBilling` | Billing | `BillingCompensated`, somente após resolver pagamento/cancelamento/estorno |

`StartDiagnosis`, `DiagnosisCompleted`, `AuthorizeExecution`, `ExecutionStarted` e `ExecutionCompleted` não são necessários como mensagens entre serviços. A OS registra essas transições localmente. Inventory não cria fila/trabalho de reparo; controla somente materiais.

Envelope v1: `messageId`, `sagaId`, `serviceOrderId`, `branchId`, `attemptId`, `type`, `schemaVersion`, `causationId`, `occurredAt` UTC e payload. Aprovação identifica revisão, composição e valores aprovados; reserva usa essa revisão e alocações por `serviceOrderItemId`. Consumo inclui `reservationId` e `operationId` estável. Respostas têm ID novo e mesma correlação. Dados pessoais mínimos ficam no payload necessário, nunca segredos no envelope/log.

Filas: `numberone-inventory-commands.fifo`, `numberone-billing-commands.fifo` e `numberone-order-results.fifo`, cada uma com DLQ FIFO. `MessageGroupId=sagaId`; deduplicação por `messageId`; inbox e unicidade de efeito persistentes continuam necessárias. O publicador preserva sequência por Saga; a fila não corrige comandos publicados fora de ordem. Sem ordem global entre filas.

Marcelo publica envelope e infraestrutura; os donos publicam schemas JSON e exemplos de sucesso/falha dos seus comandos/eventos. Alteração incompatível cria nova versão. Contratos não devem depender da linguagem escolhida.

## 2. Persistência local e estoque PostgreSQL

| Banco | Estruturas mínimas propostas |
| --- | --- |
| `service_order_db` | Cliente, veículo, catálogo de serviços, OS, diagnóstico, item de serviço, material previsto/snapshot aprovado, histórico de status, Saga, inbox e outbox |
| `billing_db` | Orçamento, itens comerciais, aprovação, pagamento, operação externa, notificação, inbox e outbox |
| `inventory_db` | Peça/insumo, fornecedor, saldo por filial/material, tentativa de reserva, reserva/alocações, movimentação, inbox e outbox |
| DynamoDB | Documento de revisão concluída por veículo/OS/versão; leitura pela API da OS |

Migrations de cada PostgreSQL ficam no repositório da aplicação, com usuário próprio. Provisionamento RDS, bancos/usuários, tabela DynamoDB e índices fica em database-infra. Acesso ao mesmo servidor físico não autoriza ler banco vizinho. Identificadores dos itens de serviço são referências externas no Inventory, sem foreign key para a OS.

### 2.1 Reserva atômica

1. Criar/obter a tentativa por chave única de Saga/tentativa/revisão e bloquear sua linha. Tentativa fechada não pode reservar de novo.
2. Deduplicar `messageId` na inbox, validar quantidades positivas e agregar pedidos do mesmo material/filial, preservando a alocação por item de serviço.
3. Bloquear os saldos em ordem determinística e validar `on_hand - reserved >= quantidade` para todos.
4. Atualizar `reserved`, criar reserva/alocações e salvar `MaterialsReserved` na outbox na mesma transação.
5. Se algum saldo faltar, nenhum é reservado. Registrar recusa/inbox/outbox sem efeitos parciais; se houve exceção SQL/rollback, fazer a gravação da recusa em nova transação com revalidação.

Exemplo ilustrativo no adaptador SQL; `reserveAll` é chamado pelo proxy transacional. Inicialização/bloqueio da tentativa, inbox, validação, outbox e mapeamentos foram resumidos. Exceção de falta de saldo deve causar rollback, não ser capturada dentro da transação para confirmar atualizações parciais.

```java
@Transactional
public void reserveAll(ReserveMaterials command) {
    if (!inbox.registerIfAbsent("reserve", command.messageId())) {
        return;
    }
    reservationGateway.lockOpenAttempt(command.attemptId());
    for (MaterialTotal item : aggregateAndSort(command.items())) {
        int changed = jdbc.update("""
            UPDATE inventory_balance
               SET reserved = reserved + ?
             WHERE branch_id = ? AND material_id = ?
               AND on_hand - reserved >= ?
            """, item.quantity(), command.branchId(),
                 item.materialId(), item.quantity());
        if (changed != 1) {
            throw new InsufficientStockException(item.materialId());
        }
    }
    Reservation reservation = reservationGateway.create(command);
    outbox.append(MaterialsReserved.from(reservation));
}
```

`aggregateAndSort` valida/agrupa e ordena chaves antes dos updates. `lockOpenAttempt` cria linha se necessário, trata a disputa de criação e bloqueia a tentativa; também protege cancelamento anterior e unicidade de reserva. Inbox usa inserção atômica como `INSERT ... ON CONFLICT DO NOTHING`, sem capturar violação de unicidade que deixe a transação PostgreSQL abortada. O desenho pode usar locks pessimistas ou condições/versões equivalentes. Deadlock/conflito técnico é retentável, não uma falsa falta de estoque.

### 2.2 Consumo, liberação e resultados incertos

Ao consumir, Inventory bloqueia tentativa/reserva, valida alocação `RESERVED` e operação única, reduz físico e reservado, grava movimento e `MaterialsConsumed` na mesma transação. Disponível não muda: o material já estava comprometido. Repetição semântica devolve o resultado anterior, mesmo com `messageId` diferente.

Ao liberar, transiciona somente alocações ainda `RESERVED` para `RELEASED`, reduz reservado e mantém físico. Fecha a tentativa para impedir uma reserva atrasada. Se não existe reserva, grava esse fechamento mesmo assim e responde idempotentemente. Se já há consumo, retorna `MaterialsAlreadyConsumed`, com detalhamento das alocações; não repõe automaticamente materiais usados.

OS serializa início/cancelamento por lock/versão local, mas isso não bloqueia o banco Inventory. Inventory serializa consumo/liberação pelo mesmo registro local. Se liberação vence, consumo é recusado; se consumo vence, cancelamento exige tratamento operacional. OS não libera a execução física antes da confirmação. Timeout de `ConsumeMaterials` mantém `START_REQUESTED`, reapresenta a mesma operação ou consulta resultado via contrato de reconciliação; não presume falha nem dispara reembolso cego.

O caminho automático de compensação é o anterior ao consumo. Correção de baixa/devolução após consumo exige operação explícita e auditável fora desse rollback automático. Estoque não controla autorização de reparo; apenas valida tentativa/reserva/alocação e estado permitido dos comandos.

## 3. Histórico de revisões no DynamoDB

O histórico é um dado funcional consultado por veículo, não apenas arquivo de logs. A OS permanece a fonte do atendimento; a projeção DynamoDB guarda uma visão de revisões concluídas, sem tomar decisões de reserva/pagamento/Saga.

| Elemento | Proposta inicial |
| --- | --- |
| Tabela | Prefixo ambiente + `vehicle_maintenance_history`, exclusiva da OS |
| Chave de partição | `vehicleId` estável |
| Chave de ordenação | `completedAt#serviceOrderId#historyVersion`; valores congelados no snapshot |
| Dados | IDs de OS/veículo/filial, data, quilometragem se informada, diagnóstico resumido, serviços executados e materiais consumidos |
| Consulta | `Query` por veículo, ordem cronológica e paginação; API OS valida acesso e retorna o histórico |
| Idempotência | Mesmo evento/versão sempre gera a mesma chave e conteúdo; escrita condicional impede duplicação |
| Correção | Nova versão explícita, ligada à anterior; não gerar novo horário/chave a cada retentativa |

Fechar primeiro os padrões de consulta. Índices extras só se houver necessidade funcional definida. Não fazer `Scan` global para montar o histórico de um veículo. Usar decimais exatos/centavos quando houver valores, IDs e dados mínimos; sem CPF, token ou dados de cartão.

### 3.1 Entrega confiável RDS → DynamoDB

1. Na transação de conclusão da OS, salvar status, histórico local e snapshot imutável na outbox com destino `DYNAMODB_HISTORY`.
2. Worker assume a pendência com lease no PostgreSQL e grava documento no DynamoDB fora dessa transação.
3. Usar `PutItem` condicionado à inexistência da chave. Caso já exista por retentativa, conferir identidade/versão/conteúdo antes de considerar entregue; colisão diferente é erro, não sucesso silencioso.
4. Após sucesso, marcar entregue no PostgreSQL. Queda antes dessa marcação provoca repetição segura.
5. Falha permanece pendente com retentativa/alarme. Consulta pode apresentar atraso; entrega/conclusão não é desfeita. Reprocessamento/backfill vem dos snapshots/dados da OS.

Não escrever PostgreSQL e DynamoDB diretamente no mesmo handler HTTP esperando atomicidade; `@Transactional` não cobre essa combinação. Não é necessário DynamoDB Streams, nova fila nem Lambda de negócio para essa projeção. A mesma tabela outbox pode distinguir destinos SQS/histórico, mas cada pendência possui confirmação própria. Leitura forte opcional da tabela não elimina o atraso antes de o worker gravar.

## 4. Todas as transações do atendimento

Cada linha representa uma transação local ou gravação explicitamente delimitada. Recepção SQS inclui inbox, validação de etapa/tentativa/versão e outbox quando há resposta. Cadastros/fornecedores/ajustes são CRUD próprios fora da Saga.

| ID | Operação local | Banco | Saída / pendência |
| --- | --- | --- | --- |
| T01 | Abrir OS, histórico e estado inicial da Saga | PostgreSQL OS | Ordem recebida; sem mensagem de diagnóstico |
| T02 | Iniciar diagnóstico | PostgreSQL OS | Status local de diagnóstico |
| T03 | Registrar/editar diagnóstico, serviços e materiais previstos | PostgreSQL OS | Composição proposta local |
| T04 | Concluir diagnóstico/congelar proposta e avançar Saga | PostgreSQL OS | `CreateBudget` |
| T05 | Criar orçamento e calcular itens/total | PostgreSQL Billing | Tarefa persistente de envio |
| T06 | Registrar aceitação de envio pelo provedor | PostgreSQL Billing | `BudgetSent` |
| T07 | Receber envio do orçamento | PostgreSQL OS | Aguardando aprovação |
| T08 | Aprovar orçamento via HTTP | PostgreSQL Billing | `BudgetApproved` |
| T09 | Receber aprovação/snapshot e pedir reserva | PostgreSQL OS | `ReserveMaterials` |
| T10 | Reservar todos os materiais/alocações | PostgreSQL Inventory | `MaterialsReserved` |
| T11 | Receber reserva e pedir cobrança | PostgreSQL OS | `RequestPayment` |
| T12 | Criar tentativa financeira/operação externa | PostgreSQL Billing | Worker cria checkout fora da transação |
| T13 | Registrar retorno/URL do checkout | PostgreSQL Billing | `PaymentPending` |
| T14 | Atualizar acompanhamento financeiro | PostgreSQL OS | Prazo de pagamento ativo |
| T15 | Validar/persistir recebimento de webhook | PostgreSQL Billing | Worker consulta provedor fora da transação |
| T16 | Confirmar valor/moeda/referência e pagamento | PostgreSQL Billing | `PaymentConfirmed` |
| T17 | Receber confirmação financeira | PostgreSQL OS | Pronta para execução; encerrar prazo financeiro |
| T18 | Solicitar início do item via HTTP | PostgreSQL OS | `START_REQUESTED` + `ConsumeMaterials`; HTTP 202 |
| T19 | Consumir alocação da reserva e gravar movimento | PostgreSQL Inventory | `MaterialsConsumed` |
| T20 | Receber consumo; confirmar início do item | PostgreSQL OS | Primeiro item: ordem em execução e Saga liberada |
| T21 | Concluir item pela API | PostgreSQL OS | Status do item; repetir T18-T21 para outros itens |
| T22 | Validar conclusão de todos os itens e finalizar reparo | PostgreSQL OS | OS finalizada + snapshot de revisão na outbox |
| T23 | Projetar documento com chave idempotente | DynamoDB OS | Revisão consultável por veículo |
| T24 | Confirmar entrega da projeção | PostgreSQL OS | Outbox do histórico marcada entregue |
| T25 | Registrar entrega do veículo via HTTP | PostgreSQL OS | Data/hora, entregue e histórico local |
| T26 | Persistir recepção/processamento de falha de consumo | PostgreSQL OS | Pendência/reconciliação ou compensação conforme resultado confirmado |

T23/T24 não bloqueiam T25. Não existe dependência de tempo rígida entre projeção e entrega. T26 é caminho alternativo, não passo obrigatório do fluxo feliz. Itens sem materiais pulam T18/T19 remotos: OS confirma o início localmente, com mesmos guardas financeiros/cancelamento. Primeiro início encerra a Saga de liberação; consumos de itens posteriores continuam com operação/correlação próprias, sem repetir cobrança.

Publicação SQS tem transações técnicas adicionais de lease/confirmar envio no banco do serviço. E-mail/Mercado Pago, espera humana e SQS ficam fora das transações de negócio. Resultado atrasado não regride estado, incluindo `PaymentPending` após `PaymentConfirmed`.

### 4.1 Compensações

| ID | Operação local | Banco | Resultado |
| --- | --- | --- | --- |
| C01 | Receber rejeição/falha/prazo confirmado, bloquear novos inícios e marcar compensação | PostgreSQL OS | `ReleaseMaterials` para a tentativa |
| C02 | Fechar tentativa e liberar somente o não consumido | PostgreSQL Inventory | `MaterialsReleased`; se consumido, `MaterialsAlreadyConsumed` e pendência operacional |
| C03 | Receber liberação confirmada e pedir limpeza financeira | PostgreSQL OS | `CancelBilling` |
| C04 | Bloquear tentativa/preparar cancelamento ou estorno | PostgreSQL Billing | Operação externa persistida; provedor fora da transação |
| C05 | Registrar resultado externo e reconciliar até final | PostgreSQL Billing | `BillingCompensated` somente quando resolvido |
| C06 | Receber todos os aceites e cancelar/rejeitar OS localmente | PostgreSQL OS | Saga compensada, motivo e histórico |

Pagamento recusado não exige estorno. Resultado externo desconhecido precisa de reconciliação; pagamento tardio de tentativa encerrada não reativa reparo e pode exigir estorno. Reserva inexistente e cobrança não iniciada devem aceitar limpeza idempotente. Falhas transitórias são retentadas; não compensar imediatamente por uma indisponibilidade temporária. Se o consumo já ocorreu, não encerrar falsamente a Saga como compensada.

## 5. Infraestrutura, autenticação e seis repositórios

| Repositório de suporte | Recursos sob sua gestão | Outputs/consumidores |
| --- | --- | --- |
| `numberone-aws-infra` | VPC/subnets, EKS/ECR, API Gateway/VPC Link/ALB, SQS/DLQs, roles IAM/Pod Identity, Secrets Manager e observabilidade | Rede/filas/roles para bancos, auth e apps; ARN das Lambdas consumido em integração posterior |
| `numberone-database-infra` | RDS/bancos/usuários, DynamoDB/índices/backups; consome rede existente | Endpoints, nomes de bancos/tabela e referências a segredos para apps; nunca senha em outputs públicos |
| `numberone-auth-lambdas` | Código/testes/deploy e funções/permissions de autenticação/autorização | ARN versionado para API Gateway; sem duplicar recurso função em outro estado |

Os três repositórios de aplicação mantêm código, migrations, Dockerfile, manifestos, pipeline e README próprios. Marcelo fornece templates/ambiente mínimo; cada dono mantém seu pipeline e contratos. Proteção de main/PR/checks vale para aplicações; aplicar também nos repositórios de suporte.

Ordem inicial: base de rede/identidades → bancos e funções de auth em paralelo → integração Gateway/authorizer usando outputs → deploy das aplicações/smoke. Usar etapas/módulos no AWS-infra para não exigir função ainda inexistente ao criar a base. Nenhum recurso deve ser criado/alterado por dois estados IaC diferentes. Configuração de banco exclusivo e grants verificáveis sustenta a independência mesmo com IaC centralizado.

Entrada proposta: API Gateway HTTP API → VPC Link → ALB interno → serviços EKS. Validar listener/rotas/path mapping e conectividade antes de integrar negócio. O authorizer decide acesso pelo token; apps validam permissões de negócio. Rotas de webhook usam validação do provedor no Billing. Definir contrato de token/claims, expiração, emissão e rotas públicas logo no início, reaproveitando fase 3; não inventar novo cadastro de identidade durante a extração.

- **Identidade AWS:** role por ServiceAccount, SDK/agente compatíveis com Pod Identity. CI usa OIDC limitado a repositório/branch e permissões Kubernetes necessárias.
- **Segredos:** credenciais SQL, segredos JWT/webhook e Mercado Pago fora do Git/logs; documentar um mecanismo de carga Secrets Manager. Autenticação do funcionário é distinta de IAM dos pods.
- **Filas:** long polling, visibilidade, extensão, retenção e DLQs; Delete após commit; reprocessamento preserva identidade e respeita etapa.
- **Retenção:** inbox/chaves de operação cobrem horizonte de reprocessamento. Histórico de manutenção não recebe TTL por padrão. TTL não agenda cancelamento.
- **Deploy:** testes/cobertura/Sonar, imagem ECR, migration compatível, rollout EKS e smoke. Rollback de imagem não desfaz dados nem pagamentos.
- **Observabilidade:** ordem/Saga/messageId nos logs; tracing HTTP/SQS; alarmes de DLQ, idade da outbox/Saga, estorno e projeção de histórico pendente.
- **Rede/custos:** validar saída para AWS APIs/Mercado Pago e recursos da fase 3. Dimensionar homologação; não introduzir plataforma de eventos ou processamento extra sem necessidade.

## 6. Validação e migração

Preservar IDs e usar carga fictícia conciliada em homologação. Cliente/veículo/OS/diagnóstico/itens/catálogo de mão de obra ficam na OS; orçamento vai para Billing; materiais/fornecedores/saldos/movimentações vão para Inventory. O antigo gateway de baixa direta deve virar contrato remoto, sem transação JPA cruzada. Histórico DynamoDB pode receber backfill das revisões concluídas pela mesma projeção idempotente.

Scripts conferem contagens, saldos, totais e referências. Pausar escrita no monólito antes da carga/virada de teste. Retorno ao monólito após operações novas exige reconciliação. Escolhas de linguagem/banco relacional alternativo devem preservar contratos/aceites e ser registradas; o baseline aprovado aqui mantém PostgreSQL para reduzir migração.

Testes prioritários:

- Duas ordens disputam a última unidade: só uma reserva é confirmada.
- Reentrega/reinício não duplica reserva, movimento, cobrança nem documento de histórico.
- Pagamento recusado libera reserva e encerra finanças antes de concluir compensação.
- Pagamento confirmado seguido de cancelamento antes do consumo: liberar e estornar com evidência.
- Consumo e liberação concorrentes: só uma transição vence; timeout não inicia reparo nem provoca reembolso indevido.
- Falha do DynamoDB: OS conclui/entrega, outbox fica pendente e histórico aparece após recuperação.
- Histórico é consultável pela API OS com acesso autorizado ao veículo; auth e webhook têm testes próprios.

Cobertura mínima de 80% em cada aplicação, unitários, integração de persistência, BDD completo, Sonar/CI e deploy Kubernetes. DynamoDB Local ajuda nos testes; evidência AWS deve cobrir IAM, filas, projeção e entrada autenticada. Mercado Pago usa conta de teste, além do simulador no CI.

## 7. Referências

Requisitos: **SOAT - Fase 4 - Tech challenge.pdf**, páginas 2-6; a divisão Execução/Produção é exemplo sugerido, não o único recorte possível. Checklist completo e responsáveis estão na proposta principal.

- [Outbox transacional AWS](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/transactional-outbox.html).
- [FIFO e grupos](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/FIFO-queues-understanding-logic.html).
- [Visibilidade e exclusão SQS](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-visibility-timeout.html).
- [Lambda authorizer](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-lambda-authorizer.html).
- [Integração privada API Gateway](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-integrations-private.html).
- [Pod Identity](https://docs.aws.amazon.com/eks/latest/userguide/pod-identities.html).
