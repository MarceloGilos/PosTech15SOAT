# Desenvolvimento do Julio

Este arquivo registra o escopo inicial do projeto. Para a fase 4, consultar a [proposta atual para a equipe](../fase-4/proposta-equipe.md#103-julio---orçamento-pagamento-e-mercado-pago): Julio fica com orçamento, pagamento e Mercado Pago.

## Escopo

Julio fica responsavel pelo modulo de catalogo e estoque:

- `servico`
- `item`
- `movimentacaoEstoque`

O objetivo e entregar todo o codigo de cadastro de servicos e pecas/insumos, junto com o controle de saldo e historico de movimentacao.

## O que desenvolver

### Banco e persistencia

- criar migration de `servico`
- criar migration de `item`
- criar migration de `movimentacaoEstoque`

### Dominio

- entidade `Servico`
- entidade `Item`
- entidade `MovimentacaoEstoque`
- enum `TipoMovimentacaoEstoque`

### Regras

- `tempoEstimadoMinuto` deve ser obrigatorio e positivo
- `valorBase` nao pode ser negativo
- `quantidadeEstoque` nao pode ficar negativa
- movimentacao `ENTRADA` soma no estoque
- movimentacao `BAIXA` subtrai do estoque
- movimentacao `AJUSTE` altera o saldo conforme a regra definida

### Aplicacao

- repository de servico
- repository de item
- repository de movimentacaoEstoque
- service de servico
- service de item
- service de estoque

### API

- endpoint de criar servico
- endpoint de atualizar servico
- endpoint de listar servicos
- endpoint de detalhar servico
- endpoint de inativar servico
- endpoint de criar item
- endpoint de atualizar item
- endpoint de listar itens
- endpoint de detalhar item
- endpoint de registrar entrada de estoque
- endpoint de registrar baixa de estoque
- endpoint de consultar saldo
- endpoint de listar movimentacoes de um item

### Testes

- testes unitarios das regras de estoque
- testes unitarios dos services
- testes de integracao dos endpoints de servico
- testes de integracao dos endpoints de item e estoque

## Entrega esperada

No final, este modulo deve permitir que os outros devs consigam:

- buscar servico por `id`
- buscar item por `id`
- consultar saldo disponivel de item
- registrar movimentacao de estoque por service interno

## Limite do escopo

Julio nao implementa:

- cliente e veiculo
- ordem de servico
- orcamento
- seguranca JWT
