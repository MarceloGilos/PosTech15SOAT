# Fase 7 - Integração, qualidade e entrega da OS

**Modelo:** GPT-6.1 Sol, High, para integração/artefatos; GPT-6 Astra, High, para uma revisão final de consistência dos fluxos. O uso de outro modelo é uma recomendação, não autorização para criar agentes/conversas automaticamente.

**Entrada:** fases anteriores revisadas e versões compatíveis de Billing/Inventory/infra/auth disponíveis. Evidências de fases anteriores não substituem esta integração.

## Implementações e validações

1. Integrar os participantes reais pelo [contrato ativo](../contratos-saga-arquitetura-v2.md), registrando commits/imagens usados. Demonstrar abertura → diagnóstico → orçamento/aprovação → consumo REST no início → execução → conclusão → pagamento iniciado pelo funcionário no Billing/Mercado Pago de teste → quitação CT-07 → entrega. Consultar o histórico no DynamoDB, projetado a partir da conclusão.
2. Executar falha/compensação com OS, Billing e Inventory reais. Demonstrar baixa, reversão idempotente ou fechamento, cancelamento do orçamento e estado final da Saga. Incluir o cenário em que a resposta do consumo se perde. Identificar separadamente os testes com simulador.
3. Criar pelo menos um BDD completo e estável. Nos testes distribuídos, aguardar estado com timeout limitado, sem sleeps longos arbitrários. Documentar pré-requisitos/fixtures; testar também recusa de pagamento e recuperação, conforme escopo do grupo.
4. Integrar entrada já existente Gateway/authorizer/VPC Link/NLB, com Marcelo ajustando rotas/mapeamentos. Testar cliente autorizado, acesso indevido, contexto parcial/forjado e provider local indisponível em produção. Validar login após a separação do cliente e ausência de leitura direta do banco OS pelo auth.
5. Garantir DB/usuário exclusivos, migrations próprias e credenciais restritas. O deploy legado consulta segredo master RDS; a nova OS não deve herdar esse acesso como estado final. Marcelo entrega segredo/usuário apropriados e permissões reais SQS/DynamoDB viáveis no Academy.
6. Adaptar Kubernetes e pipeline sem colidir com o legado: Deployment/Service/ServiceAccount/config/secret/ECR próprios, probes, requests/limits e estratégia de rollout. Preservar NLB/conectividade existentes; não criar novo cluster/ALB para a OS.
7. CI independente deve executar build, unitários/integração, contratos, cobertura mínima de 80% de linhas e SonarQube/equivalente. Falha do quality gate deve impedir promoção/deploy; não rodar análise apenas informativa e chamar isso de bloqueio.
8. Ativar deploy automatizado da OS somente após a configuração estar pronta e autorização de execução cloud. Reutilizar o ambiente production atual; não apontar o novo banco/migrations para o legado. Manter governança de PRs/checks e promoção develop → main.
9. Reaproveitar Datadog APM/DogStatsD/logs estruturados, com serviço distinto e propagação HTTP/SQS. Criar/validar alarmes de DLQ, idade de outbox/Saga, compensação e histórico pendente. Não declarar trace distribuído mostrando apenas logs.
10. Preparar rollout/corte de dados com conciliação de clientes, veículos, ordens e IDs, validando login/referências externas. Não executar corte/migração do ambiente existente sem uma solicitação específica e plano revisável. Rollback da imagem não desfaz mensagens/dados/pagamentos.

## Entregáveis da OS

- Código, Dockerfile, manifestos Kubernetes, CI/CD e README completos.
- Contratos compartilhados finais e ADRs sobre limites, Saga, início pendente, compensação e histórico NoSQL.
- Swagger/OpenAPI/collection atualizados, incluindo resposta de início pendente, novos estados e paginação de histórico.
- Relatórios de teste/cobertura e evidência Sonar; comandos reproduzíveis para unitário, integração e BDD.
- Runbook: filas/DLQ, reprocessamento, operações incertas, projeção atrasada e diagnóstico de permissões/rede.
- Evidências para o vídeo do grupo de até 15 minutos e PDF do portal. Incluir links, diagrama real, commits, fluxo, falha/compensação, deploy com testes e rastreamento distribuído.

## Aceite final

- [ ] Fluxo real de três serviços e Mercado Pago de teste demonstrado.
- [ ] Compensação e recuperação não dependem de edição manual do banco.
- [ ] Histórico NoSQL é usado pela API, com recuperação de falha comprovada.
- [ ] Autenticação e isolamento de dados funcionam após a extração.
- [ ] 80%/BDD/Sonar/checks/deploy têm evidências reais; limitações não são ocultadas.
- [ ] Logs/traces e alarmes mostram o processo distribuído.
- [ ] Matheus revisou; Julio/Anderson/Marcelo confirmaram os pontos de integração que lhes cabem.

## Prompt específico

```text
Execute somente a fase 7: integrar a OS aos participantes reais e à plataforma
existente da PosTech15SOAT, completar BDD/qualidade/observabilidade e preparar
entrega/deploy independente. Preserve os limites dos repositórios e o Academy.
Não tratar simulação como prova de integração real. Antes de qualquer deploy
ou corte de produção, deixe manifestos, configurações, testes e plano de dados
concretos e revisáveis, respeitando a autorização dada para esta execução.
Entregue evidências e pendências; não altere código de outros membros sem escopo.
```
