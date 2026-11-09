# Desenvolvimento do Marcelo

Este arquivo registra o escopo inicial do projeto. Para a fase 4, consultar a [proposta atual para a equipe](../fase-4/proposta-equipe.md#105-marcelo---infraestrutura-e-plataforma): Marcelo fica com os repositórios de infraestrutura de bancos, AWS e Lambdas de autenticação, além de CI/CD, Kubernetes, observabilidade e apoio às integrações.

## Escopo

Marcelo fica responsavel pela base tecnica do projeto e pela integracao final entre os modulos.

O foco aqui e escrever o codigo que permite os outros tres modulos funcionarem bem juntos.

## O que desenvolver

### Estrutura base do projeto

- organizar os pacotes do projeto
- definir padrao de DTOs
- definir padrao de responses de erro
- definir convencoes de controllers e services

### Infraestrutura

- configurar Flyway
- preparar configuracoes do banco
- criar estrutura de profiles
- preparar configuracao para execucao local

### Seguranca

- implementar autenticacao JWT para APIs administrativas
- configurar Spring Security
- liberar apenas o que precisar ficar publico
- deixar a consulta de acompanhamento conforme a regra definida pelo grupo

### API e documentacao tecnica

- configurar Swagger/OpenAPI
- padronizar contratos HTTP
- documentar endpoints principais

### Qualidade e integracao

- criar tratamento global de excecoes
- criar handlers para erros de validacao
- padronizar codigos HTTP
- integrar os modulos desenvolvidos por Anderson, Julio e Matheus
- ajustar conflitos entre DTOs, services e controllers

### Entregaveis tecnicos

- criar `Dockerfile`
- criar `docker-compose.yml`
- revisar `README.md`
- organizar como a aplicacao sobe localmente

### Testes

- criar base de testes de integracao
- ajudar no setup de testes dos demais
- revisar cobertura dos fluxos criticos

## Entrega esperada

No final, Marcelo deve deixar o projeto pronto para:

- subir localmente
- rodar migrations
- autenticar as APIs administrativas
- expor documentacao OpenAPI
- integrar os modulos do time
- preparar o repositorio para demonstracao

## Limite do escopo

Marcelo nao precisa implementar o CRUD inteiro de cliente, estoque ou OS sozinho.

O papel principal dele e:

- base tecnica
- seguranca
- integracao
- fechamento do projeto
