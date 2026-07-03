# Infraestrutura e CI/CD

Esta pasta contém os manifests Kubernetes usados pela esteira de CI/CD para publicar a aplicação em um cluster Kubernetes local disponível no runner self-hosted `runner-windows-0002`.

## GitHub Actions

O workflow `.github/workflows/ci-cd-kubernetes-local.yml` é iniciado por `push` em qualquer branch, por `pull_request` para `main` e por acionamento manual (`workflow_dispatch`). Ele executa:

1. Testes unitários da aplicação Java com `./mvnw clean test` em runner self-hosted.
2. Build da imagem Docker no runner self-hosted `runner-windows-0002`.
3. Validação do acesso ao cluster Kubernetes local com `kubectl cluster-info`.
4. Aplicação dos manifests Kubernetes locais em `infraestrutura/kubernetes/application.yaml`.
5. Acompanhamento do rollout do deployment `numberone`.

O deploy não usa Amazon EKS, ECR, RDS, Terraform ou credenciais AWS. A imagem é construída localmente no runner e referenciada diretamente pelo Kubernetes local.

## Pré-requisitos do runner `runner-windows-0002`

Configure o runner self-hosted com:

- Docker disponível no PATH.
- `kubectl` disponível no PATH.
- Acesso já configurado ao cluster Kubernetes local no contexto padrão do usuário que executa o serviço do runner.
- Label customizada `runner-windows-0002` no runner, usada em `runs-on: [self-hosted, runner-windows-0002]`.

## Recursos Kubernetes locais

O manifest cria no namespace `numberone`:

- Secret e ConfigMap com as configurações locais da aplicação.
- PostgreSQL local com PVC de 1 GiB.
- Mailpit local para SMTP.
- Deployment da aplicação `numberone` com 1 réplica.
- Service `NodePort` expondo a API em `http://localhost:30080` no nó local.

## Variáveis opcionais do workflow

As variáveis abaixo ficam definidas no próprio workflow e podem ser alteradas se necessário:

- `IMAGE_NAME` (padrão `numberone`).
- `K8S_NAMESPACE` (padrão `numberone`).
