<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import SurfacePanel from '@/components/SurfacePanel.vue'
import { useTasklistsStore } from '@/stores/tasklists'
import { useTasksStore } from '@/stores/tasks'

const TASK_DESCRIPTION_LIMIT = 500

const route = useRoute()
const router = useRouter()
const tasklistsStore = useTasklistsStore()
const tasksStore = useTasksStore()

const createForm = reactive({
  title: '',
  description: '',
})

const editForm = reactive({
  taskId: '',
  title: '',
  description: '',
  completed: false,
})

const createError = ref('')
const editError = ref('')
const deleteDialog = ref(false)
const pendingDeleteTaskId = ref<string | null>(null)

const activeTasklist = computed(() => tasklistsStore.activeTasklist)
const activeTasklistId = computed(() => tasklistsStore.activeTasklistId)
const currentTasklistId = computed(() =>
  typeof route.params.tasklistId === 'string' ? route.params.tasklistId : null,
)
const tasks = computed(() =>
  activeTasklistId.value ? tasksStore.tasksByTasklist(activeTasklistId.value) : [],
)
const isEmptyState = computed(() => tasks.value.length === 0)
const completedCount = computed(() => tasks.value.filter((task) => task.completed).length)
const pendingCount = computed(() => tasks.value.length - completedCount.value)
const requiredRule = (value: string) => !!value?.trim() || 'Campo obrigatório.'
const descriptionRule = (value: string) =>
  value.trim().length <= TASK_DESCRIPTION_LIMIT ||
  `A descrição pode ter no máximo ${TASK_DESCRIPTION_LIMIT} caracteres.`

async function syncTasklistContext() {
  if (!tasklistsStore.hasItems) {
    await router.replace({ name: 'app-tasklists' })
    return
  }

  if (!currentTasklistId.value) {
    const fallbackId = tasklistsStore.activeTasklistId ?? tasklistsStore.items[0].id
    tasklistsStore.setActiveTasklist(fallbackId)
    await router.replace({
      name: 'app-tasklist-details',
      params: { tasklistId: fallbackId },
    })
    return
  }

  const hasTasklist = tasklistsStore.items.some(
    (tasklist) => tasklist.id === currentTasklistId.value,
  )

  if (!hasTasklist) {
    await router.replace({ name: 'app-tasklists' })
    return
  }

  tasklistsStore.setActiveTasklist(currentTasklistId.value)
}

onMounted(async () => {
  if (!tasklistsStore.hasItems) {
    await tasklistsStore.fetchTasklists()
  }

  await syncTasklistContext()

  if (activeTasklistId.value) {
    await tasksStore.fetchTasks(activeTasklistId.value)
  }
})

watch(currentTasklistId, async (newId, oldId) => {
  if (newId && newId !== oldId) {
    tasklistsStore.setActiveTasklist(newId)
    await tasksStore.fetchTasks(newId)
  }
})

function startEditing(taskId: string) {
  const task = tasks.value.find((item) => item.id === taskId)

  if (!task) {
    return
  }

  editForm.taskId = task.id
  editForm.title = task.title
  editForm.description = task.description ?? ''
  editForm.completed = task.completed
  editError.value = ''
}

function stopEditing() {
  editForm.taskId = ''
  editForm.title = ''
  editForm.description = ''
  editForm.completed = false
  editError.value = ''
}

async function handleCreateTask() {
  createError.value = ''

  if (!activeTasklistId.value) {
    return
  }

  try {
    const task = await tasksStore.createTask({
      title: createForm.title,
      description: createForm.description,
      tasklistId: activeTasklistId.value,
    })

    if (task) {
      createForm.title = ''
      createForm.description = ''
    } else {
      createError.value = tasksStore.error ?? 'Não foi possível criar a task.'
    }
  } catch (error) {
    createError.value = error instanceof Error ? error.message : 'Não foi possível criar a task.'
  }
}

async function handleUpdateTask() {
  editError.value = ''

  if (!activeTasklistId.value) {
    return
  }

  try {
    const result = await tasksStore.updateTask(editForm.taskId, {
      title: editForm.title,
      description: editForm.description,
      completed: editForm.completed,
      tasklistId: activeTasklistId.value,
    })

    if (result) {
      stopEditing()
    } else {
      editError.value = tasksStore.error ?? 'Não foi possível atualizar a task.'
    }
  } catch (error) {
    editError.value =
      error instanceof Error ? error.message : 'Não foi possível atualizar a task.'
  }
}

async function handleToggleTask(taskId: string) {
  await tasksStore.toggleTask(taskId)
}

function handleDeleteIntent(taskId: string) {
  pendingDeleteTaskId.value = taskId
  deleteDialog.value = true
}

async function confirmDeleteTask() {
  if (!pendingDeleteTaskId.value) {
    return
  }

  const success = await tasksStore.removeTask(pendingDeleteTaskId.value)

  if (success) {
    pendingDeleteTaskId.value = null
    deleteDialog.value = false
  }
}
</script>

<template>
  <v-container class="py-8">
    <v-row class="ga-0" align="stretch">
      <v-col cols="12" lg="5">
        <SurfacePanel
          label="Tasks"
          :title="activeTasklist ? activeTasklist.name : 'Tasks por lista'"
          description="Adicione, conclua, edite e exclua tarefas dentro da lista selecionada."
        >
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

          <v-form class="mt-6" @submit.prevent="handleCreateTask">
            <v-text-field
              v-model="createForm.title"
              data-testid="task-create-title"
              label="Nova task"
              variant="outlined"
              color="primary"
              prepend-inner-icon="mdi-plus-circle-outline"
              :rules="[requiredRule]"
              :error-messages="createError ? [createError] : []"
            />

            <v-textarea
              v-model="createForm.description"
              data-testid="task-create-description"
              class="mt-3"
              label="Descrição"
              variant="outlined"
              color="primary"
              rows="3"
              auto-grow
              counter="500"
              :rules="[descriptionRule]"
            />

            <v-btn
              class="mt-2"
              block
              color="primary"
              prepend-icon="mdi-plus"
              type="submit"
              data-testid="task-create-submit"
              :loading="tasksStore.isLoading"
            >
              Criar task
            </v-btn>
          </v-form>

          <div v-if="isEmptyState && !tasksStore.isLoading" class="empty-state mt-6">
            <v-icon icon="mdi-check-circle-outline" size="36" />
            <p class="text-medium-emphasis mt-3">
              Ainda não há tasks nesta lista. Crie a primeira task para começar a organizar o fluxo.
            </p>
          </div>

          <v-list v-else class="mt-6 bg-transparent pa-0" density="comfortable">
            <v-list-item
              v-for="task in tasks"
              :key="task.id"
              rounded="xl"
              class="mb-2 task-item"
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

              <v-list-item-title class="task-title">{{ task.title }}</v-list-item-title>
              <v-list-item-subtitle>
                {{ task.description || 'Sem descrição' }}
              </v-list-item-subtitle>

              <template #append>
                <div class="d-flex ga-1">
                  <v-btn
                    icon="mdi-pencil-outline"
                    size="small"
                    variant="text"
                    color="primary"
                    data-testid="task-edit-button"
                    @click.stop="startEditing(task.id)"
                  />
                  <v-btn
                    icon="mdi-trash-can-outline"
                    size="small"
                    variant="text"
                    color="error"
                    data-testid="task-delete-button"
                    @click.stop="handleDeleteIntent(task.id)"
                  />
                </div>
              </template>
            </v-list-item>
          </v-list>
        </SurfacePanel>
      </v-col>

      <v-col cols="12" lg="7">
        <SurfacePanel
          label="Resumo"
          :title="activeTasklist ? `${activeTasklist.name} em foco` : 'Selecione uma lista'"
          :description="
            activeTasklist
              ? 'As tarefas mais recentes aparecem primeiro para manter o trabalho recente ao alcance.'
              : 'Volte para listas e escolha uma categoria válida para continuar.'
          "
        >
          <v-chip
            v-if="activeTasklist"
            class="mt-4"
            color="secondary"
            variant="flat"
            prepend-icon="mdi-timer-outline"
          >
            {{ pendingCount }} pendente(s) • {{ completedCount }} concluída(s)
          </v-chip>

          <v-form v-if="editForm.taskId" class="mt-6" @submit.prevent="handleUpdateTask">
            <v-text-field
              v-model="editForm.title"
              data-testid="task-edit-title"
              label="Editar título"
              variant="outlined"
              color="primary"
              prepend-inner-icon="mdi-pencil-outline"
              :rules="[requiredRule]"
              :error-messages="editError ? [editError] : []"
            />

            <v-textarea
              v-model="editForm.description"
              class="mt-3"
              label="Editar descrição"
              variant="outlined"
              color="primary"
              rows="3"
              auto-grow
              counter="500"
              :rules="[descriptionRule]"
            />

            <v-switch
              v-model="editForm.completed"
              class="mt-2"
              color="success"
              label="Task concluída"
            />

            <div class="d-flex flex-wrap ga-3 mt-2">
              <v-btn
                color="primary"
                prepend-icon="mdi-content-save-outline"
                type="submit"
                :loading="tasksStore.isLoading"
              >
                Salvar task
              </v-btn>
              <v-btn variant="text" color="primary" @click="stopEditing">Cancelar</v-btn>
            </div>
          </v-form>

          <div v-else-if="activeTasklist" class="mt-6 overview-grid">
            <v-sheet rounded="xl" color="background" class="pa-5 overview-card">
              <p class="section-label">Em andamento</p>
              <h3 class="text-h3 mt-2">{{ pendingCount }}</h3>
              <p class="text-medium-emphasis mt-3">
                task(s) ainda pedem ação nesta lista.
              </p>
            </v-sheet>

            <v-sheet rounded="xl" color="background" class="pa-5 overview-card">
              <p class="section-label">Concluídas</p>
              <h3 class="text-h3 mt-2">{{ completedCount }}</h3>
              <p class="text-medium-emphasis mt-3">
                task(s) já foram finalizadas neste contexto.
              </p>
            </v-sheet>
          </div>
        </SurfacePanel>
      </v-col>
    </v-row>

    <v-dialog v-model="deleteDialog" max-width="420">
      <v-card rounded="xl">
        <v-card-title class="text-h6">Excluir task?</v-card-title>
        <v-card-text>Esta ação é permanente e não pode ser desfeita.</v-card-text>
        <v-card-actions class="pa-4">
          <v-spacer />
          <v-btn variant="text" @click="deleteDialog = false">Cancelar</v-btn>
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
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 1.25rem;
  border: 1px dashed rgb(40 83 107 / 0.18);
  border-radius: 1.25rem;
  color: rgb(40 83 107 / 0.7);
}

.overview-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.overview-card {
  border: 1px solid rgb(40 83 107 / 0.08);
}

.section-label {
  color: #c2944a;
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.task-item {
  border: 1px solid rgb(40 83 107 / 0.08);
}

.task-item--completed {
  background: rgb(79 124 90 / 0.08);
}

.task-item--completed .task-title {
  text-decoration: line-through;
  color: rgb(40 83 107 / 0.56);
}

@media (max-width: 960px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
