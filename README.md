# JTech Tasklist

[![CI](https://github.com/leandropiassetta/jtech-tasklist-fullstack/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/leandropiassetta/jtech-tasklist-fullstack/actions/workflows/ci.yml)

Full-stack task management platform developed as part of a technical challenge for JTech.

The project demonstrates the implementation of a secure multi-user application using Java 21, Spring Boot, Vue 3, TypeScript, JWT authentication, PostgreSQL, and automated tests.

This repository is an independent technical challenge implementation. It is not an official JTech product and does not represent an employment relationship with JTech.

## Project Overview

JTech Tasklist connects a Spring Boot REST API to a Vue single-page application. Users can create an account, authenticate, organize task lists, and manage tasks while the backend enforces validation and resource ownership.

The repository is organized as two independently buildable modules:

- [`jtech-tasklist-backend`](./jtech-tasklist-backend/README.md): REST API, authentication, business rules, and persistence.
- [`jtech-tasklist-frontend`](./jtech-tasklist-frontend/README.md): browser application, client-side state, routing, and API integration.

## Technical Challenge

This project was built for a JTech technical challenge as a practical demonstration of full-stack engineering skills. The implementation goes beyond isolated mock screens by integrating the frontend with a real API, database-backed persistence, stateless security, and automated tests.

JTech did not publish or endorse this repository as an official product. References to the company identify only the context in which the challenge was proposed.

## Main Features

- User registration and login.
- JWT access tokens and rotating refresh tokens.
- Authenticated task-list and task CRUD operations.
- User-scoped queries and ownership checks for individual resources.
- Request validation and consistent API error responses.
- Duplicate-name protection for task lists and duplicate-title protection within a task list.
- Protected frontend routes with automatic session restoration.
- Axios interceptors that attach access tokens and coordinate token refresh for concurrent requests.
- Responsive interface built with Vuetify.
- Backend unit and integration tests plus frontend store, router, and component tests.

## Architecture

The backend follows a pragmatic Ports and Adapters structure:

```text
HTTP Controller -> Input Port -> Use Case -> Output Port -> Adapter -> JPA Repository
```

Domain rules live in use cases and domain objects. Controllers translate HTTP requests, output adapters handle persistence, and configuration code connects the application to Spring Security, JPA, and infrastructure services.

The frontend is organized around application domains and shared technical layers:

```text
Vue views -> Pinia stores -> Typed API modules -> Axios client -> REST API
```

Authentication, task-list, and task views are grouped by feature, while routing, layouts, reusable components, and API concerns remain separate.

## Technology Stack

| Area | Technologies |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5.5, Spring Security, Spring Validation, Spring Data JPA, Hibernate |
| Authentication | JWT, BCrypt, hashed and rotating refresh tokens |
| Database | PostgreSQL 16 for local runtime, H2 in-memory database for integration tests |
| Frontend | Vue 3, TypeScript, Vite, Pinia, Vue Router, Vuetify, Axios |
| Testing | JUnit 5, Mockito, Spring Boot Test, MockMvc, Vitest, Vue Test Utils, jsdom |
| Tooling | Gradle Wrapper, npm, Docker Compose, ESLint, vue-tsc, GitHub Actions |

## Authentication and Security

Spring Security protects every endpoint except authentication, API documentation, and selected actuator endpoints. The backend validates bearer JWTs without creating server-side sessions and derives the authenticated user identifier from the token subject.

Login returns an access token and a refresh token. Refresh tokens are stored as hashes in the database; a successful refresh revokes the previous token and issues a replacement. Passwords are hashed with BCrypt.

Task lists and tasks carry an owner identifier. Read, update, and delete operations verify ownership, and list queries are scoped to the authenticated user. This prevents one user from reading or mutating another user's resources.

On the frontend, the Axios client adds bearer tokens to requests. A single coordinated refresh flow queues concurrent failed requests and retries them after token renewal; failed renewal clears the session and redirects to login.

## Backend

The backend is located in `jtech-tasklist-backend` and exposes these primary endpoints:

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/auth/register` | Register a user |
| `POST` | `/auth/login` | Authenticate and issue tokens |
| `POST` | `/auth/refresh` | Rotate the refresh token and issue new tokens |
| `GET`, `POST` | `/api/v1/tasklists` | List or create the authenticated user's task lists |
| `GET`, `PUT`, `DELETE` | `/api/v1/tasklists/{id}` | Read, rename, or delete an owned task list |
| `GET`, `POST` | `/api/v1/tasks` | List/filter or create tasks |
| `GET`, `PUT`, `DELETE` | `/api/v1/tasks/{id}` | Read, update, or delete an owned task |

Bean Validation checks HTTP payloads, while the global exception handler maps validation and domain failures to structured API responses. PostgreSQL persistence is implemented with Spring Data JPA and Hibernate.

Backend verification commands:

```bash
cd jtech-tasklist-backend
./gradlew test
./gradlew build
```

## Frontend

The frontend is located in `jtech-tasklist-frontend`. It provides registration and login, protected navigation, task-list management, and task creation, editing, completion, and deletion.

Pinia stores coordinate API state. Vue Router guards separate public and authenticated areas. Only session data is persisted in `localStorage`; task lists and tasks are reloaded from the backend, which remains the source of truth.

Frontend quality and build commands:

```bash
cd jtech-tasklist-frontend
npm run lint -- --no-fix
npm run type-check
npm run test:unit -- --run
npm run build
```

## Automated Tests

Backend tests include isolated use-case tests for authentication, refresh-token rotation, task lists, tasks, and exception handling. MockMvc integration tests exercise controller validation, authentication, ownership, and CRUD behavior against an H2 in-memory database under the `test` profile.

Frontend tests exercise Pinia stores, route guards, authentication views, task-list views, and task views with Vitest, Vue Test Utils, and jsdom.

Run the test suites independently:

```bash
cd jtech-tasklist-backend
./gradlew test

cd ../jtech-tasklist-frontend
npm run test:unit -- --run
```

## Running Locally

### Prerequisites

- JDK 21.
- Node.js `^20.19.0` or `>=22.12.0`.
- Docker with Docker Compose.

### 1. Start PostgreSQL

From the repository root:

```bash
cd jtech-tasklist-backend/composer
docker compose up -d
```

The compose file starts PostgreSQL 16 on `localhost:5432` with local development credentials.

### 2. Start the backend

In a second terminal, from the repository root:

```bash
cd jtech-tasklist-backend
./gradlew bootRun
```

The API starts at `http://localhost:8080`.

### 3. Configure and start the frontend

In a third terminal, from the repository root:

```bash
cd jtech-tasklist-frontend
cp .env.example .env
npm ci
npm run dev
```

The application starts at `http://localhost:5173` and connects to the local API.

To stop the database and preserve its Docker volume:

```bash
cd jtech-tasklist-backend/composer
docker compose down
```

## Environment Variables

### Backend

| Variable | Local default | Description |
| --- | --- | --- |
| `PROFILE` | `dev` | Active Spring profile |
| `PORT` | `8080` | HTTP port |
| `DS_URL` | `localhost` | PostgreSQL host |
| `DS_PORT` | `5432` | PostgreSQL port |
| `DS_DATABASE` | `tasklist` | Database name |
| `DS_USER` | `postgres` | Database user |
| `DS_PASS` | `postgres` | Database password for local development |
| `JWT_SECRET` | Development-only fallback | Secret used to sign access tokens; set a strong private value outside local development |

`HOMOLOGATION_SERVER`, `HOMOLOGATION_PORT`, and `PRODUCTION_URI` can optionally override the server metadata displayed in the generated OpenAPI document.

### Frontend

| Variable | Example | Description |
| --- | --- | --- |
| `VITE_API_BASE_URL` | `http://localhost:8080` | Base URL used by the Axios API client |

The tracked `.env.example` contains the local frontend value. Real environment files are ignored by Git.

## API Documentation

With the backend running, the generated OpenAPI documentation is available at:

- Swagger UI: `http://localhost:8080/doc/tasklist/v1/api.html`
- OpenAPI JSON: `http://localhost:8080/doc/tasklist/v3/api-documents`

Use the Swagger UI **Authorize** control with a JWT access token to call protected endpoints.

## CI Pipeline

The [GitHub Actions workflow](./.github/workflows/ci.yml) runs on pushes and pull requests targeting `main`, and it can also be started manually.

Its independent jobs perform the following checks:

- **Backend:** set up Temurin Java 21 with Gradle caching, run `./gradlew test`, and run the complete `./gradlew build` lifecycle, which includes Gradle verification tasks.
- **Frontend:** set up Node 22 with npm caching, install from `package-lock.json` with `npm ci`, run ESLint without automatic fixes, run `vue-tsc`, execute Vitest once in non-interactive mode, and create the production build.

Concurrent runs for the same branch or pull request are cancelled when a newer run starts.

## Project Structure

```text
.
├── .github/workflows/ci.yml
├── README.md
├── jtech-tasklist-backend/
│   ├── composer/docker-compose.yml
│   ├── gradle/wrapper/
│   ├── src/main/java/br/com/jtech/tasklist/
│   │   ├── adapters/              # HTTP and persistence adapters
│   │   ├── application/           # Domain, use cases, and ports
│   │   └── config/                # Security, infrastructure, and wiring
│   ├── src/main/resources/
│   └── src/test/                  # Unit and integration tests
└── jtech-tasklist-frontend/
    ├── public/
    ├── src/
    │   ├── api/                   # Typed API modules and Axios client
    │   ├── modules/               # Auth, task-list, and task views
    │   ├── stores/                # Pinia state
    │   ├── router/                # Routes and guards
    │   ├── layouts/               # Public and authenticated layouts
    │   └── components/            # Shared UI components
    └── src/**/__tests__/          # Frontend tests
```

## Technical Decisions

- **Integrated full-stack flow:** the frontend uses the real backend instead of mocked task persistence, demonstrating end-to-end authentication, state synchronization, and error handling.
- **Backend as source of truth:** the browser persists session data only; application data is always loaded from the API.
- **Pragmatic Ports and Adapters:** business use cases depend on ports, while Spring controllers and JPA repositories remain at the edges without adding abstraction that the challenge does not need.
- **Refresh-token rotation:** refresh tokens are random values stored only as hashes, and each successful use revokes the previous token.
- **Ownership at the use-case boundary:** user isolation is enforced before resource reads or mutations, independent of frontend behavior.
- **H2 for integration tests:** controller integration tests remain self-contained and do not require a PostgreSQL service in CI.
- **Hibernate schema updates for the challenge:** `ddl-auto: update` keeps local setup small; a production evolution would use versioned migrations such as Flyway or Liquibase.

## Author

Developed by [Leandro Piasseta](https://github.com/leandropiassetta) as an independent implementation of the JTech technical challenge.
