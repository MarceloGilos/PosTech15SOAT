# Prompts para executar a implementação da OS

Use **um prompt por execução**. Copie o bloco comum e, em seguida, o bloco da fase desejada na mesma mensagem. Selecione o modelo e o esforço no seletor do Codex. Para uma conversa nova, dê acesso ao workspace ou forneça o contrato atualizado, a especificação da fase e o último relatório com o checkout/commit de partida. As recomendações são específicas para este projeto. Os controles disponíveis variam por conta. [Documentação oficial de modelos no Codex](https://learn.chatgpt.com/docs/models)

| Fase | Modelo | Entrega para revisar |
| --- | --- | --- |
| [1A · Conferir a base](#1a--conferir-a-base) | GPT-6.1 Sol · Medium | Verificação da base já existente, se necessária. |
| [1B · Revisar os contratos](#1b--revisar-os-contratos) | GPT-6 Astra · High | Pacote de contratos, cenários e pendências para a equipe. |
| [2 · Adaptar domínio e banco](#2--adaptar-domínio-e-banco) | GPT-6.1 Sol · High | Domínio e migration compatíveis com consumo direto e pagamento após o reparo. |
| [3 · Mensageria](#3--mensageria) | GPT-6.1 Sol · High | Consumidor SQS, inbox e primeira transição da Saga. |
| [4 · Fluxo principal](#4--fluxo-principal) | GPT-6 Astra · High | Do orçamento aprovado à entrega, com estado durável. |
| [5 · Compensação e recuperação](#5--compensação-e-recuperação) | GPT-6 Astra · High | Cancelamento, `CLOSED`/`REVERSED` e retomada. |
| [6 · Histórico DynamoDB](#6--histórico-dynamodb) | GPT-6.1 Sol · High | Projeção idempotente e consulta por veículo. |
| [7 · Integração e entrega](#7--integração-e-entrega) | GPT-6.1 Sol · High | Integração real e evidências. Astra High para uma revisão final separada. |

**A fase 3 já tem base preparada.** O pacote local `2.0.0-os-baseline.1` incorpora as escolhas e já está no checkout da fase 2. Use o prompt 1B quando precisar revisar contratos. A fase 6 pode avançar em paralelo à fase 5 depois que o ponto de conclusão técnica da fase 4 estiver estável, em checkouts isolados se houver edições concorrentes.

## Bloco comum

```text
Trabalhe apenas na fase indicada abaixo para o repositório
PosTech15SOAT/numberone-service-order. A documentação de referência fica em
D:/Dev/PosTech15SOAT/doc/fase-4/. Leia orientacoes-implementacao-equipe.md,
contratos-saga-arquitetura-v2.md, implementacao-os/README.md e o arquivo da fase.
As orientações de 07/11 fecham DEC-01 a DEC-20 por delegação de Matheus e
prevalecem sobre pontos em aberto da revisão 1B. Trate o pacote anterior como histórico.

Confirme o checkout, a branch, o HEAD e as instruções do repositório antes de editar.
Preserve alterações existentes, inclusive arquivos em stage. Use a entrega
revisada da fase anterior como base. Se ela faltar, deixe explícita a dependência,
avance no que for independente e mantenha bloqueadas as decisões dependentes.
Respeite o escopo da OS. Use as escolhas do guia na implementação e registre
desvios ou limitações verificadas ao integrar os serviços.

Execute as verificações pertinentes e registre comandos, resultados e limites
dos testes. Identifique claramente fixtures, emuladores e integrações reais.
Ao terminar, entregue resumo, arquivos alterados, roteiro de demonstração,
pendências e checklist de aceite. Pare para revisão de Matheus antes da próxima
fase. Mudanças em outros repositórios, push, merge e execução em AWS dependem
do escopo explicitamente autorizado para a sessão.
```

## 1A · Conferir a base

**GPT-6.1 Sol · Medium** para esta verificação pontual.

```text
Revise somente a fase 1A em modo leitura. Leia
implementacao-os/fase-1-repositorio-contratos.md. Use como referência o bootstrap
11f3b82 e o checkout local
D:/Dev/PosTech15SOAT/tmp/fase-1/numberone-service-order.
Confira Maven Wrapper, Java/Spring, health/readiness, Docker, Kubernetes inicial
e os checks Required validation e Validate promotion source. Distinga o que
está versionado da execução de CI e da governança efetivamente ativa.
Entregue evidências e eventuais lacunas para revisão. O repositório já existe.
```

## 1B · Revisar os contratos

**GPT-6 Astra · High**.

```text
Execute somente a revisão da fase 1B. Leia
implementacao-os/fase-1-repositorio-contratos.md. Use como ponto de partida
D:/Dev/PosTech15SOAT/tmp/fase-1/numberone-service-order, cuja revisão 1B está
no commit afdd830. Confira o estado atual antes de editar. Preserve o histórico do
pacote anterior e reaproveite o validador e o CI.

Entregue um pacote versionado e revisável com catálogo, JSON Schemas e fixtures
para OrcamentoEnviado, OrcamentoAprovado, OrcamentoRejeitado e PagamentoConfirmado.
Ative PagamentoAtualizado para estorno integral confirmado conforme o guia de 07/11.
Modele uma fila Billing → OS com DLQ. Documente envelope, IDs, revisões,
idempotência, ordem dos eventos e transições da Saga.

Revise o OpenAPI de CT-02A/03, CT-05/08/08B e CT-07/09 e a API da OS para
início pendente. Inclua falta de saldo, timeout, CLOSED e REVERSED. Proponha
o vínculo obrigatório compositionVersion nos três eventos comerciais, com
exemplo de aprovação obsoleta. Acrescente CT-10/11, lookup Auth → OS,
cancelamento e retirada conforme o guia de 07/11. A fila é Standard.

Rode a validação offline de schemas, exemplos válidos/inválidos e OpenAPI.
Entregue uma matriz de decisões para Julio, Anderson, Marcelo e Matheus.
O aceite desta fase é o pacote revisável. Código de domínio entra na fase 2.
```

## 2 · Adaptar domínio e banco

**GPT-6.1 Sol · High**.

```text
Revise e complete somente a adaptação da fase 2 depois da revisão de 1B.
Leia implementacao-os/fase-2-dominio-persistencia.md e use
D:/Dev/PosTech15SOAT/tmp/fase-2/numberone-service-order como base.
Confira os commits fce733a e 13817e5 e preserve os arquivos já preparados
no índice do Git. Preserve os módulos e as regras locais já extraídos.

Reaproveite a adaptação local de 07/11 e confira as guardas para aprovação no início e conclusão técnica
antes do pagamento. Altere a regra de confirmação de consumo e crie migration
incremental após V1 para aceitar consumo direto sem reserva. Revise status,
DTOs, tracking e snapshot aprovado conforme o contrato. Mantenha as rotas de
início integrado e entrega real protegidas até a fase 4.

Valide PostgreSQL novo e atualização de um banco na V1. Execute regressões
de acesso, domínio e API. Demonstre que o reparo pode ser concluído sem
pagamento e que o banco aceita consumo confirmado sem reserva.
```

## 3 · Mensageria

**GPT-6.1 Sol · High**.

```text
Execute somente a fase 3 a partir da fase 2 revisada.
Use D:/Dev/PosTech15SOAT/tmp/fase-2/numberone-service-order e leia
doc/contracts/saga/v2/phase3-handoff.md. O pacote ativo é
2.0.0-os-baseline.1 e readyForOsImplementation=true permite esta execução.
approvedContract=false registra que a integração conjunta será revisada depois.
Preserve o índice Git e as alterações locais existentes.
Leia implementacao-os/fase-3-mensageria.md na pasta de especificação. Implemente na OS as migrations
service_order_saga e inbox_event e crie a Saga com a abertura da OS.
Implemente o consumidor SQS da fila Billing → OS, usando o contrato ativo.

Demonstre OrcamentoEnviado levando a Saga a WAITING_APPROVAL. Grave inbox e
transição no mesmo commit. Use eventId para deduplicar e compare o conteúdo para
detectar colisão de ID. Confirme a mensagem depois do commit. Trate falha,
reentrega e DLQ conforme o contrato. A outbox de publicação SQS é do Billing.

Teste rollback, duplicata, colisão de ID e reentrega após commit. Use fixture
de produtor quando Billing estiver indisponível e identifique o tipo de
transporte usado. A máquina completa entra na fase 4.
```

## 4 · Fluxo principal

**GPT-6 Astra · High**.

```text
Execute somente a fase 4 a partir da mensageria revisada.
Leia implementacao-os/fase-4-saga-fluxo-principal.md. Complete o caminho
principal da Saga na OS. Consuma a decisão do orçamento vigente e forneça
CT-02A e CT-03 ao Billing. Ao iniciar um item, persista a tentativa com
operationId antes de chamar CT-05. Revalide as guardas antes de confirmar
o início. Trate item sem materiais, falta de saldo, pedido duplicado e
resultado desconhecido com os estados definidos no contrato.

Conclua tecnicamente a OS antes da cobrança. O funcionário inicia o pagamento
no Billing. Atualize a projeção financeira pelos eventos aplicáveis. Na
entrega, exija CT-07 com settled=true e revalide as guardas locais antes de
gravar DELIVERED e concluir a Saga. Preserve a consulta oficial mesmo com
evento financeiro atrasado.

Demonstre o caminho principal com contratos reais e participantes simulados
quando necessário. Teste aprovação obsoleta, consumo pendente, falta de saldo
e entrega bloqueada sem quitação confirmada. A compensação completa é fase 5.
```

## 5 · Compensação e recuperação

**GPT-6 Astra · High**.

```text
Execute somente a fase 5 com CT-08/08B/09 alinhados. Leia
implementacao-os/fase-5-compensacao-recuperacao.md. Implemente C1–C6 na OS:
aceite o cancelamento automático apenas quando integral, antes do primeiro
início confirmado e com não uso verificado. Grave COMPENSATING e bloqueie
novos inícios e retentativas de consumo.

Reconcilie pelo operationId original com CT-08 e use CT-08B com compensationId
estável. CLOSED impede a baixa que chegar depois. REVERSED confirma o movimento
inverso quando a baixa ocorreu e a conferência permite devolver ao saldo.
Chame CT-09 ou CT-11 depois de resolver o estoque e grave COMPENSATED apenas quando
os efeitos aplicáveis estiverem confirmados. Encaminhe para revisão
operacional os casos fora da janela automática.

Teste os dois lados da corrida, timeout após baixa, resposta tardia,
reversão repetida e reinício antes de CT-09. Demonstre saldo antes/depois,
estado durável das operações e um roteiro de retomada.
```

## 6 · Histórico DynamoDB

**GPT-6.1 Sol · High**.

```text
Execute somente a fase 6 depois de estabilizar a conclusão técnica.
Leia implementacao-os/fase-6-historico-nosql.md. Grave um snapshot imutável da
manutenção executada e a pendência de projeção no mesmo commit que leva a OS a
COMPLETED. Implemente a outbox local de histórico e o worker da OS que escreve
diretamente no DynamoDB com chave estável e escrita condicional.

Implemente consulta paginada e autorizada por veículo. Demonstre que uma
falha de DynamoDB deixa a projeção retomável e permite concluir, pagar e
entregar. Teste retry após escrita, colisão de conteúdo e acesso indevido.
Entregue a configuração necessária para Marcelo e evidências locais.
```

## 7 · Integração e entrega

**GPT-6.1 Sol · High** para executar. **GPT-6 Astra · High** para a revisão separada.

```text
Execute somente a fase 7. Leia implementacao-os/fase-7-integracao-entrega.md.
Use as versões revisadas de OS, Billing, Inventory e plataforma e registre
commits/imagens utilizados. Demonstre o fluxo real de orçamento a entrega,
com consumo REST, SQS Billing → OS, Mercado Pago de teste e histórico DynamoDB.
Demonstre também a compensação e a recuperação de uma resposta de consumo
perdida.

Complete o BDD, os testes de segurança e isolamento, a cobertura mínima,
o quality gate, os artefatos de deploy, a observabilidade e o runbook.
Prepare as evidências para o vídeo e o PDF do portal. Registre quais checks
rodaram e quais dependem de ambiente, serviços ou autorização. Prepare o
deploy de forma revisável e execute em AWS somente quando a sessão autorizar.
```

Prompt opcional de revisão final, em uma execução separada com Astra.

```text
Faça uma revisão somente de leitura da entrega da fase 7. Compare os commits,
o contrato ativo e as evidências. Priorize perda de resposta, duplicação,
corrida entre consumo e cancelamento, entrega sem quitação, perda de histórico
e divergências entre fixtures e serviços reais. Classifique os achados por
gravidade com arquivo ou evidência e informe o que impede o aceite.
```
