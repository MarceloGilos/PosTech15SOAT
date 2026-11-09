# Desenvolvimento do Matheus

Este arquivo registra o escopo inicial do projeto. Para a fase 4, consultar a [proposta atual para a equipe](../fase-4/proposta-equipe.md#102-matheus---os-diagnóstico-execução-e-saga): Matheus fica com OS, clientes/veículos, diagnóstico, execução, Saga e histórico de revisões no DynamoDB.

## Escopo

Matheus fica responsavel pelo modulo principal de ordem de servico:

- `ordemServico`
- `ordemServicoServico`
- `ordemServicoServicoItens`
- `orcamento`

O objetivo e entregar o fluxo principal da oficina em codigo.

## O que desenvolver

### Banco e persistencia

- criar migration de `ordemServico`
- criar migration de `ordemServicoServico`
- criar migration de `ordemServicoServicoItens`
- criar migration de `orcamento`

### Dominio

- entidade `OrdemServico`
- entidade `Orcamento`
- entidade `OrdemServicoServico`
- entidade `OrdemServicoServicoItem`
- enum `EStatusOS`
- enum `EStatusServico`
- enum de status do orcamento

### Regras

- criar OS vinculando cliente e veiculo existentes
- registrar descricao inicial
- registrar diagnostico
- adicionar servicos na OS
- adicionar pecas e insumos na OS
- calcular valor do orcamento
- gerar orcamento
- registrar aprovacao ou reprovacao do cliente
- mudar status da OS conforme o fluxo
- controlar status dos servicos da OS
- registrar datas principais da OS

### Aplicacao

- repository de ordemServico
- repository de ordemServicoServico
- repository de ordemServicoServicoItens
- repository de orcamento
- service de ordemServico
- service de orcamento

### API

- endpoint de criar OS
- endpoint de detalhar OS
- endpoint de listar OS
- endpoint de registrar diagnostico
- endpoint de adicionar servicos na OS
- endpoint de adicionar itens na OS
- endpoint de gerar orcamento
- endpoint de responder orcamento
- endpoint de iniciar execucao
- endpoint de finalizar OS
- endpoint de consulta de acompanhamento da OS

### Integracoes internas

- usar cliente e veiculo do modulo do Anderson
- usar servico e item do modulo do Julio
- acionar baixa de estoque quando houver consumo de item no fluxo aprovado

### Testes

- testes unitarios do fluxo da OS
- testes unitarios do fluxo do orcamento
- testes de integracao dos endpoints principais
- testes cobrindo transicao de status

## Entrega esperada

No final, este modulo deve resolver o core do desafio:

- criacao da OS
- acompanhamento
- orcamento
- aprovacao
- execucao
- fechamento

## Limite do escopo

Matheus nao implementa:

- JWT
- docker
- swagger
- tratamento global de excecao
