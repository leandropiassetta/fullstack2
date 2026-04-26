# jtech-tasklist-frontend

Frontend da aplicação construído com `Vue 3`, `Pinia`, `Vue Router` e `Vuetify`.

## Visão Geral

Organizei o frontend por domínio para deixar os fluxos principais mais fáceis de entender e manter.

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
  types/
  components/
```

Essa estrutura distribui melhor as responsabilidades entre:

- telas e fluxos por domínio
- estado global
- rotas e guards
- integração HTTP
- componentes compartilhados

## Stack

- `Vue 3` com Composition API
- `TypeScript`
- `Pinia`
- `Vue Router 4`
- `Vuetify`
- `Axios`
- `Vitest`

## Decisões da fundação

- `modules/` concentra as áreas de domínio da interface.
- `stores/` guarda o estado global compartilhado, com a API como fonte de verdade.
- `api/` concentra os módulos HTTP com tipagem explícita.
- `layouts/` separa o contexto público (`PublicLayout`) da área autenticada (`AppLayout`).
- `plugins/` centraliza o bootstrap de `Pinia` e `Vuetify`.

## Variáveis de ambiente

```env
VITE_API_BASE_URL=http://localhost:8080
```

Crie um arquivo `.env` na raiz do frontend com essa variável antes de rodar a aplicação.

## Rotas

- `/login` é pública
- `/app/*` é protegida por guard
- visitante não autenticado vai para `/login`
- usuário autenticado que acessa `/login` é redirecionado para `/app/tasklists`
- listas usam:
  - `/app/tasklists`
  - `/app/tasklists/:tasklistSlug`
- tasks operam dentro da rota da lista ativa:
  - `/app/tasklists/:tasklistSlug`

## Persistência

No frontend, apenas a sessão de autenticação persiste em `localStorage`:

- `user`
- `accessToken`
- `refreshToken`
- `isAuthenticated`

Listas e tarefas são carregadas da API a cada sessão.

## Fluxo de autenticação

- a tela `/login` alterna entre Entrar e Cadastrar
- login chama `POST /auth/login`
- cadastro chama `POST /auth/register` e depois faz login automático
- refresh token é tratado por interceptor do Axios
- falha de refresh faz logout e redireciona para `/login`
- `logout` limpa o estado global e o `localStorage`

## Listas e tarefas

### Tasklists

- CRUD de listas via API (`/api/v1/tasklists`)
- validação local de nome obrigatório
- backend valida duplicidade e ownership
- lista ativa sincronizada com a URL
- exclusão pede confirmação explícita e remove em cascata as tarefas associadas
- após exclusão bem-sucedida, tasks locais da lista são removidas do estado

### Tasks

- CRUD de tasks via API (`/api/v1/tasks`)
- tasks sempre pertencem à lista ativa
- toggle de conclusão envia o body completo da task
- `description` é opcional e limitada a 500 caracteres
- ordenação vem do backend por `createdAt` DESC

## Como rodar

```sh
npm install
npm run dev
```

## Testes

```sh
npm run test
```

Comando equivalente:

```sh
npm run test:unit
```

Cobertura atual:

- stores de auth, tasklists e tasks
- guards de rota
- testes de UI de login, listas e tarefas

## Qualidade

```sh
npm run build
npm run lint
```
