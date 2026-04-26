package br.com.jtech.tasklist.application.core.usecases;

import br.com.jtech.tasklist.application.core.domains.Task;
import br.com.jtech.tasklist.application.core.domains.Tasklist;
import br.com.jtech.tasklist.application.ports.input.TaskInputGateway;
import br.com.jtech.tasklist.application.ports.output.TaskOutputGateway;
import br.com.jtech.tasklist.application.ports.output.TasklistOutputGateway;
import br.com.jtech.tasklist.config.infra.exceptions.ForbiddenException;
import br.com.jtech.tasklist.config.infra.exceptions.TaskAlreadyExistsException;
import br.com.jtech.tasklist.config.infra.exceptions.TaskNotFoundException;
import br.com.jtech.tasklist.config.infra.exceptions.TasklistNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class TaskUseCase implements TaskInputGateway {

    private final TaskOutputGateway taskOutputGateway;
    private final TasklistOutputGateway tasklistOutputGateway;

    public TaskUseCase(TaskOutputGateway taskOutputGateway,
                       TasklistOutputGateway tasklistOutputGateway) {
        this.taskOutputGateway = taskOutputGateway;
        this.tasklistOutputGateway = tasklistOutputGateway;
    }

    @Override
    @Transactional
    public Task create(Task task, String authenticatedUserId) {
        Tasklist tasklist = getOwnedTasklist(task.getTasklistId(), authenticatedUserId);
        String normalizedTitle = Task.normalizeTitle(task.getTitle());

        if (taskOutputGateway.existsByTasklistIdAndTitle(tasklist.getId(), normalizedTitle)) {
            throw new TaskAlreadyExistsException();
        }

        task.setTitle(normalizedTitle);
        task.setDescription(Task.normalizeDescription(task.getDescription()));
        task.setOwnerId(authenticatedUserId);
        task.setTasklistId(tasklist.getId());
        task.setCompleted(false);

        return taskOutputGateway.create(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> findAll(String authenticatedUserId, String tasklistId) {
        if (tasklistId == null || tasklistId.isBlank()) {
            return taskOutputGateway.findAllByOwnerId(authenticatedUserId);
        }

        Tasklist tasklist = getOwnedTasklist(tasklistId, authenticatedUserId);
        return taskOutputGateway.findAllByOwnerIdAndTasklistId(authenticatedUserId, tasklist.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Task findById(String taskId, String authenticatedUserId) {
        return getOwnedTask(taskId, authenticatedUserId);
    }

    @Override
    @Transactional
    public Task update(String taskId, Task task, String authenticatedUserId) {
        Task existingTask = getOwnedTask(taskId, authenticatedUserId);
        Tasklist tasklist = getOwnedTasklist(task.getTasklistId(), authenticatedUserId);
        String normalizedTitle = Task.normalizeTitle(task.getTitle());
        Boolean completed = Task.requireCompleted(task.getCompleted());

        if (taskOutputGateway.existsByTasklistIdAndTitleAndIdNot(tasklist.getId(), normalizedTitle, taskId)) {
            throw new TaskAlreadyExistsException();
        }

        existingTask.setTitle(normalizedTitle);
        existingTask.setDescription(Task.normalizeDescription(task.getDescription()));
        existingTask.setCompleted(completed);
        existingTask.setTasklistId(tasklist.getId());

        return taskOutputGateway.update(existingTask);
    }

    @Override
    @Transactional
    public void delete(String taskId, String authenticatedUserId) {
        Task existingTask = getOwnedTask(taskId, authenticatedUserId);
        taskOutputGateway.deleteById(existingTask.getId());
    }

    private Task getOwnedTask(String taskId, String authenticatedUserId) {
        Task task = taskOutputGateway.findById(taskId)
                .orElseThrow(TaskNotFoundException::new);

        if (!task.belongsTo(authenticatedUserId)) {
            throw new ForbiddenException("You do not have access to this task");
        }

        return task;
    }

    private Tasklist getOwnedTasklist(String tasklistId, String authenticatedUserId) {
        Tasklist tasklist = tasklistOutputGateway.findById(tasklistId)
                .orElseThrow(TasklistNotFoundException::new);

        if (!tasklist.belongsTo(authenticatedUserId)) {
            throw new ForbiddenException("You do not have access to this tasklist");
        }

        return tasklist;
    }

}
