package br.com.jtech.tasklist.application.ports.output;

import br.com.jtech.tasklist.application.core.domains.Task;

import java.util.List;
import java.util.Optional;

public interface TaskOutputGateway {

    Task create(Task task);

    List<Task> findAllByOwnerId(String ownerId);

    List<Task> findAllByOwnerIdAndTasklistId(String ownerId, String tasklistId);

    Optional<Task> findById(String taskId);

    boolean existsByTasklistIdAndTitle(String tasklistId, String title);

    boolean existsByTasklistIdAndTitleAndIdNot(String tasklistId, String title, String taskId);

    Task update(Task task);

    void deleteById(String taskId);

}
