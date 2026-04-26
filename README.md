# JTech Tasklist Fullstack

Aplicação fullstack de listas e tarefas multi-usuário, com backend em Spring Boot e frontend em Vue 3.

## Visão Geral da Arquitetura

Estruturei a entrega em dois módulos independentes, mas conectados entre si:

- [jtech-tasklist-backend](./jtech-tasklist-backend/README.md)
- [jtech-tasklist-frontend](./jtech-tasklist-frontend/README.md)

No backend, a aplicação segue uma variação pragmática de arquitetura em camadas com ports and adapters:

```text
Controller -> Input Port -> Use Case -> Output Port -> Adapter -> Repository
```

Essa organização separa com clareza:

- entrada HTTP
- regras de negócio
- persistência
- segurança e infraestrutura

No frontend, a organização foi feita por domínio:

```text
src/
  api/
  modules/
    auth/
    tasklists/
    tasks/
  stores/
  router/
  layouts/
  plugins/
  components/
  types/
```

Essa estrutura ajudou a distribuir melhor as responsabilidades entre:

- telas e fluxos por domínio
- estado global em Pinia
- rotas e guards
- integração HTTP
- componentes compartilhados

A principal decisão arquitetural foi tratar a entrega como uma aplicação fullstack integrada. Em vez de manter o frontend isolado em mock, preferi conectar os dois módulos e trabalhar com fluxo real de autenticação, estado e acesso aos recursos.

## Stack Tecnológica

### Backend

- `Java 21`
  - escolhi uma versão moderna, estável e acima do mínimo exigido
- `Spring Boot`
  - serviu como base da API e acelerou a configuração da aplicação
- `Spring Security`
  - protege rotas, valida JWT e reforça o controle de acesso por usuário autenticado
- `Spring Validation`
  - centraliza validações no contrato HTTP e reduz lógica repetida
- `Spring Data JPA + Hibernate`
  - simplifica a persistência e mantém boa integração com PostgreSQL
- `PostgreSQL`
  - foi escolhido por se encaixar bem na modelagem relacional multi-usuário
- `JWT`
  - foi usado para autenticação stateless
- `BCrypt`
  - foi adotado para hash seguro de senhas
- `JUnit 5 + Mockito + Spring Boot Test`
  - cobrem testes unitários e de integração com o ferramental padrão do ecossistema Spring

### Frontend

- `Vue 3`
  - usei Composition API para manter os fluxos mais organizados e previsíveis
- `TypeScript`
  - ajudou a manter contratos mais claros entre stores, componentes e API
- `Pinia`
  - foi a escolha para estado global por ser simples, moderno e alinhado ao Vue 3
- `Vue Router 4`
  - separa a área pública da área autenticada e permite guards com clareza
- `Vuetify`
  - serviu como base visual para acelerar a entrega com consistência
- `Axios`
  - foi adotado para a camada HTTP com interceptors e refresh token
- `Vitest`
  - foi usado para testes unitários do frontend por integrar bem com Vite e Vue

## Como Rodar Localmente

### Pré-requisitos

- `Java 21+`
- `Node 20.19+` ou `22.12+`
- `Docker` e `Docker Compose`

### 1. Subir o banco de dados

```bash
cd jtech-tasklist-backend/composer
docker compose up -d
```

### 2. Rodar o backend

```bash
cd jtech-tasklist-backend
./gradlew bootRun
```

Backend disponível em:

```text
http://localhost:8080
```

### 3. Configurar o frontend

Criar `jtech-tasklist-frontend/.env`:

```env
VITE_API_BASE_URL=http://localhost:8080
```

### 4. Rodar o frontend

```bash
cd jtech-tasklist-frontend
npm install
npm run dev
```

Frontend disponível em:

```text
http://localhost:5173
```

## Como Rodar os Testes

### Backend

```bash
cd jtech-tasklist-backend
./gradlew test
```

### Frontend

```bash
cd jtech-tasklist-frontend
npm run test
```

Comando equivalente:

```bash
npm run test:unit
```

### Build de validação

```bash
cd jtech-tasklist-frontend
npm run build
```

## Estrutura de Pastas Detalhada

```text
.
├── README.md
├── jtech-tasklist-backend
│   ├── composer/                            # docker-compose do PostgreSQL
│   ├── src/main/java/.../adapters
│   │   ├── input/controllers                # controllers REST
│   │   ├── input/protocols                  # DTOs de request/response
│   │   └── output                           # adapters de persistência
│   ├── src/main/java/.../application
│   │   ├── core/domains                     # entidades de domínio
│   │   ├── core/usecases                    # regras de negócio
│   │   └── ports                            # contratos de entrada e saída
│   ├── src/main/java/.../config
│   │   ├── infra                            # segurança, exceptions e utilitários
│   │   └── usecases                         # configuração de beans
│   └── src/test/java                        # testes unitários e de integração
└── jtech-tasklist-frontend
    ├── public/                              # assets públicos
    ├── src/api                              # client axios + módulos HTTP
    ├── src/components                       # componentes compartilhados
    ├── src/layouts                          # contexto público e autenticado
    ├── src/modules
    │   ├── auth                             # login/cadastro e tela pública
    │   ├── tasklists                        # navegação e CRUD de listas
    │   └── tasks                            # CRUD de tarefas por lista
    ├── src/plugins                          # bootstrap de Pinia e Vuetify
    ├── src/router                           # rotas e guards
    ├── src/stores                           # estado global
    └── src/types                            # contratos tipados
```

## Decisões Técnicas Aprofundadas

### Integração real entre frontend e backend

Embora a proposta do frontend mencionasse autenticação simulada e persistência local completa, preferi evoluir a entrega como uma aplicação fullstack integrada. Essa escolha me permitiu demonstrar:

- autenticação real
- refresh token
- ownership de recursos
- sincronização de estado com API
- fluxo completo entre frontend e backend

Trade-off:

- perde aderência literal ao mock local
- ganha consistência arquitetural e fluxo real entre os módulos

### Persistência do frontend

No frontend, apenas a sessão de autenticação persiste em `localStorage`:

- `user`
- `accessToken`
- `refreshToken`
- `isAuthenticated`

Listas e tarefas são recarregadas da API a cada sessão.

Trade-off:

- abre mão da persistência completa local de listas e tarefas
- mantém o backend como fonte de verdade e evita divergência de estado

### Segurança e ownership

No backend:

- listas pertencem a um usuário
- tarefas pertencem a um usuário e a uma lista
- operações sensíveis validam propriedade do recurso

Essa modelagem reforça o contexto multi-usuário e reduz risco de acesso indevido.

### Refresh token com fila de requests

No client Axios, implementei:

- interceptor para `401`
- controle de `isRefreshing`
- fila de requests pendentes
- bloqueio de refresh para rotas `/auth/*`

Essa decisão evita refresh paralelo duplicado, reduz risco de loop infinito e deixa o fluxo autenticado mais estável.

### Organização modular do frontend

No frontend, a separação por domínio (`auth`, `tasklists`, `tasks`) foi uma escolha deliberada para evitar uma estrutura centrada apenas em tipo técnico.

Na prática, isso ajudou em:

- navegação mais simples pelo código
- manutenção dos fluxos principais
- separação mais clara entre lista, tarefa e autenticação

### Experiência de uso

Além do mínimo funcional, tratei a interface como parte importante da qualidade da entrega.

Isso aparece em:

- hierarquia mais clara entre listas e tarefas
- microcopy mais direta
- fluxo master-detail mais legível
- identidade visual consistente entre login e área autenticada

## Melhorias e Roadmap

### Backend

- substituir `ddl-auto` por migrations com Flyway ou Liquibase
- adicionar observabilidade e logs estruturados
- separar melhor requests de criação e atualização
- introduzir mappers dedicados entre domínio e infraestrutura

### Frontend

- ampliar cobertura de testes de UI e cenários de erro
- evoluir feedback visual para estados de loading e empty state
- adicionar suporte a mover tarefas entre listas com regra explícita
- consolidar tokens visuais e sistema de design em escala maior

### Produto

- filtros por status
- ordenação customizável
- busca por tarefa
- telemetria de uso e comportamento
