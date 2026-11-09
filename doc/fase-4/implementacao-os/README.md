# Implementação da OS em fases

**Responsável** · Matheus. **Revisão do plano** · 07/11/2026. As [orientações da equipe](../orientacoes-implementacao-equipe.md) fecham DEC-01 a DEC-20 por delegação de Matheus e complementam o [contrato da Saga](../contratos-saga-arquitetura-v2.md). Elas prevalecem sobre as pendências do pacote de revisão 1B. O [contexto da organização](contexto-organizacao.md) registra as fontes de código e plataforma consultadas. O [contrato anterior da fase 1B](contrato-saga-v1-proposta.md) serve como histórico.

O percurso de referência é **diagnóstico → orçamento aprovado → consumo ao iniciar o item → execução → conclusão técnica → pagamento iniciado pelo funcionário no Billing → consulta de quitação → entrega**. A OS orquestra com estado durável em PostgreSQL. Billing publica eventos para a OS em uma fila SQS. Consumo, reversão/fechamento e cancelamento comercial usam REST. A OS projeta o histórico de revisões no DynamoDB.

## Estado local verificado em 07/11/2026

- **Fase 1A** · A base do repositório existe. O commit local de referência é `11f3b82`, com bootstrap Java/Spring, health, Docker, Kubernetes inicial e CI. Aproveitar essa estrutura.
- **Fase 1B** · O commit local `afdd830` preserva `2.0.0-review.1`. A revisão local seguinte já materializa `2.0.0-os-baseline.1`, com composição obrigatória, EV-04 ativo, CT-10/11 e os contratos de API definidos no guia.
- **Fase 2** · A branch local `feature/fase-2-dominio-persistencia` em `tmp/fase-2/numberone-service-order` tem os commits `fce733a` e `13817e5`, 11 arquivos preparados no índice e uma adaptação posterior ainda sem commit. A adaptação inclui `CompositionSnapshot`, migration V2 e consumo direto. O relatório de 07/11 registra 134 testes sem falhas e 93,40% de cobertura de linhas.
- **Fases 3 a 7** · A base local tem componentes que serão reaproveitados, mas ainda carece de Saga em execução, mensageria, integrações reais, histórico DynamoDB e evidências da entrega distribuída.

O pacote `2.0.0-os-baseline.1` já foi incorporado ao filesystem do checkout da fase 2, com validador e documentação. Use a [preparação da fase 3](../../../tmp/fase-2/numberone-service-order/doc/progress/preparacao-fase-3.md) para iniciar a implementação. O código Java/SQL e o índice Git preexistentes da fase 2 foram conferidos por hash.

## Ajustes e implementação por fase

| Fase | O que reaproveitar ou ajustar | O que implementar ou validar | Modelo |
| --- | --- | --- | --- |
| [1A · Base](fase-1-repositorio-contratos.md#1a-base-existente) | Reaproveitar repositório, bootstrap e CI. | Conferir os checks e o estado da governança quando preparar a entrega. | Sol 6.1 Medium para conferência |
| [1B · Contratos](fase-1-repositorio-contratos.md#1b-contratos) | Trocar o contrato ativo de comandos, reserva e pagamento antecipado pelo fluxo atualizado. | Schemas e fixtures dos eventos Billing → OS, OpenAPI REST, matriz de estados, IDs e cenários de contrato. | Astra High |
| [2 · Domínio](fase-2-dominio-persistencia.md) | Manter a extração e corrigir as guardas de pagamento e de consumo condicionado à reserva. | Migration incremental, compatibilidade dos status/DTOs e regressões. O início integrado entra na fase 4. | Sol 6.1 High |
| [3 · Mensageria](fase-3-mensageria.md) | Aproveitar tooling de contratos. A outbox de publicação SQS é do Billing. | Consumidor SQS e inbox da OS, persistência inicial da Saga e primeira transição durável. | Sol 6.1 High |
| [4 · Caminho principal](fase-4-saga-fluxo-principal.md) | Separar os status comerciais da OS da etapa técnica da Saga. | Orçamento por eventos, consumo REST por item, execução/conclusão, projeção de pagamento e entrega com consulta oficial. | Astra High |
| [5 · Compensação](fase-5-compensacao-recuperacao.md) | Aplicar a janela de cancelamento validada por Matheus. | Reconciliar timeout, coordenar `CLOSED`/`REVERSED`, cancelar orçamento elegível e retomar após falha. | Astra High |
| [6 · Histórico](fase-6-historico-nosql.md) | Manter o desenho DynamoDB com outbox local da OS. | Snapshot na conclusão, worker idempotente e API autorizada de consulta por veículo. | Sol 6.1 High |
| [7 · Entrega](fase-7-integracao-entrega.md) | Reaproveitar Gateway, EKS, Datadog e CI da organização. | Integração real, Mercado Pago de teste, BDD, cobertura, qualidade, deploy e evidências. | Sol 6.1 High, Astra High na revisão final |

## Ordem de retomada

1. **Iniciar a fase 3 na base preparada.** Os contratos e exemplos locais já permitem implementar inbox/Saga e o primeiro handler usando fixtures do Billing.
2. **Ao integrar as transições das fases 4 e 5**, aplicar valor aprovado positivo e adaptar o cancelamento de itens aprovados preservando os snapshots. Manter a compatibilidade com a adaptação existente da fase 2.
3. **Revisar cada entrega.** A fase 6 pode começar assim que o ponto de conclusão técnica estiver estável, em paralelo à fase 5. A fase 7 usa versões compatíveis dos três serviços e a infraestrutura disponível.

## Entrega ao modelo e revisão

Use os [prompts por fase](prompts-por-fase.md), com modelo sugerido e bloco comum para cada execução.

Forneça esta pasta, o contrato atualizado e os relatórios da última fase. Informe o checkout e commit de destino. Use o código da organização como referência de origem e o checkout local da OS como base para a evolução existente.

```text
Execute somente a fase [N] da implementação da OS.
Leia este README, contratos-saga-arquitetura-v2.md, contexto-organizacao.md
e o arquivo da fase. Confira o Git e as instruções do checkout de destino.
Preserve alterações já preparadas e trabalhe dentro do repositório da OS.
Use fixtures identificadas enquanto dependências reais estiverem indisponíveis.
Entregue as alterações, os resultados de testes, o roteiro de demonstração
e os alinhamentos pendentes. Pare para a revisão de Matheus.
```

O aceite de cada fase exige evidência do que foi demonstrado, identificação dos limites do teste e revisão de Matheus. Alterações em repositórios de outros serviços, merge, migração de produção e deploy seguem autorizações próprias.
