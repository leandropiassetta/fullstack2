package br.com.jtech.tasklist.adapters.output;

import br.com.jtech.tasklist.adapters.output.repositories.TaskRepository;
import br.com.jtech.tasklist.adapters.output.repositories.TasklistRepository;
import br.com.jtech.tasklist.adapters.output.repositories.UserRepository;
import br.com.jtech.tasklist.adapters.output.repositories.entities.TaskEntity;
import br.com.jtech.tasklist.application.core.domains.Task;
import br.com.jtech.tasklist.application.ports.output.TaskOutputGateway;
import br.com.jtech.tasklist.config.infra.exceptions.TaskNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TaskAdapter implements TaskOutputGateway {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TasklistRepository tasklistRepository;

    @Override
    public Task create(Task task) {
        TaskEntity entity = taskRepository.save(TaskEntity.builder()
                .title(Task.normalizeTitle(task.getTitle()))
                .description(Task.normalizeDescription(task.getDescription()))
                .completed(Boolean.TRUE.equals(task.getCompleted()))
                .owner(userRepository.getReferenceById(UUID.fromString(task.getOwnerId())))
                .tasklist(tasklistRepository.getReferenceById(UUID.fromString(task.getTasklistId())))
                .build());

        return Task.of(entity);
    }

    @Override
    public List<Task> findAllByOwnerId(String ownerId) {
        return Task.of(taskRepository.findAllByOwnerIdOrderByCreatedAtDesc(UUID.fromString(ownerId)));
    }

    @Override
    public List<Task> findAllByOwnerIdAndTasklistId(String ownerId, String tasklistId) {
        return Task.of(taskRepository.findAllByOwnerIdAndTasklistIdOrderByCreatedAtDesc(
                UUID.fromString(ownerId),
                UUID.fromString(tasklistId)
        ));
    }

    @Override
    public Optional<Task> findById(String taskId) {
        return taskRepository.findById(UUID.fromString(taskId))
                .map(Task::of);
    }

    @Override
    public boolean existsByTasklistIdAndTitle(String tasklistId, String title) {
        return taskRepository.existsByTasklistIdAndNormalizedTitle(
                UUID.fromString(tasklistId),
                Task.normalizeTitle(title)
        );
    }

    @Override
    public boolean existsByTasklistIdAndTitleAndIdNot(String tasklistId, String title, String taskId) {
        return taskRepository.existsByTasklistIdAndNormalizedTitleAndIdNot(
                UUID.fromString(tasklistId),
                Task.normalizeTitle(title),
                UUID.fromString(taskId)
        );
    }

    @Override
    public Task update(Task task) {
        TaskEntity entity = taskRepository.findById(UUID.fromString(task.getId()))
                .orElseThrow(TaskNotFoundException::new);

        entity.setTitle(Task.normalizeTitle(task.getTitle()));
        entity.setDescription(Task.normalizeDescription(task.getDescription()));
        entity.setCompleted(Task.requireCompleted(task.getCompleted()));
        entity.setTasklist(tasklistRepository.getReferenceById(UUID.fromString(task.getTasklistId())));

        return Task.of(taskRepository.save(entity));
    }

    @Override
    public void deleteById(String taskId) {
        taskRepository.deleteById(UUID.fromString(taskId));
    }

}
