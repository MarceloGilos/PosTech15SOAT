# Fase 5 · Compensação e recuperação

**Modelo** · GPT-6 Astra, High. **Entrada** · Caminho principal revisado, CT-08/08B/09 alinhados com Anderson e Julio e política da seção 5 do [contrato atualizado](../contratos-saga-arquitetura-v2.md).

## Implementar na OS

1. Para cancelamento integral elegível, antes do primeiro início confirmado e com não uso das peças conferido, serializar cancelamento e confirmação de início. Gravar `COMPENSATING`, autor, motivo, evidência e escopo. Bloquear novos inícios e retentativas de consumo. Resultado tardio pode registrar o fato ocorrido e deve respeitar o bloqueio.
2. Resolver todas as operações afetadas. Consultar CT-08 pelo `operationId` original. Uma recusa terminal dispensa reversão. Se a operação tiver consumido, pedir CT-08B com `compensationId` estável e conferência de não uso/devolução. Se não houver baixa confirmada, CT-08B pode gravar `CLOSED`, bloqueando o pedido que chegar atrasado. Confirmar o resultado com o Inventory antes de prosseguir.
3. Depois de resolver o estoque, chamar CT-09 para o orçamento conhecido ou CT-11 para fechar o contexto pela OS. Usar operação estável e resultado confirmado. CT-11 confirma `NO_BUDGET` com contexto fechado quando o orçamento não existe e bloqueia criação atrasada. Um 404 de consulta sozinho mantém a etapa pendente.
4. Confirmados os efeitos aplicáveis, gravar OS `CANCELLED` e Saga `COMPENSATED` em transação local. Revisão operacional cobre cancelamento parcial, início já confirmado, uso físico ou conflito que exija decisão humana. Retorno ao estoque exige conferência da devolução quando a peça saiu fisicamente.
5. Retomar operações após timeout/reinício, com chaves estáveis, próxima tentativa e exclusão mútua local. Fora do cancelamento, consultar CT-08 e retentar CT-05 com o mesmo ID quando a tentativa ainda estiver ativa e for seguro repetir. Durante o cancelamento, resolver por consulta ou CT-08B. `UNKNOWN` mantém o resultado em aberto. Rejeição de orçamento aguarda revisão. Recusa de pagamento após o reparo mantém OS concluída e entrega bloqueada. Tratamento de estorno é do Billing conforme escopo acordado. Documentar reprocessamento de eventos e operações, e como acionar revisão operacional.

Inventory implementa a serialização de CT-05 e CT-08B pela mesma identidade, inclusive se a tentativa ainda não existe. Billing implementa CT-09 com validação da revisão e elegibilidade. A OS valida o protocolo com fixtures e, na fase 7, com os serviços reais.

## Aceite

- [ ] Com saldo 5, fechamento antes da baixa mantém saldo 5 e CT-05 atrasado é recusado.
- [ ] Baixa confirmada muda 5 → 4. Com não uso/devolução conferidos, reversão muda 4 → 5 uma única vez mesmo com retry.
- [ ] Timeout depois da baixa é reconciliado pelo mesmo ID. Cancelamento concorrente e resposta tardia não iniciam o item.
- [ ] Reinício entre resolução do estoque e CT-09 retoma o fluxo. Saga só finaliza depois da confirmação aplicável de Billing.
- [ ] Testes distinguem falha técnica, recusa de negócio e efeito físico. Cenário de intervenção e runbook estão disponíveis para revisão.
