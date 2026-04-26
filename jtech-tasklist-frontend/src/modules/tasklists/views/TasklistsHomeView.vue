<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import SurfacePanel from '@/components/SurfacePanel.vue'
import { useTasklistsStore, toTasklistSlug } from '@/stores/tasklists'
import { useTasksStore } from '@/stores/tasks'
import type { TasklistItem } from '@/types/tasklist'

const TASK_DESCRIPTION_LIMIT = 500

const route = useRoute()
const router = useRouter()
const tasklistsStore = useTasklistsStore()
const tasksStore = useTasksStore()

const createListForm = reactive({
  name: '',
})

const editListForm = reactive({
  tasklistId: '',
  name: '',
})

const createTaskForm = reactive({
  title: '',
  description: '',
})

const editTaskForm = reactive({
  taskId: '',
  title: '',
  description: '',
  completed: false,
})

const createListError = ref('')
const editListError = ref('')
const createTaskError = ref('')
const editTaskError = ref('')
const deleteListDialog = ref(false)
const deleteTaskDialog = ref(false)
const pendingDeleteListId = ref<string | null>(null)
const pendingDeleteTaskId = ref<string | null>(null)
const deleteListError = ref('')
const showTaskDescription = ref(false)

const currentTasklistSlug = computed(() =>
  typeof route.params.tasklistSlug === 'string' ? route.params.tasklistSlug : null,
)
const tasklists = computed(() => tasklistsStore.items)
const activeTasklist = computed(() => tasklistsStore.activeTasklist)
const activeTasklistId = computed(() => tasklistsStore.activeTasklistId)
const tasks = computed(() =>
  activeTasklistId.value ? tasksStore.tasksByTasklist(activeTasklistId.value) : [],
)
const isEmptyState = computed(() => tasklists.value.length === 0)
const isTaskEmptyState = computed(() => tasks.value.length === 0)
const completedCount = computed(() => tasks.value.filter((task) => task.completed).length)
const pendingCount = computed(() => tasks.value.length - completedCount.value)

const requiredRule = (value: string) => !!value?.trim() || 'Campo obrigatório.'
const descriptionRule = (value: string) =>
  value.trim().length <= TASK_DESCRIPTION_LIMIT ||
  `A descrição pode ter no máximo ${TASK_DESCRIPTION_LIMIT} caracteres.`

function formatTasklistName(name: string) {
  const trimmed = name.trim()
  if (!trimmed) return ''
  return trimmed.charAt(0).toUpperCase() + trimmed.slice(1)
}

function getTasklistRoute(tasklist: TasklistItem) {
  return {
    name: 'app-tasklist-details',
    params: { tasklistSlug: toTasklistSlug(tasklist.name) },
  }
}

function getCurrentRouteTasklist() {
  if (!currentTasklistSlug.value) return null
  return tasklistsStore.getListBySlug(currentTasklistSlug.value)
}

function syncTasklistFromRoute() {
  if (!tasklistsStore.hasItems) {
    tasklistsStore.setActiveTasklist(null)
    return
  }

  const routeTasklist = getCurrentRouteTasklist()

  if (routeTasklist) {
    tasklistsStore.setActiveTasklist(routeTasklist.id)
    return
  }

  const fallbackTasklist =
    tasklistsStore.activeTasklist ?? tasklistsStore.items[0] ?? null

  if (!fallbackTasklist) {
    tasklistsStore.setActiveTasklist(null)
    return
  }

  tasklistsStore.setActiveTasklist(fallbackTasklist.id)
  void router.replace(getTasklistRoute(fallbackTasklist))
}

onMounted(async () => {
  await tasklistsStore.fetchTasklists()
  syncTasklistFromRoute()
})

watch(
  () => [route.params.tasklistSlug, tasklistsStore.items.length],
  () => {
    if (!tasklistsStore.isLoading) {
      syncTasklistFromRoute()
    }
  },
)

watch(activeTasklistId, async (newId, oldId) => {
  if (newId && newId !== oldId) {
    await tasksStore.fetchTasks(newId)
  }
})

watch(
  () => createTaskForm.title,
  (value) => {
    if (value.trim()) {
      showTaskDescription.value = true
    }
  },
)

function startEditingTasklist(tasklist: TasklistItem) {
  editListForm.tasklistId = tasklist.id
  editListForm.name = tasklist.name
  editListError.value = ''
}

function stopEditingTasklist() {
  editListForm.tasklistId = ''
  editListForm.name = ''
  editListError.value = ''
}

function startEditingTask(taskId: string) {
  const task = tasks.value.find((item) => item.id === taskId)
  if (!task) return

  editTaskForm.taskId = task.id
  editTaskForm.title = task.title
  editTaskForm.description = task.description ?? ''
  editTaskForm.completed = task.completed
  editTaskError.value = ''
}

function stopEditingTask() {
  editTaskForm.taskId = ''
  editTaskForm.title = ''
  editTaskForm.description = ''
  editTaskForm.completed = false
  editTaskError.value = ''
}

async function handleCreateTasklist() {
  createListError.value = ''

  try {
    const tasklist = await tasklistsStore.createTasklist(createListForm.name)
    if (!tasklist) {
      createListError.value = tasklistsStore.error ?? 'Não foi possível criar a lista.'
      return
    }

    createListForm.name = ''
    await router.push(getTasklistRoute(tasklist))
  } catch (error) {
    createListError.value =
      error instanceof Error ? error.message : 'Não foi possível criar a lista.'
  }
}

async function handleRenameTasklist() {
  editListError.value = ''

  try {
    const tasklist = await tasklistsStore.renameTasklist(editListForm.tasklistId, editListForm.name)
    if (!tasklist) {
      editListError.value = tasklistsStore.error ?? 'Não foi possível renomear a lista.'
      return
    }

    stopEditingTasklist()

    if (activeTasklistId.value === tasklist.id) {
      await router.replace(getTasklistRoute(tasklist))
    }
  } catch (error) {
    editListError.value =
      error instanceof Error ? error.message : 'Não foi possível renomear a lista.'
  }
}

async function handleSelectTasklist(tasklist: TasklistItem) {
  tasklistsStore.setActiveTasklist(tasklist.id)
  await router.push(getTasklistRoute(tasklist))
}

function handleDeleteTasklistIntent(tasklistId: string) {
  pendingDeleteListId.value = tasklistId
  deleteListError.value = ''
  deleteListDialog.value = true
}

async function confirmDeleteTasklist() {
  if (!pendingDeleteListId.value) return

  deleteListError.value = ''
  const success = await tasklistsStore.deleteTasklist(pendingDeleteListId.value)

  if (!success) {
    deleteListError.value = tasklistsStore.error ?? 'Não foi possível excluir a lista.'
    return
  }

  deleteListDialog.value = false
  pendingDeleteListId.value = null

  if (tasklistsStore.activeTasklist) {
    await router.push(getTasklistRoute(tasklistsStore.activeTasklist))
    return
  }

  await router.push({ name: 'app-tasklists' })
}

async function handleCreateTask() {
  createTaskError.value = ''

  if (!activeTasklistId.value) return

  try {
    const createdTask = await tasksStore.createTask({
      title: createTaskForm.title,
      description: createTaskForm.description,
      tasklistId: activeTasklistId.value,
    })

    if (!createdTask) {
      createTaskError.value = tasksStore.error ?? 'Não foi possível adicionar a tarefa.'
      return
    }

    createTaskForm.title = ''
    createTaskForm.description = ''
    showTaskDescription.value = false
  } catch (error) {
    createTaskError.value =
      error instanceof Error ? error.message : 'Não foi possível adicionar a tarefa.'
  }
}

async function handleUpdateTask() {
  editTaskError.value = ''

  if (!activeTasklistId.value) return

  try {
    const updatedTask = await tasksStore.updateTask(editTaskForm.taskId, {
      title: editTaskForm.title,
      description: editTaskForm.description,
      completed: editTaskForm.completed,
      tasklistId: activeTasklistId.value,
    })

    if (!updatedTask) {
      editTaskError.value = tasksStore.error ?? 'Não foi possível atualizar a tarefa.'
      return
    }

    stopEditingTask()
  } catch (error) {
    editTaskError.value =
      error instanceof Error ? error.message : 'Não foi possível atualizar a tarefa.'
  }
}

async function handleToggleTask(taskId: string) {
  await tasksStore.toggleTask(taskId)
}

function handleDeleteTaskIntent(taskId: string) {
  pendingDeleteTaskId.value = taskId
  deleteTaskDialog.value = true
}

async function confirmDeleteTask() {
  if (!pendingDeleteTaskId.value) return

  const success = await tasksStore.removeTask(pendingDeleteTaskId.value)
  if (success) {
    pendingDeleteTaskId.value = null
    deleteTaskDialog.value = false
  }
}
</script>

<template>
  <v-container class="dashboard py-8">
    <v-row class="ga-0 dashboard-row" align="start">
      <v-col cols="12" lg="4" xl="3" class="dashboard-col dashboard-col--lists">
        <SurfacePanel
          label=""
          title="Suas listas"
          description="Crie listas para separar suas tarefas por assunto, projeto ou rotina."
          class="tasklists-panel"
        >
          <v-progress-linear
            v-if="tasklistsStore.isLoading"
            indeterminate
            color="primary"
            class="mt-4"
          />

          <v-alert
            v-if="tasklistsStore.error && !tasklistsStore.isLoading"
            class="mt-4"
            type="error"
            variant="tonal"
            density="compact"
            :text="tasklistsStore.error"
          />

          <div class="list-composer mt-6">
              <v-text-field
                v-model="createListForm.name"
                data-testid="tasklist-create-input"
                label="Nova lista"
                variant="outlined"
                color="primary"
              hide-details="auto"
              prepend-inner-icon="mdi-playlist-plus"
              :rules="[requiredRule]"
              :error-messages="createListError ? [createListError] : []"
              @keyup.enter="handleCreateTasklist"
            />
            <v-btn
              icon="mdi-plus"
              color="primary"
              variant="flat"
              size="small"
              data-testid="tasklist-create-submit"
              :loading="tasklistsStore.isLoading"
              @click="handleCreateTasklist"
            />
          </div>

          <div v-if="editListForm.tasklistId" class="mt-4">
            <v-form @submit.prevent="handleRenameTasklist">
              <v-text-field
                v-model="editListForm.name"
                label="Editar nome da lista"
                variant="outlined"
                color="primary"
                hide-details="auto"
                :rules="[requiredRule]"
                :error-messages="editListError ? [editListError] : []"
              />

              <div class="d-flex ga-2 mt-2">
                <v-btn
                  color="primary"
                  size="small"
                  type="submit"
                  :loading="tasklistsStore.isLoading"
                >
                  Salvar
                </v-btn>
                <v-btn variant="text" size="small" @click="stopEditingTasklist">Cancelar</v-btn>
              </div>
            </v-form>
          </div>

          <div v-if="isEmptyState && !tasklistsStore.isLoading" class="empty-state mt-6">
            <v-icon icon="mdi-format-list-bulleted-square" size="36" />
            <p class="text-medium-emphasis mt-3">
              Nenhuma lista criada ainda. Crie a primeira para começar a organizar suas tarefas.
            </p>
          </div>

          <v-list v-else class="mt-6 bg-transparent pa-0" density="comfortable">
            <v-list-item
              v-for="tasklist in tasklists"
              :key="tasklist.id"
              :active="tasklist.id === activeTasklistId"
              rounded="xl"
              class="mb-2 tasklist-item"
              :class="{ 'tasklist-item--active': tasklist.id === activeTasklistId }"
              @click="handleSelectTasklist(tasklist)"
            >
              <template #prepend>
                <v-icon icon="mdi-format-list-bulleted-square" />
              </template>

              <v-list-item-title class="tasklist-name">
                {{ formatTasklistName(tasklist.name) }}
              </v-list-item-title>

              <template #append>
                <div class="d-flex ga-1">
                  <v-btn
                    icon="mdi-pencil-outline"
                    size="small"
                    variant="text"
                    color="primary"
                    data-testid="tasklist-edit-button"
                    @click.stop="startEditingTasklist(tasklist)"
                  />
                  <v-btn
                    icon="mdi-trash-can-outline"
                    size="small"
                    variant="text"
                    color="error"
                    data-testid="tasklist-delete-button"
                    @click.stop="handleDeleteTasklistIntent(tasklist.id)"
                  />
                </div>
              </template>
            </v-list-item>
          </v-list>
        </SurfacePanel>
      </v-col>

      <v-col cols="12" lg="8" xl="9" class="dashboard-col dashboard-col--workspace">
        <SurfacePanel
          label=""
          :title="activeTasklist ? 'Tarefas da lista' : 'Selecione uma lista'"
          :description="
            activeTasklist
              ? 'Tudo o que você precisa acompanhar nesta lista está aqui.'
              : 'Escolha uma lista à esquerda para ver e organizar as tarefas dela.'
          "
          class="workspace-panel"
        >
          <template v-if="activeTasklist">
            <div class="workspace-context mt-7">
              <div class="workspace-context__label">Lista selecionada</div>
              <div class="workspace-context__name">
                {{ formatTasklistName(activeTasklist.name) }}
              </div>
            </div>

            <div class="task-toolbar mt-7">
              <div class="task-toolbar__copy">
                <p class="task-toolbar__summary">
                  {{
                    pendingCount > 0
                      ? `${pendingCount} tarefa(s) esperando sua atenção.`
                      : 'Tudo em dia nesta lista.'
                  }}
                </p>
                <p class="task-toolbar__caption">
                  As tarefas abaixo pertencem a esta lista.
                </p>
              </div>
              <span class="status-badge">
                {{ pendingCount }} em aberto
              </span>
            </div>

            <div class="task-composer mt-6">
              <div class="task-composer__row">
                <v-text-field
                  v-model="createTaskForm.title"
                  data-testid="task-create-title"
                  label="O que você precisa fazer?"
                  variant="plain"
                  color="primary"
                  hide-details="auto"
                  class="task-title-input"
                  :rules="[requiredRule]"
                  :error-messages="createTaskError ? [createTaskError] : []"
                  @focus="showTaskDescription = true"
                  @keyup.enter="handleCreateTask"
                />

                <v-btn
                  class="task-add-button"
                  color="primary"
                  size="small"
                  variant="flat"
                  prepend-icon="mdi-plus"
                  data-testid="task-create-submit"
                  :loading="tasksStore.isLoading"
                  @click="handleCreateTask"
                >
                  Adicionar
                </v-btn>
              </div>

              <v-expand-transition>
                <div v-if="showTaskDescription" class="task-composer__details">
                  <v-textarea
                    v-model="createTaskForm.description"
                    data-testid="task-create-description"
                    label="Detalhes (opcional)"
                    variant="plain"
                    color="primary"
                    rows="2"
                    auto-grow
                    counter="500"
                    hide-details="auto"
                    class="task-description-input"
                    :rules="[descriptionRule]"
                  />
                </div>
              </v-expand-transition>
            </div>

            <v-progress-linear
              v-if="tasksStore.isLoading"
              indeterminate
              color="primary"
              class="mt-4"
            />

            <v-alert
              v-if="tasksStore.error && !tasksStore.isLoading"
              class="mt-4"
              type="error"
              variant="tonal"
              density="compact"
              :text="tasksStore.error"
            />

            <div class="task-list-block mt-8">
              <div v-if="isTaskEmptyState && !tasksStore.isLoading" class="empty-state">
                <v-icon icon="mdi-check-circle-outline" size="36" />
                <p class="text-medium-emphasis mt-3">
                  Esta lista ainda está vazia. Adicione a primeira tarefa para começar.
                </p>
              </div>

              <v-list v-else class="bg-transparent pa-0" density="comfortable">
                <v-list-item
                  v-for="task in tasks"
                  :key="task.id"
                  rounded="xl"
                  class="task-item"
                  :class="{ 'task-item--completed': task.completed }"
                >
                  <template #prepend>
                    <v-checkbox-btn
                      :model-value="task.completed"
                      color="success"
                      data-testid="task-toggle"
                      @click.stop="handleToggleTask(task.id)"
                      @keydown.enter.prevent="handleToggleTask(task.id)"
                      @keydown.space.prevent="handleToggleTask(task.id)"
                    />
                  </template>

                  <v-list-item-title>
                    <span class="task-title">{{ task.title }}</span>
                  </v-list-item-title>
                  <v-list-item-subtitle v-if="task.description">
                    {{ task.description }}
                  </v-list-item-subtitle>

                  <template #append>
                    <div class="d-flex ga-2">
                      <v-btn
                        icon="mdi-pencil"
                        size="small"
                        variant="text"
                        color="primary"
                        data-testid="task-edit-button"
                        @click.stop="startEditingTask(task.id)"
                      />
                      <v-btn
                        icon="mdi-delete"
                        size="small"
                        variant="text"
                        color="error"
                        data-testid="task-delete-button"
                        @click.stop="handleDeleteTaskIntent(task.id)"
                      />
                    </div>
                  </template>
                </v-list-item>
              </v-list>
            </div>

            <div v-if="editTaskForm.taskId" class="task-editor mt-8">
              <v-form @submit.prevent="handleUpdateTask">
                <v-text-field
                  v-model="editTaskForm.title"
                  data-testid="task-edit-title"
                  label="Título da tarefa"
                  variant="outlined"
                  color="primary"
                  :rules="[requiredRule]"
                  :error-messages="editTaskError ? [editTaskError] : []"
                />

                <v-textarea
                  v-model="editTaskForm.description"
                  class="mt-3"
                  label="Detalhes (opcional)"
                  variant="outlined"
                  color="primary"
                  rows="2"
                  auto-grow
                  counter="500"
                  :rules="[descriptionRule]"
                />

                <v-switch
                  v-model="editTaskForm.completed"
                  class="mt-2"
                  color="success"
                  label="Tarefa concluída"
                />

                <div class="d-flex ga-3 mt-2">
                  <v-btn color="primary" type="submit" :loading="tasksStore.isLoading">
                    Salvar alterações
                  </v-btn>
                  <v-btn variant="text" color="primary" @click="stopEditingTask">Cancelar</v-btn>
                </div>
              </v-form>
            </div>

            <div class="overview-grid mt-10">
              <v-sheet rounded="xl" color="background" class="pa-5 overview-card overview-card--pending">
                <p class="stats-title">Pendentes</p>
                <h3 class="text-h2 mt-2">{{ pendingCount }}</h3>
                <p class="text-medium-emphasis mt-3">
                  Ainda precisam de ação.
                </p>
              </v-sheet>

              <v-sheet
                rounded="xl"
                color="background"
                class="pa-5 overview-card overview-card--completed"
              >
                <p class="stats-title">Concluídas</p>
                <h3 class="text-h2 mt-2">{{ completedCount }}</h3>
                <p class="text-medium-emphasis mt-3">
                  Já foram finalizadas.
                </p>
              </v-sheet>
            </div>
          </template>

          <div v-else class="empty-state mt-6">
            <v-icon icon="mdi-playlist-plus" size="36" />
            <p class="text-medium-emphasis mt-3">
              Escolha uma lista para ver as tarefas dela ou crie uma nova para começar.
            </p>
          </div>
        </SurfacePanel>
      </v-col>
    </v-row>

    <v-dialog v-model="deleteListDialog" max-width="420">
      <v-card rounded="xl">
        <v-card-title class="text-h6">Excluir lista?</v-card-title>
        <v-card-text>
          <p>Esta ação remove a lista e todas as tarefas vinculadas a ela. Não poderá ser desfeita.</p>
          <v-alert
            v-if="deleteListError"
            class="mt-4"
            type="error"
            variant="tonal"
            density="compact"
            :text="deleteListError"
          />
        </v-card-text>
        <v-card-actions class="pa-4">
          <v-spacer />
          <v-btn variant="text" @click="deleteListDialog = false">Cancelar</v-btn>
          <v-btn
            color="error"
            variant="flat"
            data-testid="tasklist-delete-confirm"
            :loading="tasklistsStore.isLoading"
            @click="confirmDeleteTasklist"
          >
            Excluir tudo
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="deleteTaskDialog" max-width="420">
      <v-card rounded="xl">
        <v-card-title class="text-h6">Excluir tarefa?</v-card-title>
        <v-card-text>Esta tarefa será removida permanentemente.</v-card-text>
        <v-card-actions class="pa-4">
          <v-spacer />
          <v-btn variant="text" @click="deleteTaskDialog = false">Cancelar</v-btn>
          <v-btn
            color="error"
            variant="flat"
            data-testid="task-delete-confirm"
            :loading="tasksStore.isLoading"
            @click="confirmDeleteTask"
          >
            Excluir
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<style scoped>
.dashboard {
  max-width: 1600px;
  margin: 0 auto;
}

.dashboard-row {
  row-gap: 1.5rem;
}

.dashboard :deep(.panel) {
  min-height: 100%;
}

.dashboard-col--lists {
  padding-right: 0;
}

.dashboard-col--workspace {
  padding-left: 0;
}

.tasklists-panel :deep(.panel) {
  max-width: 520px;
}

.workspace-panel :deep(.panel) {
  min-height: clamp(640px, 78vh, 820px);
}

:deep(.panel > h1) {
  font-family:
    'Inter',
    sans-serif;
  font-size: 3.35rem !important;
  font-weight: 900 !important;
  letter-spacing: -0.04em !important;
  line-height: 0.98;
  color: #111111 !important;
  margin-bottom: 10px;
}

:deep(.panel > p) {
  font-size: 1.1rem !important;
  color: #5f5f5f !important;
  line-height: 1.5;
  max-width: 48rem;
}

.workspace-context {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 1rem 1.1rem;
  border: 1px solid rgb(40 83 107 / 0.18);
  border-left: 5px solid rgb(13 43 77 / 0.82);
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgb(246 241 233 / 0.98), rgb(239 233 223 / 0.96));
  box-shadow:
    0 12px 24px rgb(84 74 53 / 0.08),
    0 1px 0 rgb(255 255 255 / 0.85) inset;
}

.workspace-context__label {
  font-size: 0.76rem;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgb(13 43 77 / 0.82);
}

.workspace-context__name {
  font-size: 1.28rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: rgb(10 27 43 / 0.98);
}

.list-composer :deep(.v-field) {
  border-radius: 18px;
  background: rgb(255 255 255 / 0.94);
  border: 1px solid rgb(96 88 67 / 0.15);
  box-shadow:
    0 8px 18px rgb(84 74 53 / 0.06),
    0 1px 0 rgb(255 255 255 / 0.8) inset;
}

.list-composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 1rem;
  align-items: start;
}

.list-composer :deep(.v-field__input) {
  min-height: 52px;
  font-size: 0.96rem;
  font-weight: 500;
  padding: 0.78rem 0.95rem 0.18rem !important;
}

.list-composer :deep(.v-label) {
  font-size: 0.95rem;
  color: rgb(84 74 53 / 0.62);
  margin-inline-start: 0.2rem;
  margin-top: -0.1rem;
}

.list-composer :deep(.v-btn) {
  width: 48px;
  height: 48px;
  border-radius: 999px;
  background: linear-gradient(180deg, #1b3958, #0d2b4d) !important;
  box-shadow: 0 8px 18px rgb(13 43 77 / 0.18);
}

.tasklists-panel :deep(.v-list) {
  margin-top: 1.35rem !important;
}

.tasklist-item {
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
  border: 1px solid rgb(96 88 67 / 0.05);
  background: rgb(255 255 255 / 0.72);
  box-shadow: 0 8px 18px rgb(84 74 53 / 0.04);
  border-left: 4px solid transparent;
  border-radius: 18px !important;
  min-height: 56px;
}

.tasklist-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 22px rgb(84 74 53 / 0.06);
}

.tasklist-item--active {
  border-left-color: rgb(var(--v-theme-primary));
  background: linear-gradient(180deg, rgb(231 225 216 / 0.96), rgb(224 217 207 / 0.92));
  box-shadow: 0 12px 24px rgb(84 74 53 / 0.08);
}

.tasklist-name {
  text-transform: capitalize;
  font-size: 0.96rem;
  font-weight: 600;
}

.tasklist-item :deep(.v-list-item__prepend) {
  padding-inline-end: 12px;
}

.tasklist-item :deep(.v-list-item__append) {
  padding-inline-start: 8px;
}

.tasklist-item :deep(.v-list-item__prepend > .v-icon) {
  color: rgb(13 43 77 / 0.68);
}

.tasklist-item--active :deep(.v-list-item__prepend > .v-icon) {
  color: rgb(13 43 77 / 0.92);
}

.task-composer {
  background: #ffffff;
  border: 2px solid rgb(197 173 115 / 0.56);
  border-radius: 28px;
  padding: 20px;
  box-shadow:
    0 14px 28px rgb(179 149 72 / 0.12),
    0 1px 0 rgb(255 255 255 / 0.85) inset;
  margin-top: 20px;
}

.task-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.9rem;
}

.task-toolbar__copy {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.task-toolbar__summary {
  color: rgb(22 22 22 / 0.92);
  font-size: 1.08rem;
  font-weight: 850;
  line-height: 1.2;
}

.task-toolbar__caption {
  color: rgb(84 74 53 / 0.68);
  font-size: 0.94rem;
  line-height: 1.4;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 0.75rem 1rem;
  border-radius: 999px;
  background: linear-gradient(180deg, rgb(255 255 255 / 0.9), rgb(246 241 233 / 0.92));
  border: 1px solid rgb(197 173 115 / 0.34);
  box-shadow:
    0 10px 22px rgb(84 74 53 / 0.08),
    0 1px 0 rgb(255 255 255 / 0.8) inset;
  color: rgb(13 43 77 / 0.92);
  font-size: 0.88rem;
  font-weight: 800;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  white-space: nowrap;
}

.task-composer__row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.9rem;
  align-items: stretch;
}

.task-composer__details {
  margin-top: 1.15rem;
}

.task-title-input :deep(.v-field) {
  border-radius: 20px;
  background: #fffefc;
  border: 1px solid rgb(84 74 53 / 0.14);
  box-shadow: 0 8px 18px rgb(84 74 53 / 0.08);
}

.task-title-input :deep(.v-field__outline) {
  display: none;
}

.task-title-input :deep(.v-field__input) {
  min-height: 72px;
  padding: 0.8rem 1.2rem 0.18rem !important;
  font-size: 1.5rem !important;
  font-weight: 800 !important;
  color: #111 !important;
  letter-spacing: -0.02em;
}

.task-title-input :deep(.v-label) {
  color: rgb(84 74 53 / 0.58);
  font-weight: 500;
  margin-inline-start: 0.2rem;
  margin-top: -0.15rem;
}

.task-title-input :deep(.v-input__details) {
  padding-inline: 0;
}

.task-title-input {
  position: relative;
}

.task-title-input:focus-within :deep(.v-field) {
  border-color: rgb(197 173 115 / 0.68);
  box-shadow:
    0 0 0 4px rgb(197 173 115 / 0.12),
    0 10px 20px rgb(84 74 53 / 0.1);
}

.task-description-input {
  background: #fffefb !important;
  border-radius: 20px !important;
  margin-top: 16px;
  padding: 16px 18px !important;
  border: 1px solid rgb(84 74 53 / 0.14);
  box-shadow: 0 8px 18px rgb(84 74 53 / 0.06);
}

.task-description-input :deep(.v-field) {
  background: transparent;
  box-shadow: none;
}

.task-description-input :deep(.v-field__outline) {
  display: none;
}

.task-description-input :deep(.v-field__input) {
  padding-inline: 0.1rem;
  font-size: 0.95rem;
  line-height: 1.5;
}

.task-add-button {
  min-width: 104px;
  min-height: 44px;
  align-self: center;
  font-weight: 700;
  letter-spacing: 0.01em;
  border-radius: 14px;
  background: linear-gradient(180deg, #2d3e50 0%, #0d2b4d 100%) !important;
  color: #fff !important;
  text-transform: none !important;
  box-shadow: 0 8px 20px -6px rgb(13 43 77 / 0.28);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    filter 0.2s ease !important;
}

.task-add-button:hover {
  transform: translateY(-1px);
  filter: brightness(1.03);
  box-shadow: 0 14px 28px -8px rgb(13 43 77 / 0.38);
}

.task-list-block {
  padding-top: 0.3rem;
}

.task-item {
  background: #ffffff !important;
  border: 1px solid rgb(84 74 53 / 0.12) !important;
  border-radius: 22px !important;
  margin-bottom: 12px !important;
  padding: 14px 16px 14px 12px !important;
  box-shadow: 0 8px 16px rgb(84 74 53 / 0.05) !important;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease,
    background-color 0.2s ease;
}

.task-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 26px rgb(13 43 77 / 0.06) !important;
  border-color: #d97706 !important;
}

.task-item--completed {
  background: linear-gradient(180deg, rgb(251 251 249 / 0.96), rgb(246 246 243 / 0.94)) !important;
  border-color: rgb(84 74 53 / 0.09) !important;
}

.task-title {
  font-size: 1.12rem !important;
  font-weight: 800 !important;
  color: #0f172a !important;
  display: block !important;
  opacity: 1 !important;
}

.task-item--completed .task-title {
  text-decoration: line-through;
  text-decoration-thickness: 1.5px;
  text-decoration-color: rgb(84 74 53 / 0.26);
  color: rgb(15 23 42 / 0.62) !important;
}

.task-item :deep(.v-list-item-subtitle) {
  font-size: 0.9rem !important;
  color: #64748b !important;
  margin-top: 2px;
  line-height: 1.45;
}

.task-item--completed :deep(.v-list-item-subtitle) {
  color: rgb(100 116 139 / 0.72) !important;
}

.task-item :deep(.v-selection-control) {
  margin-inline-end: 0.4rem;
}

.task-editor {
  padding: 1.25rem;
  border: 1px solid rgb(84 74 53 / 0.1);
  border-radius: 24px;
  background: rgb(255 255 255 / 0.78);
  box-shadow: 0 10px 22px rgb(84 74 53 / 0.05);
}

.overview-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-top: 40px;
}

.overview-card {
  background: #ffffff !important;
  border: 1px solid rgb(84 74 53 / 0.13) !important;
  border-radius: 24px !important;
  border-bottom: 5px solid transparent !important;
  padding: 32px !important;
  text-align: left;
  box-shadow: 0 12px 20px rgb(84 74 53 / 0.05);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.overview-card--pending {
  border-bottom-color: rgb(13 43 77 / 0.22) !important;
  background:
    linear-gradient(180deg, rgb(255 255 255 / 1), rgb(249 251 253 / 1)) !important;
}

.overview-card--completed {
  border-bottom-color: rgb(46 125 50 / 0.26) !important;
  background:
    linear-gradient(180deg, rgb(255 255 255 / 1), rgb(248 252 248 / 1)) !important;
}

.overview-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 30px rgb(84 74 53 / 0.08);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  padding: 1.5rem;
  border: 1px dashed rgb(40 83 107 / 0.16);
  border-radius: 24px;
  text-align: center;
}

.stats-title {
  font-size: 0.75rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.15em;
  color: #8592a3;
}

.overview-card h3 {
  font-size: 3.1rem !important;
  font-weight: 900;
  color: #1e293b;
  margin: 8px 0;
}

.overview-card :deep(.text-medium-emphasis) {
  color: rgb(84 74 53 / 0.72) !important;
}

.tasklist-item {
  border-radius: 16px !important;
  font-weight: 700 !important;
  color: #475569 !important;
}

@media (max-width: 1279px) {
  .dashboard-col--lists,
  .dashboard-col--workspace {
    padding-left: 0;
    padding-right: 0;
  }

  .tasklists-panel :deep(.panel) {
    max-width: none;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }
}

@media (min-width: 1280px) {
  .dashboard-row {
    margin-left: -0.75rem;
    margin-right: -0.75rem;
  }

  .dashboard-col {
    padding-left: 0.75rem;
    padding-right: 0.75rem;
  }
}

@media (max-width: 720px) {
  .task-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .status-badge {
    align-self: flex-start;
  }

  .task-composer__row {
    grid-template-columns: 1fr;
  }

  .task-add-button {
    width: 100%;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
