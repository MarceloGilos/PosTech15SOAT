# Exemplo: fluxo atual e fluxo distribuído de uma ordem de serviço

Este exemplo complementa a [proposta da fase 4 na AWS](proposta-equipe.md) e seu [anexo técnico](proposta-implementacao.md). A seção atual descreve o código consultado; o fluxo distribuído descreve o comportamento a implementar. Valores e identificadores abaixo são fictícios. `OS-123` é um nome de apresentação; os contratos reais continuam usando UUID.

## 1. Atendimento de exemplo

- Cliente e veículo previamente cadastrados.
- Diagnóstico: substituir as pastilhas de freio.
- Item de serviço: troca de pastilhas, mão de obra de R$ 150,00.
- Insumo: um kit de pastilhas, preço de venda de R$ 200,00.
- Total: R$ 350,00.
- Estoque inicial na filial: cinco kits, sem reservas anteriores.

No novo desenho, os três serviços são **OS**, **Billing** e **Inventory**. Diagnóstico e execução pertencem à OS; Inventory controla materiais e estoque.

## 2. Como funciona hoje

As operações abaixo são requisições distintas à mesma aplicação Spring Boot, com persistência no mesmo PostgreSQL. Não existe atualmente uma Saga coordenando automaticamente essa sequência.

| Passo | Operação atual | Resultado esperado no exemplo |
| --- | --- | --- |
| 1 | `POST /api/admin/ordens-servico` | `ServiceOrderService.createServiceOrder` valida cliente/veículo e salva a ordem como `RECEBIDA` |
| 2 | `PATCH /api/admin/ordens-servico/{id}/iniciar-diagnostico` | `addFinalDiagnosis` registra diagnóstico final/previsão e altera a ordem para `EM_DIAGNOSTICO` |
| 3 | `POST /api/admin/itens-ordem-servico` | Inclui o item de serviço da troca de pastilhas |
| 4 | `POST /api/admin/itens-ordem-servico/{itemId}/insumos` | Associa um kit ao item de serviço |
| 5 | `POST /api/admin/ordens-servico/{id}/orcamentos` | Cria orçamento; quando o valor não é informado, o cálculo atual soma item e insumos: R$ 350,00 |
| 6 | `PATCH /api/admin/orcamentos-ordem-servico/{orcamentoId}/solicitar-aprovacao` | Salva orçamento enviado, altera ordem para `AGUARDANDO_APROVACAO` e solicita notificação por e-mail |
| 7 | Aprovação do orçamento | `ServiceOrderBudgetService.approve` salva aprovação e atualiza diretamente a ordem para `APROVADA` |
| 8 | `PATCH /api/admin/ordens-servico/{id}/iniciar` | `startOrderService` altera a ordem para `EM_EXECUCAO`; essa ação não faz a baixa dos insumos |
| 9 | `PATCH /api/admin/itens-ordem-servico/{itemId}/iniciar` | `startServiceOrderItem` exige ordem em execução, verifica insumos, inicia o item e baixa um kit; saldo passa de cinco para quatro |
| 10 | Concluir item, concluir ordem e entregar | Chamadas aos endpoints de conclusão e entrega registram as transições e datas correspondentes |

Se algum insumo estiver indisponível na verificação do passo 9, o item passa a `AGUARDANDO_PECAS_E_INSUMOS`. Se uma falha for identificada durante a baixa, o gateway pode lançar uma exceção. O método de início do item está anotado com `@Transactional`; este exemplo não substitui um teste de concorrência das operações atuais.

Pagamento Mercado Pago, reserva antecipada e outbox são acréscimos propostos para a fase 4. Não fazem parte desse fluxo atual.

Código de referência:

- [ServiceOrderController](../../src/main/java/br/com/fiap/numberone/serviceorder/api/controllers/ServiceOrderController.java).
- [ServiceOrderService](../../src/main/java/br/com/fiap/numberone/serviceorder/application/services/ServiceOrderService.java).
- [ServiceOrderBudgetService](../../src/main/java/br/com/fiap/numberone/serviceorder/application/services/ServiceOrderBudgetService.java).
- [ServiceOrderItemService](../../src/main/java/br/com/fiap/numberone/serviceorder/application/services/ServiceOrderItemService.java).
- [InventoryWithdrawalGatewayImpl](../../src/main/java/br/com/fiap/numberone/serviceorder/infrastructure/persistence/gateways/InventoryWithdrawalGatewayImpl.java).

## 3. Como ficará em OS, Billing e Inventory

O orquestrador executa na OS. **Diagnóstico e execução continuam junto da ordem**, com chamadas locais e PostgreSQL da OS. Inventory mantém somente as regras de materiais; Billing mantém orçamento/pagamento. Mensagens entre aplicações usam SQS FIFO e outbox; entradas humanas são HTTP.

```mermaid
sequenceDiagram
    participant O as OS e orquestrador
    participant B as Billing
    participant I as Inventory
    Note over O: HTTP: abrir OS, diagnosticar e concluir composição
    O->>B: CreateBudget
    B->>O: BudgetSent
    Note over B: Cliente aprova pela API de Billing
    B->>O: BudgetApproved
    O->>I: ReserveMaterials
    I->>O: MaterialsReserved
    O->>B: RequestPayment
    Note over B: Cliente paga; Billing confere Mercado Pago
    B->>O: PaymentConfirmed
    Note over O: HTTP: solicitar início; salvar START_REQUESTED
    O->>I: ConsumeMaterials
    I->>O: MaterialsConsumed
    Note over O: Confirmar início e Saga; concluir reparo/entregar na OS
```

### Etapa A: abertura e diagnóstico locais na OS

OS valida cliente/veículo, abre a ordem e inicia o acompanhamento da Saga no seu PostgreSQL. A equipe registra diagnóstico, serviço e material previsto pela API da OS. Serviço/mão de obra pertencem ao catálogo da OS; peça/insumo e disponibilidade são referências do Inventory consultadas por API, sem acesso ao banco dele.

Ao concluir o diagnóstico, uma chamada local monta a proposta e avança a Saga. A mesma transação salva o diagnóstico e `CreateBudget` na outbox. Não envia `StartDiagnosis` para outro serviço nem precisa receber `DiagnosisCompleted` para conhecer seu próprio diagnóstico.

O comando leva composição proposta, destinatário mínimo, IDs e revisão: mão de obra R$ 150,00, um kit com referência de R$ 200,00. Billing calcula/preserva o total e a revisão comercial. Nenhum desses valores exige relacionamento JPA externo.

### Etapa B: orçamento e aprovação no Billing

Billing calcula R$ 350,00, salva orçamento e tarefa de envio, notifica o cliente e publica `BudgetSent`. OS atualiza acompanhamento. A aprovação pela API de Billing grava a aprovação e `BudgetApproved` juntos. O evento traz a composição/valores aprovados; a OS preserva esse snapshot e pede `ReserveMaterials` para a tentativa/revisão correta.

Isso substitui a escrita direta atual de `ServiceOrderBudgetService` no gateway da ordem. Se o preço de referência mudar depois, não alterar silenciosamente o orçamento aprovado.

### Etapa C: reserva no Inventory e pagamento no Billing

Inventory recebe filial, OS/Saga/tentativa, revisão e alocação do kit ao item de serviço. Em uma transação do seu PostgreSQL, valida saldo, reserva um kit e grava inbox/outbox com `MaterialsReserved`. Físico continua cinco; reservado fica um; disponível passa a quatro.

Somente após essa confirmação, OS publica `RequestPayment`. Billing prepara checkout, interage com Mercado Pago fora da transação SQL e confirma valor/moeda/referência. Após `PaymentConfirmed`, OS fica pronta para execução. Ainda não baixou o saldo físico nem iniciou o reparo.

### Etapa D: solicitação de início e consumo remoto

1. Equipe chama HTTP na **OS** para iniciar o item.
2. OS valida pagamento, reserva e estado, salva `START_REQUESTED` e `ConsumeMaterials` na mesma transação local. Retorna HTTP 202.
3. Inventory recebe o comando e consome a alocação uma única vez: físico cinco → quatro; reservado um → zero; disponível continua quatro. Movimento e `MaterialsConsumed` ficam na mesma transação.
4. OS recebe a confirmação e muda o item para execução. No primeiro início, ordem passa a `EM_EXECUCAO` e a Saga de liberação termina. A equipe pode começar o reparo.

| Momento | Físico | Reservado | Disponível |
| --- | --- | --- | --- |
| Antes da reserva | 5 | 0 | 5 |
| Depois de reservar | 5 | 1 | 4 |
| Depois de consumir | 4 | 0 | 4 |

Aqui está a principal diferença frente à proposta anterior: **a baixa e o início não cabem mais na mesma transação**, porque estão em serviços distintos. `@Transactional` na OS não controla Inventory. Por isso existe início pendente e confirmação por mensagem. Reentrega não pode duplicar o consumo. Timeout deixa o início pendente até reconciliação, sem mandar o mecânico iniciar nem presumir que o consumo falhou.

Para vários itens, reservar todo o material aprovado e consumir a alocação de cada item quando solicitado. Serviço sem material pode iniciar localmente após os mesmos guardas de pagamento/cancelamento, sem comando remoto vazio.

### Etapa E: conclusão, histórico NoSQL e entrega

Equipe conclui os itens e o reparo pela API da OS. A OS valida suas próprias informações, finaliza a ordem e grava snapshot para histórico na outbox PostgreSQL. Não espera `ExecutionCompleted` de outro serviço; ela é dona da execução.

Worker da OS grava no DynamoDB o documento da revisão do veículo, com OS, data, quilometragem, serviços e material consumido. Chave estável por veículo/OS/versão evita duplicação. Depois confirma a entrega dessa pendência no PostgreSQL. Queda entre gravação e confirmação provoca repetição segura.

A API de histórico da OS consulta o DynamoDB. Se a projeção estiver indisponível, o atendimento pode concluir/ser entregue e o histórico fica pendente de retentativa/alerta. Não existe transação conjunta RDS/DynamoDB. A entrega do veículo continua sendo uma transação local da OS.

## 4. Falha: pagamento recusado após reserva

Política deste exemplo: encerrar a tentativa, sem nova cobrança automática.

1. Billing confirma recusa, registra a falha e publica `PaymentFailed`.
2. OS valida a tentativa, bloqueia novos inícios, entra em compensação e envia `ReleaseMaterials`.
3. Inventory libera o kit reservado, fecha a tentativa e publica `MaterialsReleased`, junto com os efeitos no PostgreSQL. Físico continua cinco; reservado volta a zero; disponível volta a cinco.
4. OS recebe a confirmação e pede `CancelBilling`.
5. Billing encerra a tentativa financeira e publica `BillingCompensated` após resolver pendências externas.
6. OS recebe os aceites e cancela localmente a ordem/Saga, preservando motivo e histórico.

Não existe estorno de uma cobrança recusada. Se o resultado era desconhecido, Billing precisa conciliá-lo. Se o pagamento foi aprovado e o cancelamento ocorreu antes de consumir materiais, a sequência inclui liberação e estorno confirmado. Não concluir compensação só porque o estorno foi solicitado.

Liberação repetida não aumenta saldo duas vezes. Liberação recebida antes da reserva fecha a tentativa, bloqueando reserva atrasada. Se o material já foi consumido, Inventory responde `MaterialsAlreadyConsumed` e a OS registra pendência operacional: sem reposição ou reembolso automáticos.

## 5. Onde cada dado fica

| Serviço / armazenamento | Dados do exemplo |
| --- | --- |
| OS / PostgreSQL | Cliente, veículo, OS, diagnóstico, serviço, material previsto/aprovado, execução, histórico operacional e Saga |
| Billing / PostgreSQL | Orçamento R$ 350,00, aprovação, revisão, tentativa financeira e referências Mercado Pago |
| Inventory / PostgreSQL | Kit, fornecedor, saldo, reserva/alocação, movimento de consumo e controles da tentativa |
| OS / DynamoDB | Revisão concluída do veículo, com serviços e materiais efetivamente usados, consultável pela API da OS |

Os três serviços usam IDs/contratos comuns sem acessar bancos vizinhos. Cada PostgreSQL mantém sua inbox/outbox; a outbox da OS também protege a projeção eventual no DynamoDB. Provisionamento fica nos três repositórios de suporte descritos na proposta, com seis repositórios no total.
