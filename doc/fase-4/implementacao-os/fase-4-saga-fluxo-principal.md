# Fase 4 · Saga do orçamento até a entrega

**Modelo** · GPT-6 Astra, High. **Entrada** · Domínio, consumidor SQS e contratos revisados. Usar fixtures compatíveis para participantes indisponíveis.

## Implementar na OS

1. Completar a máquina de estados da seção 4 do [contrato atualizado](../contratos-saga-arquitetura-v2.md) para o caminho principal. Tratar `OrcamentoEnviado`, `OrcamentoAprovado` e `OrcamentoRejeitado` com revisão vigente, validação da composição e transições idempotentes. Se decisão chegar antes do envio e faltar contexto, aplicar a política de retry/reconciliação fechada em 1B. Evento obsoleto não regride a OS.
2. Implementar CT-02A e CT-03 na OS para que Billing consulte estado e composição. Recuperar a revisão vigente por CT-10 quando faltar o vínculo local. A aprovação deixa a Saga pronta para execução. O funcionário inicia o item na OS. Rota global de início apenas valida e retorna a prontidão, conforme o guia.
3. Gravar `saga_operation` com intenção, `operationId`, item e tentativa antes do CT-05. O início fica pendente até confirmação. Quando a API não tiver resposta definitiva, devolver a operação consultável conforme o contrato HTTP fechado na fase 1B. Repetição da solicitação ativa reaproveita a tentativa. Consumir em bloco os materiais do item via REST. Item sem materiais inicia localmente conforme as guardas.
4. Aplicar `CONSUMED` com referências de movimentos após revalidar cancelamento, item e versão. Estoque insuficiente confirmado coloca o item em `WAITING_FOR_PARTS_AND_SUPPLIES`. Nova tentativa é explícita após reposição. Timeout mantém o mesmo `operationId` pendente. A primeira confirmação leva a Saga a `IN_EXECUTION`. As operações seguintes são independentes por item e não fazem a Saga voltar a `START_PENDING`.
5. Concluir itens e OS com as guardas técnicas. A OS comercial fica `COMPLETED` e a Saga `WAITING_PAYMENT`. O funcionário inicia o pagamento diretamente no Billing. A OS consome `PagamentoConfirmado` para a projeção e `PagamentoAtualizado` se acordado para a v1. O início e a conclusão do reparo já ocorreram nessa etapa.
6. A entrega consulta CT-07 e exige `settled=true`. Após a resposta, revalidar as guardas locais e gravar OS `DELIVERED` e Saga `COMPLETED` no mesmo commit. Recusa, indisponibilidade ou timeout bloqueiam a entrega. A indicação antecipada de `READY_FOR_DELIVERY` segue o detalhe a fechar com Julio. A entrega pode consultar CT-07 mesmo quando o evento de pagamento ainda está atrasado.

## Aceite

- [ ] Fluxo de orçamento aprovado até entrega demonstrado, com reparo concluído antes do pagamento.
- [ ] Consumo de dois itens, item sem material, falta de saldo, solicitação repetida e resultado desconhecido testados.
- [ ] Evento de aprovação fora de ordem e pagamento atrasado seguem as guardas do contrato. Entrega com quitação falsa ou indisponível fica bloqueada.
- [ ] HTTP, DTOs e projeções distinguem início solicitado, confirmado e pendente. Compensação completa entra na fase 5.
