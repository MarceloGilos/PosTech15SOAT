# Revisão da arquitetura de referência do Julio

**Data:** 02/11/2026. **Escopo:** comparar a mensagem enviada à equipe e o PDF de 33 páginas, versão 1.0 de 01/11/2026, com a última proposta e a especificação da OS preparadas neste projeto. Esta é uma análise; não aprova novas decisões nem modifica os documentos anteriores.

## 1. Parecer

O documento é uma boa base de organização funcional: separa responsabilidades, descreve os casos de uso, relaciona endpoints existentes, propõe contratos e identifica pendências. A divisão OS/Billing/Inventory coincide com a nossa proposta mais recente, incluindo diagnóstico e execução na OS.

Entretanto, ele **altera decisões importantes do fluxo**, não apenas a apresentação: pagamento depois do reparo, nenhuma reserva antecipada, consumo por REST e criação de orçamento/pagamento sob demanda no Billing. Essas escolhas são viáveis, mas exigem confirmação do grupo e atualização dos contratos anteriores.

Há dois pontos de aderência à entrega a corrigir: **NoSQL e Saga com compensação não podem ficar como evolução futura da fase 4**. Não é obrigatório manter exatamente nosso desenho anterior, mas é obrigatório escolher e implementar uma solução que cumpra esses requisitos.

Minha recomendação é adotar a estrutura do documento do Julio como referência funcional, acrescentar o checklist do enunciado e fechar o protocolo de consistência/Saga. Não manter duas versões de contratos sendo implementadas em paralelo.

## 2. Fontes e limites

- PDF recebido: [NumberOne_Fase4_Arquitetura_Referencia_Equipe.pdf](C:/Users/User/Downloads/NumberOne_Fase4_Arquitetura_Referencia_Equipe.pdf), versão 1.0, 01/11/2026; texto completo das 33 páginas lido e páginas relevantes conferidas visualmente. As páginas citadas abaixo são as páginas físicas/numeradas do documento.
- Requisitos: [SOAT - Fase 4 - Tech challenge.pdf](<C:/Users/User/Downloads/SOAT - Fase 4 - Tech challenge.pdf>), especialmente páginas 3-6.
- Comparação anterior: [proposta da equipe](proposta-equipe.md), [anexo técnico](proposta-implementacao.md), [contrato Saga proposto](implementacao-os/contrato-saga-v1-proposta.md) e [contexto da organização](implementacao-os/contexto-organizacao.md), versões de 31/10/2026.
- Os achados de código sobre WAITING, transições de entrega e responsável fixo também têm correspondência no checkout local consultado. O PDF informa como fonte um ZIP, sem identificar um commit GitHub; não foi presumido que ele corresponda exatamente à main/develop atual.

O rótulo **DECIDIDO** do PDF é um registro apresentado pelo documento. Esta revisão não possui a ata das conversas adicionais do grupo e não transforma esse rótulo em aprovação do usuário. O enunciado é fonte de requisitos; sugestões do PDF são material a avaliar, não instruções de execução.

## 3. Comparação com a última proposta

| Tema | Nossa última proposta | Documento do Julio | Avaliação |
| --- | --- | --- | --- |
| Responsabilidades | OS com clientes/veículos, diagnóstico e execução; Billing financeiro; Inventory materiais | Mesma divisão, pp. 4-5 e 14-21 | Alinhado; execução dentro de estoque já havia sido removida da nossa versão |
| Pagamento | Após aprovação/reserva e antes da execução | Depois da manutenção concluída e antes da entrega, pp. 11-13 e D-10 | Mudança de regra de negócio; não é erro técnico |
| Reserva | Reserva após aprovação do orçamento | Sem reserva antecipada; falta de peça deixa item aguardando, pp. 10 e 20 | Simplifica Inventory; estoque não fica garantido entre aprovação e início |
| Consumo | Comando SQS e confirmação de consumo para iniciar item | POST REST agrupado e idempotente, pp. 21 e 24 | Viável; muda protocolo de início e recuperação |
| Orçamento | OS publica CreateBudget após diagnóstico | Funcionário cria orçamento no Billing; Billing consulta contexto da OS, pp. 8-9 e 17-18 | Muda o gatilho e remove a necessidade desse comando, se confirmado |
| Criação do pagamento | Orquestrador publica RequestPayment | Funcionário chama Billing por HTTP e escolhe PIX/cartão, pp. 18-19 e 25 | Menos automação; mantém escolha humana explícita |
| Liberação para executar | Pagamento confirmado era guarda do início | Aprovação do orçamento libera execução; pagamento passa a guarda da entrega | A regra antiga não pode continuar nos testes/handlers da OS |
| Entrega | Guardas locais e confirmação financeira recebida no fluxo | Consulta oficial de quitação ao Billing em cada entrega, pp. 12-13 e 25 | Regra clara; indisponibilidade Billing passa a bloquear entrega |
| Saga | Estado durável, transações e compensações definidos | Saga detalhada adiada; PF-06 fora do escopo atual, pp. 29-30 | Lacuna obrigatória da entrega |
| NoSQL | DynamoDB para histórico consultável de revisões na OS, mantendo três PostgreSQL | Histórico NoSQL futuro e tecnologia Inventory ainda aberta, pp. 6, 21, 29-30 | É preciso garantir pelo menos um uso NoSQL na entrega |
| Orçamento manual | Preservação das regras atuais, mas contratos focados na composição calculada | Preserva valor manual ou cálculo automático, p. 17 | Detalhe útil a incorporar no contrato definitivo |
| Nova revisão após rejeição | Uma revisão aprovada por tentativa; reabertura limitada | Preserva possibilidade de novo orçamento; unicidade pendente, p. 17 | Definir orçamento vigente e rejeitar eventos de revisão anterior |
| Eventos | Nomes em inglês, envelope com IDs da Saga/tentativa/operação | OrcamentoAprovado/Rejeitado e PagamentoConfirmado, com eventId/eventType, pp. 23 e 26 | Nome é escolha; misturar formatos na implementação causa incompatibilidade |
| AWS | Handoff posterior já ajustado à organização: NLB, auth, Datadog e Academy | Preserva a base conceitual fase 3, p. 27 | Alinhado com o handoff; difere do desenho genérico mais antigo com ALB/OIDC |
| Planejamento | Sete fases da OS com revisão, testes e aceites | Plano conjunto por contratos/bases/domínio/interações/pagamento/robustez, p. 31 | Complementares, após atualizar as fases afetadas |

## 4. Correções necessárias para atender à fase 4

### 4.1 NoSQL precisa aparecer no escopo de entrega

Na p. 6, o PDF coloca a projeção NoSQL como evolução, não pré-requisito. Na p. 30, PF-05 mantém histórico NoSQL como futuro. O enunciado, p. 3, exige pelo menos um SQL e um NoSQL.

**Não é obrigatório que o histórico seja o uso NoSQL. É obrigatório que algum uso NoSQL seja implementado.** Inventory poderia usar NoSQL, desde que a equipe escolha e desenvolva isso; se OS, Billing e Inventory ficarem todos em PostgreSQL e o histórico for adiado, o requisito não estará coberto.

Como a preferência anterior era preservar o RDS do Inventory, recomendo manter o pequeno histórico DynamoDB da OS: concluir revisão → snapshot/outbox PostgreSQL → projeção idempotente → consulta por veículo. Essa projeção não precisa bloquear execução, pagamento ou entrega e não exige um quarto microsserviço.

### 4.2 Saga e compensações não podem ser somente PF-06

O enunciado, p. 4, pede Saga, rollback/compensação e justificativa no README; o vídeo e o PDF final também precisam demonstrar/descrever essa estratégia. O documento adia a Saga nas pp. 29-30 e não define uma sequência compensável completa no plano de integração.

Eventos, outbox, retry e operação idempotente são componentes úteis, mas não demonstram sozinhos uma Saga com tratamento de falhas. É necessário explicitar etapas locais, responsável pela coordenação, andamento durável, respostas de falha, recuperação e compensações aplicáveis.

Isso **não exige recolocar a reserva ou o pagamento antecipado**. Também não exige usar SQS para toda chamada, Step Functions ou um novo serviço de orquestração. Um fluxo híbrido pode coordenar etapas REST e eventos; é preciso definir sua consistência e demonstrar uma compensação real no processo escolhido. Referência técnica: [Saga na AWS](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/saga-orchestration.html).

### 4.3 Reaproveitar o checklist completo da entrega

O capítulo 12 traz bons testes de integração e critérios de PR. Ele não substitui o checklist formal: 80% por serviço, ao menos um BDD completo, Sonar/equivalente no CI, proteção de main, artefatos de cada repositório, deploy Kubernetes, vídeo até 15 minutos e PDF do portal.

Recomendo anexar a seção 8 da proposta anterior ao documento único, ajustando responsáveis e novos fluxos. Evita tratar o PDF de arquitetura como se já comprovasse todos os entregáveis.

## 5. Mudanças de negócio que podem ser adotadas

**Pagamento depois do reparo.** É coerente com o fluxo de oficina descrito pelo Julio e reduz a necessidade de estornar porque faltou peça antes da execução. A contrapartida é que o serviço já estará realizado se o cliente não pagar; nesse caso, bloquear a entrega é regra de negócio, não rollback do reparo. Não repor automaticamente materiais usados por falha financeira.

**Sem reserva antecipada.** Reduz estados, prazos e liberação de reservas. A aprovação do orçamento não promete disponibilidade futura. O Inventory precisa consumir atomicamente no início, e a OS precisa representar espera por peças e impedir conclusão com itens aguardando. Essa decisão deve ser explícita nos testes e na comunicação funcional.

**Consumo síncrono.** É válido para resposta rápida ao início do item. Ainda são duas transações de banco. Usar REST não elimina a operação pendente nem permite envolver as duas aplicações em um @Transactional.

**Quitação REST na entrega.** Evita que atraso do evento impeça reconhecer um pagamento já registrado no Billing. O custo é a dependência de disponibilidade do Billing na entrega. A projeção paymentStatus continua informativa. Não há transação distribuída entre GET de quitação e gravação da entrega: definir se o resultado vale como autorização naquele instante e o tratamento de futuras reversões, sem adicionar um protocolo extra se isso estiver fora do MVP.

**Orçamento/pagamento sob demanda no Billing.** A tela da OS pode chamar a API do Billing; não precisa encaminhar tudo pelo backend OS. Esse caminho reduz comandos da proposta anterior, mas precisa atualizar a especificação para ninguém implementar simultaneamente automação por SQS e criação manual duplicada.

Essas alternativas são escolhas do grupo. A reserva e a cobrança antecipada da proposta anterior eram opções de arquitetura/negócio, não exigências do enunciado.

## 6. Fronteiras técnicas a fechar antes da implementação cruzada

### 6.1 Resultado incerto de consumo: prioridade alta

As pp. 10, 21 e 24 reconhecem o caso Inventory consumiu, mas OS não confirmou. A p. 31 cita consulta por operationId, porém o capítulo de contratos não define esse endpoint/resposta.

Proposta mínima para fechar com Anderson:

1. OS salva operação de início pendente e operationId antes da chamada remota; não mantém transação SQL aberta durante REST.
2. Inventory grava consumo, movimentos e resultado idempotente na mesma transação local.
3. OS só confirma o item em execução depois de resultado confirmado.
4. Timeout/erro de conexão mantém operação incerta. OS consulta a operação ou repete com a mesma identidade; não cria outra baixa nem muda automaticamente para falta de peça.
5. Estado desconhecido/não encontrado não prova que a requisição jamais será aplicada. Fechar o contrato de consulta, replay, retenção e retomada por worker.

Não é obrigatório expor exatamente o enum START_REQUESTED, mas é necessário representar essa pendência duravelmente.

### 6.2 Idempotência precisa de mais do que um índice único

O contrato precisa decidir:

- Mesma chave + mesmo conteúdo: retornar resultado anterior.
- Mesma chave + conteúdo diferente: conflito, sem nova baixa/cobrança.
- Mesma operação de negócio + UUID novo por duplo clique: proteção também pela referência do item/OS, não apenas pelo UUID fornecido.
- Consumo rejeitado por falta de saldo: replay devolve a recusa anterior ou existe uma nova tentativa explícita após reposição? Nunca regenerar chave automaticamente para resultado desconhecido.
- Onde persistem estado/resultado/retentativa em OS e Inventory; não basta guardar o ID só no processo HTTP.

Essa é uma pendência concreta do contrato, não uma crítica ao uso de REST.

### 6.3 Saga enxuta compatível com o fluxo novo

Um candidato a validar é o início do item: OS registra intenção → Inventory aplica baixa → OS confirma início. Falhas técnicas recuperáveis seguem por reconciliação. Uma impossibilidade definitiva antes do uso físico pode exigir devolução/reversão auditável do material, **somente com regra de negócio e condições que assegurem que ele não foi usado**.

Uma reversão assim precisa de operação própria, idempotência e proteção contra comando atrasado que refaça a baixa. Depois de reparo/uso físico, a compensação não pode fingir que as peças voltaram ao estoque. O grupo deve confirmar se esse fluxo atende às transações críticas escolhidas para a demonstração ou definir outra Saga que cubra orçamento/OS. Esta revisão não aprova automaticamente esse desenho.

P10 já levanta a questão de devolução no PDF. A complementação é transformar a decisão em etapas/contratos/testes e em parte obrigatória da entrega, sem chamar apenas retry de compensação.

### 6.4 Orçamento enviado, preço e revisão

- **P13:** aproveitar o evento de envio de orçamento, equivalente ao BudgetSent anterior, para OS chegar a WAITING_APPROVAL. Definir payload, envio aceito versus orçamento aprovado, duplicação e atraso; evitar novo endpoint de escrita externa sem necessidade.
- **P08/P12:** manter Inventory como fonte do preço-base da peça, OS como dono do catálogo técnico e Billing como fonte do valor comercial aprovado. Consulta em lote de preços é razoável; Billing congela os preços usados no orçamento e não recalcula depois da aprovação.
- **P05/P07:** se novo orçamento/revisão for permitido, definir qual está vigente, quando itens ficam congelados e como eventos antigos são descartados/recuperados. Exemplo com budgetVersion não encerra a semântica dessa versão.
- Se todos os itens forem cancelados mas a OS ainda puder concluir, fechar o valor devido e o orçamento aplicável antes da cobrança; não inferir automaticamente valor zero nem o total anterior.

O documento já reconhece boa parte dessas dúvidas. Minha recomendação é encerrar as decisões antes de gerar os schemas definitivos.

### 6.5 Identidade e plataforma

O PDF acerta ao remover o UUID fixo do responsável na baixa, identificado também no código local. responsibleUserId deve vir da identidade validada, não ser aceito do cliente sem verificação. Chamadas entre serviços precisam de mecanismo definido com Marcelo.

Continua pendente a fronteira do login existente: a Lambda auth da fase 3 consulta cliente no RDS. Isolar o banco OS exige mudar esse acesso por contrato/projeção de propriedade adequada, como registrado no handoff anterior. Isso não aparece resolvido no documento do Julio.

Também confirmar nomes de filas/consumidores e seus outputs. O desenho anterior usava três filas de comandos/resultados; o novo usa principalmente eventos Billing → OS. Não manter filas/comandos desnecessários apenas para coincidir com os nomes antigos. Escolher Standard/FIFO conscientemente, mantendo deduplicação, versão, confirmação após commit e testes de recuperação.

Outbox/inbox propostos pelo Julio devem ser preservados. A outbox protege persistência/publicação; ela não cria atomicidade entre bancos de serviços. Referência: [Transactional Outbox na AWS](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/transactional-outbox.html).

## 7. O que a mensagem resumida não deixa tão claro

A mensagem está alinhada aos donos e ao uso de REST/eventos do PDF. Ela não evidencia duas alterações centrais que estão no documento: pagamento somente depois de COMPLETED e exclusão da reserva antecipada. Vale destacá-las no alinhamento para que Matheus e Anderson não comecem pelo contrato anterior.

Fornecedores aparece como operação no resumo enviado, mas as pp. 20-21 colocam esse módulo como evolução/escopo adicional ainda a especificar. Fechar se ele entra no MVP. Isso é uma divergência de escopo entre resumo e PDF, não uma obrigação adicional criada pelo enunciado.

O documento usa categorias ATUAL/DECIDIDO/PROPOSTO/PENDENTE que ajudam a revisão. Para as decisões D-07, D-10 e D-12, registrar a confirmação do grupo e a versão que substituem. O fato de o PDF possuir esse rótulo não altera sozinho a especificação anterior.

## 8. Impacto nas sete fases da OS

| Fase anterior | Impacto se o grupo adotar o fluxo do Julio |
| --- | --- |
| 1 - Repositório/contratos | Manter o bootstrap; refazer os contratos antes de congelá-los. Retirar reservas/comandos financeiros automáticos onde não se aplicarem; incluir contexto de orçamento, consumo/reconciliação REST, settlement e Saga definida |
| 2 - Domínio/persistência | Reaproveitar quase toda a extração. Rever guarda de pagamento no início, estados de consumo pendente, validação de WAITING e regra de entrega/cancelamento |
| 3 - Mensageria | Manter inbox/outbox e transporte, ajustando mensagens/consumidores ao novo fluxo. Persistência de operações REST pode usar worker/registro próprio, sem forçar toda integração ao SQS |
| 4 - Saga principal | Redesenhar: pagamento não libera execução. Aprovação libera início, consumo confirma material e pagamento passa a etapa posterior ao reparo |
| 5 - Compensação | Retirar liberação de reserva inexistente e o pressuposto de estorno antes do reparo; definir Saga/compensações verdadeiras do novo processo |
| 6 - Histórico NoSQL | Manter para cumprir o requisito caso Inventory permaneça PostgreSQL; escolher outro uso NoSQL explicitamente se for substituir |
| 7 - Integração/entrega | Atualizar BDD, teste de quitação REST, falha no consumo, pagamento depois do reparo, Saga/NoSQL e checklist formal |

Não executar o contrato antigo da fase 1 como se fosse compatível com o novo PDF. Já é possível revisar o bootstrap mínimo e a extração local, mas integrações dependentes das mudanças precisam de uma referência única aprovada.

## 9. Proposta de alinhamento para a equipe

1. Confirmar o novo fluxo de negócio: pagamento após reparo, sem reserva, consumo REST e quitação REST na entrega.
2. Fixar onde entra o NoSQL na entrega; sugestão com menor mudança sobre o RDS desejado: histórico DynamoDB na OS.
3. Definir a Saga escolhida e sua compensação demonstrável; remover PF-06 do futuro para esse recorte obrigatório.
4. Fechar P08/P13, revisão vigente, operação incerta de consumo e identidade interna.
5. Gerar schemas/fixtures/OpenAPI únicos e atualizar a fase 1 da OS e o BDD.
6. Anexar o checklist formal, com nomes e evidências, ao documento de referência único.

## 10. Texto sugerido para responder ao Julio

> Julio, a organização por casos de uso e por serviço ajuda bastante e está alinhada à divisão OS/Billing/Inventory que fechamos. O PDF também muda pontos do fluxo anterior: pagamento após o reparo, retirada da reserva e consumo via REST. Acho viável simplificar assim, desde que todos confirmem essas regras e atualizemos os contratos antes de implementar.
>
> Eu ajustaria dois pontos da entrega: o enunciado exige NoSQL e Saga com compensações, então esses itens não podem ficar como evolução futura. Podemos preservar o fluxo que você propôs e definir uma Saga enxuta, além de manter o histórico no DynamoDB para aproveitar o RDS atual do Inventory.
>
> Antes de congelar os contratos, precisamos fechar também o evento de envio do orçamento, a origem/snapshot dos preços e a recuperação quando Inventory consome, mas OS não recebe a resposta. Com esses ajustes e o checklist formal anexado, podemos usar esse material como referência única.

Esse texto foi apenas preparado nesta revisão; não foi enviado à equipe.
