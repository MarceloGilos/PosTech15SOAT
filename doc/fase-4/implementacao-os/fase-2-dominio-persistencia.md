# Fase 2 · Adaptar o domínio e o PostgreSQL da OS

**Modelo** · GPT-6.1 Sol, High. **Entrada** · [Contrato atualizado](../contratos-saga-arquitetura-v2.md), revisão da fase 1B e checkout local da fase 2 indicado no [índice](README.md).

## Reaproveitar

Preservar clientes, veículos, catálogo técnico, diagnóstico, itens, estimativa identificada como estimativa, persistência PostgreSQL/Flyway, testes e segurança por cliente/filial. As relações JPA continuam locais. Materiais seguem como referências externas, com quantidades planejadas na OS. Preservar os dois commits e a documentação já preparada no Git.

## Ajustar

1. Remover a exigência financeira de `requestStart()`, `completeItem()` e `complete()`. O início exige orçamento vigente aprovado e, quando há materiais, consumo confirmado para marcar o item em execução. A conclusão exige execução válida e ausência de pendências de consumo aplicáveis. O reparo pode atingir OS `COMPLETED` antes da cobrança. A liberação de entrega usa CT-07 na fase 4.
2. Ajustar `MaterialAllocation.hasConfirmedConsumption()` e a constraint `ck_material_quantities` da V1. Hoje ambos vinculam consumo à reserva. Criar migration incremental que aceite consumo direto dentro da quantidade planejada e com referência auditável de operação/movimento. Revisar mapeamentos JPA, constraints, DTOs, tracking e testes que usam `reservationId` ou `reservedQuantity`. Preservar auditoria e validar os dados existentes antes de remover ou descontinuar colunas. Uma reserva antiga não comprova consumo.
3. Separar os status. A ordem comercial usa `APPROVED` conforme o catálogo do contrato. A prontidão `READY_FOR_EXECUTION` e o início pendente `START_PENDING` são técnicos. Tratar a exposição atual de `READY_FOR_EXECUTION` e `START_REQUESTED` nas APIs com uma regra de compatibilidade documentada e testar leitura de dados existentes. Operações da Saga entram nas fases 3 e 4.
4. Revisar o snapshot aprovado já implementado com `compositionVersion`, incluindo guardas de edição e testes de revisão obsoleta. Aplicar valor aprovado positivo conforme o guia de 07/11, preservando a leitura dos dados históricos.
5. Manter PostgreSQL isolado, migrations no repositório da OS e configuração externa. O Compose usa `service_order_db`. O nome proposto no RDS é `numberone_os`. Alinhar o nome remoto com Marcelo e parametrizar a conexão.
6. Preservar o bloqueio operacional do início integrado enquanto a fase 4 estiver pendente. A rota de entrega deve permanecer bloqueada para entrega real até que a checagem de quitação esteja disponível. Usar fixtures somente em ambiente de teste/desenvolvimento.

## Aceite

- [ ] Regressões de cliente, veículo, diagnóstico, acesso e estimativa continuam passando.
- [ ] PostgreSQL aceita consumo confirmado sem reserva e rejeita quantidades e referências inválidas. Validar banco novo e atualização a partir da V1.
- [ ] Testes demonstram conclusão técnica sem pagamento, bloqueio de conclusão com execução/consumo pendentes e proteção de snapshot aprovado.
- [ ] Status, OpenAPI e tracking são compatíveis com a decisão de migração. Documentar a integração que ainda falta.
- [ ] Entregar mapa das alterações, evidências novas de testes e cobertura e demonstração local para revisão.
