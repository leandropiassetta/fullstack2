package br.com.jtech.tasklist.adapters.output;

import br.com.jtech.tasklist.adapters.output.repositories.TaskRepository;
import br.com.jtech.tasklist.adapters.output.repositories.TasklistRepository;
import br.com.jtech.tasklist.adapters.output.repositories.UserRepository;
import br.com.jtech.tasklist.adapters.output.repositories.entities.TasklistEntity;
import br.com.jtech.tasklist.application.core.domains.Tasklist;
import br.com.jtech.tasklist.application.ports.output.TasklistOutputGateway;
import br.com.jtech.tasklist.config.infra.exceptions.TasklistNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TasklistAdapter implements TasklistOutputGateway {

    private final TasklistRepository tasklistRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    @Override
    public Tasklist create(Tasklist tasklist) {
        TasklistEntity entity = TasklistEntity.builder()
                .name(Tasklist.normalizeName(tasklist.getName()))
                .owner(userRepository.getReferenceById(UUID.fromString(tasklist.getOwnerId())))
                .build();

        return Tasklist.of(tasklistRepository.save(entity));
    }

    @Override
    public List<Tasklist> findAllByOwnerId(String ownerId) {
        return Tasklist.of(tasklistRepository.findAllByOwnerIdOrderByNameAsc(UUID.fromString(ownerId)));
    }

    @Override
    public Optional<Tasklist> findById(String tasklistId) {
        return tasklistRepository.findById(UUID.fromString(tasklistId))
                .map(Tasklist::of);
    }

    @Override
    public boolean existsByOwnerIdAndName(String ownerId, String name) {
        return tasklistRepository.existsByOwnerIdAndNormalizedName(
                UUID.fromString(ownerId),
                Tasklist.normalizeName(name)
        );
    }

    @Override
    public boolean existsByOwnerIdAndNameAndIdNot(String ownerId, String name, String tasklistId) {
        return tasklistRepository.existsByOwnerIdAndNormalizedNameAndIdNot(
                UUID.fromString(ownerId),
                Tasklist.normalizeName(name),
                UUID.fromString(tasklistId)
        );
    }

    @Override
    public Tasklist update(Tasklist tasklist) {
        TasklistEntity entity = tasklistRepository.findById(UUID.fromString(tasklist.getId()))
                .orElseThrow(TasklistNotFoundException::new);

        entity.setName(Tasklist.normalizeName(tasklist.getName()));
        return Tasklist.of(tasklistRepository.save(entity));
    }

    @Override
    public boolean hasTasks(String tasklistId) {
        return taskRepository.existsByTasklistId(UUID.fromString(tasklistId));
    }

    @Override
    public void deleteById(String tasklistId) {
        UUID tasklistUuid = UUID.fromString(tasklistId);
        taskRepository.deleteAllByTasklistId(tasklistUuid);
        tasklistRepository.deleteById(tasklistUuid);
    }

}
