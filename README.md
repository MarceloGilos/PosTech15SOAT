# PosTech15SOAT

Base tecnica inicial do back-end do Tech Challenge da oficina.

Neste momento, o projeto ja possui:

- estrutura base em Spring Boot 4
- configuracao de Flyway
- autenticacao JWT para APIs administrativas
- bootstrap automatico de usuario admin local
- padrao de tratamento de erros
- documentacao OpenAPI via Swagger
- Dockerfile e docker-compose
- testes de integracao iniciais para seguranca

## Stack

- Java 25
- Spring Boot 4.0.5
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Flyway
- PostgreSQL
- H2 para testes

## Estrutura inicial implementada

Pacotes principais:

- `shared/api`: health check e padrao de erro
- `shared/config`: configuracao de OpenAPI
- `shared/security`: JWT, login, bootstrap do admin e seguranca da API

## Subida local sem Docker

### 1. Banco

Suba um PostgreSQL local com:

- database: `numberone`
- username: `postgres`
- password: `postgres`


Se o Docker estiver ativo, ao iniciar com `./mvnw spring-boot:run` a aplicacao passa a tentar subir automaticamente os servicos do `docker-compose.yml` (como o PostgreSQL), reduzindo erros de conexao recusada em `localhost:5432`.

Ou sobrescreva por variavel de ambiente:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

### 2. Rodar a aplicacao

```bash
./mvnw spring-boot:run
```

Por padrao, o profile `dev` sera usado.

### 3. Mailpit para emails locais

Suba o Mailpit com:

```bash
docker compose up -d mailpit
```

Com a configuracao atual da aplicacao, ele ficara disponivel em:

- SMTP: `localhost:1025`
- inbox web: `http://localhost:8025`

Assim, os emails enviados pela aplicacao ficarao visiveis na interface web do Mailpit.

## Usuario admin local

Ao subir a aplicacao pela primeira vez, um usuario admin e criado automaticamente se a tabela `admin_users` estiver vazia.

Credenciais locais padrao:

- usuario: `admin`
- senha: `admin123456`

Esses valores podem ser sobrescritos por:

- `BOOTSTRAP_ADMIN_USERNAME`
- `BOOTSTRAP_ADMIN_PASSWORD`
- `BOOTSTRAP_ADMIN_ROLE`

## JWT

O login administrativo pode ser feito em:

```text
POST /api/public/auth/login
```

Exemplo:

```json
{
  "username": "admin",
  "password": "admin123456"
}
```

O token retornado deve ser enviado como:

```text
Authorization: Bearer <token>
```

## Endpoints iniciais

Publicos:

- `GET /api/public/health`
- `POST /api/public/auth/login`

Administrativos protegidos:

- `GET /api/admin/session`

## Swagger

Com a aplicacao rodando:

```text
http://localhost:8080/swagger-ui.html
```

## Flyway

Migrations atuais:

- `V1__create_admin_users_table.sql`

## Rodando os testes

```bash
./mvnw test
```

## Subida com Docker

```bash
docker compose up --build
```

Servicos:

- app: `http://localhost:8080`
- postgres: `localhost:5432`
- mailpit smtp: `localhost:1025`
- mailpit web: `http://localhost:8025`

## Proximos passos do dominio

Esta base foi preparada para receber os modulos do time:

- client e vehicle
- servico, item e estoque
- ordem de servico e orcamento

Ou seja, a parte de infraestrutura e seguranca ja fica pronta para a evolucao dos modulos de negocio.



### Checklist rapido de validacao local

1. Confirme se o Docker esta ativo (se for usar auto-start do compose).
2. Verifique se a porta `5432` esta livre antes de subir a aplicacao.
3. Suba a aplicacao com `./mvnw spring-boot:run`.
4. Valide se o banco subiu com `docker compose ps postgres` (status `running`).
5. Teste o health check em `GET http://localhost:8080/api/public/health`.
6. Se ainda falhar conexao, sobrescreva `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` no shell e rode novamente.
