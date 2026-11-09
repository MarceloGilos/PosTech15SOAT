# Desenvolvimento do Anderson

Este arquivo registra o escopo inicial do projeto. Para a fase 4, consultar a [proposta atual para a equipe](../fase-4/proposta-equipe.md#104-anderson---inventory-e-regras-de-materiais): Anderson fica com Inventory, peças/insumos, fornecedores, disponibilidade, reservas, consumo e movimentações em PostgreSQL.

## Escopo

Anderson fica responsavel pelo modulo de cadastro base:

- `cliente`
- `veiculo`

O objetivo e entregar todo o codigo necessario para cadastrar, consultar, atualizar e inativar clientes e veiculos.

## O que desenvolver

### Banco e persistencia

- criar as migrations de `cliente`
- criar as migrations de `veiculo`
- garantir `placa` unica em `veiculo`

### Dominio

- entidade `Cliente`
- entidade `Veiculo`
- enums ou objetos de valor que ajudarem nesse contexto

### Regras

- validar CPF/CNPJ no cadastro de cliente
- validar placa no cadastro de veiculo
- nao permitir veiculo sem cliente vinculado
- nao permitir duplicidade de placa

### Aplicacao

- repository de cliente
- repository de veiculo
- service de cliente
- service de veiculo

### API

- endpoint de criar cliente
- endpoint de atualizar cliente
- endpoint de listar clientes
- endpoint de detalhar cliente
- endpoint de inativar cliente
- endpoint de criar veiculo
- endpoint de atualizar veiculo
- endpoint de listar veiculos
- endpoint de detalhar veiculo

### Testes

- testes unitarios das validacoes
- testes unitarios dos services
- testes de integracao dos endpoints de cliente
- testes de integracao dos endpoints de veiculo

## Entrega esperada

No final, este modulo deve permitir que os outros devs consigam:

- buscar cliente por `id`
- buscar cliente por `documento`
- buscar veiculo por `id`
- buscar veiculo por `placa`

## Limite do escopo

Anderson nao implementa:

- estoque
- ordem de servico
- orcamento
- seguranca JWT
