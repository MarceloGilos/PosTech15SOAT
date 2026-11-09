# Fase 3 · Consumo SQS, inbox e base da Saga

**Modelo** · GPT-6.1 Sol, High. **Entrada** · Fases 1B e 2 revisadas. Usar o [contrato atualizado](../contratos-saga-arquitetura-v2.md) e o pacote de schemas ativo.

## Implementar na OS

1. Criar migrations de `service_order_saga` e `inbox_event`. Criar uma Saga por OS com estado técnico inicial `IN_DIAGNOSIS` na mesma transação da abertura, com estratégia documentada para ordens existentes. Usar versão ou lock para serializar mudanças da mesma ordem.
2. Configurar consumidor da fila Billing → OS por ambiente. Usar credenciais do ambiente, long polling, visibilidade e política de retry/DLQ alinhados com Marcelo. A integração desabilitada deve ficar explícita na configuração local.
3. Validar envelope/tipo/versão e despachar para o handler. Na mesma transação, registrar `eventId` único, hash, desfecho e mudança local. Duplicata idêntica é confirmável sem reaplicar. Mesmo ID com outro conteúdo é erro. Confirmar a mensagem ao SQS depois do commit. Falha transitória e mensagem inválida seguem as políticas de reentrega e DLQ.
4. Demonstrar a primeira fatia com `OrcamentoEnviado`: o evento associa o orçamento vigente e leva a Saga a `WAITING_APPROVAL`. O produtor pode ser uma fixture compatível enquanto Billing estiver indisponível. Habilitar consumo de eventos em ambiente compartilhado só quando os handlers necessários estiverem disponíveis.
5. Instrumentar `eventId`, `correlationId`, OS/Saga e contadores de falha, duplicação e atraso. Evitar payloads sensíveis em logs.

Julio implementa no Billing a outbox que grava a mudança comercial/financeira e o evento no mesmo commit. O worker publica e, se precisar repetir, preserva `eventId`. A OS tem uma outbox local para o histórico no DynamoDB na fase 6.

## Aceite

- [ ] Abertura cria Saga e primeira transição acontece por evento válido.
- [ ] Rollback permite reentrega. Queda após commit e antes do ACK preserva efeito único.
- [ ] Evento duplicado, obsoleto ou inválido segue o tratamento do contrato. Colisão de ID com conteúdo diferente é detectada.
- [ ] Evidência usa PostgreSQL e identifica transporte por emulador, fila dedicada ou mock. Integrar com o Billing real na fase 7.
