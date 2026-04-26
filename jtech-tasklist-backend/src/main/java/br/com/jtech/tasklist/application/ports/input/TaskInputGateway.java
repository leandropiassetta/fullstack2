package br.com.jtech.tasklist.application.ports.input;

import br.com.jtech.tasklist.application.core.domains.Task;

import java.util.List;

public interface TaskInputGateway {

    Task create(Task task, String authenticatedUserId);

    List<Task> findAll(String authenticatedUserId, String tasklistId);

    Task findById(String taskId, String authenticatedUserId);

    Task update(String taskId, Task task, String authenticatedUserId);

    void delete(String taskId, String authenticatedUserId);

}
