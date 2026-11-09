# Fase 1 · Reaproveitar a base e revisar os contratos

**Modelos** · GPT-6.1 Sol, Medium, para conferir a base em 1A. GPT-6 Astra, High, para revisar os contratos em 1B. **Entrada** · [Índice](README.md), [contexto da organização](contexto-organizacao.md) e [contrato atualizado](../contratos-saga-arquitetura-v2.md).

## 1A. Base existente

Reaproveitar o bootstrap em `PosTech15SOAT/numberone-service-order`: Java/Spring, Maven Wrapper, health/readiness, Docker, manifests iniciais e CI. Conferir a origem registrada e os checks `Required validation` e `Validate promotion source`. Distinguir workflow versionado de ruleset efetivamente ativo. Manter as convenções da Fase 3 para identidade do Gateway, NLB, Datadog e governança.

## 1B. Contratos

1. Ler o pacote legado em `doc/contracts/saga/v1` do repositório OS e identificar os arquivos que o validador do CI percorre. Versionar o novo pacote de forma inequívoca, com um ADR que indique qual pacote está ativo e preserve a referência ao anterior. A versão do desenho arquitetural e `schemaVersion: 1` dos novos tipos são identificadores distintos.
2. Gerar catálogo, JSON Schemas e fixtures dos cinco eventos do [guia de decisões](../orientacoes-implementacao-equipe.md), com `compositionVersion` obrigatório nos três eventos de orçamento. Incluir `PagamentoAtualizado` para estorno integral confirmado. Publicação SQS pertence ao Billing. Consumo pertence à OS. Registrar fila Standard, DLQ Standard, IAM e reprocessamento.
3. Distinguir `eventId` para inbox, `correlationId` para rastreio, `serviceOrderId` para localizar a Saga, `operationId` para cada operação REST e `compensationId` para o fechamento ou reversão. Documentar `budgetVersion` crescente por OS e `paymentVersion` por pagamento.
4. Revisar OpenAPI. A OS fornece CT-02A e CT-03 ao Billing. Inventory fornece CT-05/08/08B à OS. Billing fornece CT-07/09 à OS. Descrever disponibilidade como leitura, consumo como operação idempotente e `CLOSED` como resultado durável. Fechar códigos HTTP, recusa de saldo, resultado desconhecido, quitação e conflito de idempotência. Atualizar o contrato da API pública da OS para início pendente, consulta de andamento e entrega, preservando rotas/DTOs compatíveis.
5. Aplicar o vínculo definido por `compositionVersion` e snapshot imutável. Documentar CT-10 para recuperar a revisão vigente e CT-11 para fechar o contexto por OS, inclusive sem orçamento. Acrescentar lookup Auth → OS e os contratos de cancelamento e retirada. Revisar os DTOs contra a implementação atual da fase 2.
6. Atualizar ADR, estados comerciais, estados da Saga e mapa de operações REST. Testar JSON Schema offline e OpenAPI no CI. Incluir fixtures válidas e inválidas para campos, UUID, data, valor, revisão e tipo. Cobrir também `CLOSED`, `REVERSED` e `INSUFFICIENT_STOCK` nas respostas de Inventory.

| Responsável | Alinhamento para a implementação |
| --- | --- |
| Julio | Semântica de `SENT`, revisão vigente, vínculo com composição, EV-04 condicional e elegibilidade de CT-09. |
| Anderson | Consumo agrupado, consulta, fechamento e reversão. Formalizar resultados e a evidência de não uso/devolução. |
| Marcelo | Fila/DLQ, permissões, identidade de serviço, banco lógico da OS, tabela DynamoDB e lookup do cliente no auth. |
| Matheus | Contratos da OS, transições, compatibilidade da API e testes que dão suporte às próximas fases. |

## Aceite

- [ ] O CI valida a versão ativa com schemas e fixtures reproduzíveis.
- [ ] Os participantes conseguem identificar quais eventos e endpoints implementar, inclusive no cenário de timeout e cancelamento.
- [ ] Decisões aprovadas têm versão ou commit registrado. Pontos sem aceite dos envolvidos estão identificados no pacote.
- [ ] Entrega revisável com links, mudanças no contrato e solicitações para cada responsável.
