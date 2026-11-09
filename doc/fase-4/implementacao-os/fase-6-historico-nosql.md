# Fase 6 - Histórico de revisões no DynamoDB

**Modelo** · GPT-6.1 Sol, High. **Entrada** · Conclusão técnica da fase 4 estável. Alinhar tabela e acesso com Marcelo. O [contrato atualizado](../contratos-saga-arquitetura-v2.md) define o DynamoDB como o uso de NoSQL da entrega. A projeção pode ser desenvolvida em paralelo à fase 5.

## Objetivo

Exibir revisões concluídas por veículo usando NoSQL de negócio, sem adicionar outro microsserviço e sem depender de escrita dupla RDS/DynamoDB no request.

## Implementações

1. Ao concluir o reparo, gravar OS `COMPLETED`, snapshot imutável e pendência de projeção no mesmo commit PostgreSQL. Snapshot inclui IDs, filial, data final, quilometragem quando informada, serviços executados e materiais efetivamente consumidos. Criar a entrada para manutenção executada.
2. Usar outbox local da OS para o histórico. O worker da própria OS grava direto no DynamoDB fora da transação PostgreSQL e marca a projeção concluída depois do sucesso. A única fila da Saga continua Billing → OS.
3. Tabela proposta `vehicle_maintenance_history`, prefixada por ambiente, exclusiva da OS. PK vehicleId; SK completedAt#serviceOrderId#historyVersion, com valores congelados no snapshot.
4. Escrita condicional/idempotente. Documento existente só é tratado como repetição se identidade, versão e conteúdo coincidirem; colisão diferente é falha visível. Retentativa não cria horário/chave novos.
5. API autenticada da OS consulta por vehicleId com Query e paginação. Validar acesso ao veículo/filial com as regras atuais. Não usar Scan global ou confiar em vehicleId enviado pelo cliente sem autorização.
6. Falha da projeção não desfaz reparo/entrega. Expor métrica de atraso/pendência e retentativas; não prometer histórico instantâneo enquanto worker ainda não publicou. Correção de revisão é nova versão explícita, não sobrescrita casual.
7. Preparar reprocessamento por snapshot, com contagens e idempotência. Um eventual backfill de revisões legadas depende da disponibilidade de dados e de escopo próprio.

Provisionamento da tabela fica com Marcelo/database-infra; código do adaptador, documentos e API ficam na OS. O modelo prepara a solicitação de IaC e testa localmente; não cria recursos cloud por conta própria.

## Testes e aceite

- [ ] Rollback da conclusão não deixa snapshot de revisão concluída.
- [ ] Queda após PutItem e antes de marcar outbox não duplica documento.
- [ ] DynamoDB indisponível permite concluir, pagar e entregar. A projeção fica pendente e retomável.
- [ ] Consulta é paginada, por veículo, sem acesso a dados de outro cliente.
- [ ] Cenário fictício de histórico consultável demonstrado; DynamoDB Local e evidência AWS são identificados separadamente.
- [ ] Matheus revisou formato, conteúdo mínimo e política de reprocessamento.

## Prompt específico

```text
Implemente somente a fase 6: projeção idempotente de revisões concluídas no
DynamoDB e consulta autorizada pela API da OS. Use snapshot + outbox PostgreSQL,
chave estável e escrita condicional; não assumir transação RDS/DynamoDB.
Não bloquear conclusão/entrega por falha da projeção. Prepare o pedido de
infra para Marcelo, teste falha/reentrega/paginação e pare para revisão.
```
