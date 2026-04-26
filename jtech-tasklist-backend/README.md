# jtech-tasklist-backend

API REST de listas e tarefas multi-usuário, construída com Spring Boot.

## Visão Geral

O backend segue uma variação pragmática de arquitetura em camadas com ports and adapters. A ideia aqui foi manter a separação entre entrada HTTP, regras de negócio, persistência e infraestrutura, sem transformar o projeto em uma implementação purista só por formalidade.

Representação estrutural:

```text
Controller -> Input Port -> Use Case -> Output Port -> Adapter -> Repository
```

Na prática, essa organização ajudou em:

- melhor separação de responsabilidades
- regras de negócio mais testáveis
- menor acoplamento entre domínio e framework

## Stack

- `Java 21`
- `Spring Boot 3.5.5`
- `Spring Security`
- `Spring Validation`
- `Spring Data JPA + Hibernate`
- `PostgreSQL 16`
- `JWT + BCrypt + Refresh Token`
- `JUnit 5 + Mockito + Spring Boot Test`

## Requisitos para rodar

- `JDK 21+`
- `Docker` e `Docker Compose`
- `Gradle Wrapper` já incluído no projeto

## Como rodar

### 1. Subir o banco de dados

```bash
cd composer
docker compose up -d
```

### 2. Rodar a aplicação

```bash
./gradlew bootRun
```

Aplicação disponível em:

```text
http://localhost:8080
```

Swagger:

```text
http://localhost:8080/doc/tasklist/v1/api.html
```

### 3. Variáveis de ambiente

| Variável | Padrão |
|---|---|
| `PORT` | `8080` |
| `DS_URL` | `localhost` |
| `DS_PORT` | `5432` |
| `DS_DATABASE` | `tasklist` |
| `DS_USER` | `postgres` |
| `DS_PASS` | `postgres` |
| `JWT_SECRET` | valor padrão de desenvolvimento |

### 4. Rodar os testes

```bash
./gradlew test
```

A suíte cobre:

- testes unitários dos use cases de autenticação, tasklists e tasks
- testes de integração dos controllers
- cenários de validação, autenticação, ownership e refresh token

## Estrutura

```text
adapters/
  input/controllers/              # controllers REST
  input/protocols/                # DTOs de request/response
  output/                         # adapters de persistência
  output/repositories/            # JpaRepository
  output/repositories/entities/   # entidades JPA

application/
  core/domains/                   # entidades de domínio
  core/usecases/                  # regras de negócio
  ports/input/                    # contratos de entrada
  ports/output/                   # contratos de saída

config/
  infra/exceptions/               # exceptions de domínio
  infra/utils/                    # handler global e utilitários
  usecases/                       # configuração de beans
```

## Decisões Técnicas

### Refresh token com rotação

Cada uso bem-sucedido do endpoint de refresh revoga logicamente o token anterior e emite um novo refresh token. Isso preserva histórico, evita delete físico e reduz risco de reuso indevido.

### Ownership por recurso

- listas pertencem a um usuário
- tarefas pertencem a um usuário e a uma lista
- operações sensíveis validam propriedade do recurso

Essa modelagem reforça bem o cenário multi-usuário do desafio.

### Duplicidade de task por título normalizado

As tasks validam duplicidade por `lower(trim(title))`, dentro da mesma lista. Isso evita duplicatas semânticas como diferenças só de caixa ou espaço.

### Contrato de `completed`

Na criação, a task sempre nasce com `completed = false`. Na atualização, o campo é tratado explicitamente no `PUT`. Para o escopo do desafio, preferi manter um request compartilhado. Em uma evolução futura, separar create e update deixaria esse contrato mais claro.

### DDL gerenciado pelo Hibernate

`ddl-auto: update` foi mantido por praticidade durante o desafio. Em produção, o caminho natural seria usar Flyway ou Liquibase para migrations versionadas.

## Endpoints

### Autenticação

| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/auth/register` | Cadastrar usuário |
| POST | `/auth/login` | Login e geração de tokens |
| POST | `/auth/refresh` | Rotacionar refresh token |

### Tasklists

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/v1/tasklists` | Listar listas do usuário |
| GET | `/api/v1/tasklists/{id}` | Buscar lista por ID |
| POST | `/api/v1/tasklists` | Criar lista |
| PUT | `/api/v1/tasklists/{id}` | Atualizar lista |
| DELETE | `/api/v1/tasklists/{id}` | Excluir lista e tarefas associadas |

### Tasks

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/v1/tasks` | Listar tasks do usuário |
| GET | `/api/v1/tasks?tasklistId={id}` | Listar tasks de uma lista |
| GET | `/api/v1/tasks/{id}` | Buscar task por ID |
| POST | `/api/v1/tasks` | Criar task |
| PUT | `/api/v1/tasks/{id}` | Atualizar task |
| DELETE | `/api/v1/tasks/{id}` | Excluir task |
