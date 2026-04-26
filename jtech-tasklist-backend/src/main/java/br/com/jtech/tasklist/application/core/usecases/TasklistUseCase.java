package br.com.jtech.tasklist.application.core.usecases;

import br.com.jtech.tasklist.application.core.domains.Tasklist;
import br.com.jtech.tasklist.application.ports.input.TasklistInputGateway;
import br.com.jtech.tasklist.application.ports.output.TasklistOutputGateway;
import br.com.jtech.tasklist.config.infra.exceptions.ForbiddenException;
import br.com.jtech.tasklist.config.infra.exceptions.TasklistAlreadyExistsException;
import br.com.jtech.tasklist.config.infra.exceptions.TasklistNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class TasklistUseCase implements TasklistInputGateway {

    private final TasklistOutputGateway tasklistOutputGateway;

    public TasklistUseCase(TasklistOutputGateway tasklistOutputGateway) {
        this.tasklistOutputGateway = tasklistOutputGateway;
    }

    @Override
    @Transactional
    public Tasklist create(Tasklist tasklist, String authenticatedUserId) {
        String normalizedName = Tasklist.normalizeName(tasklist.getName());

        if (tasklistOutputGateway.existsByOwnerIdAndName(authenticatedUserId, normalizedName)) {
            throw new TasklistAlreadyExistsException();
        }

        tasklist.setName(normalizedName);
        tasklist.setOwnerId(authenticatedUserId);
        return tasklistOutputGateway.create(tasklist);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tasklist> findAll(String authenticatedUserId) {
        return tasklistOutputGateway.findAllByOwnerId(authenticatedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Tasklist findById(String tasklistId, String authenticatedUserId) {
        Tasklist tasklist = getOwnedTasklist(tasklistId, authenticatedUserId);
        return tasklist;
    }

    @Override
    @Transactional
    public Tasklist update(String tasklistId, Tasklist tasklist, String authenticatedUserId) {
        Tasklist existingTasklist = getOwnedTasklist(tasklistId, authenticatedUserId);
        String normalizedName = Tasklist.normalizeName(tasklist.getName());

        if (tasklistOutputGateway.existsByOwnerIdAndNameAndIdNot(authenticatedUserId, normalizedName, tasklistId)) {
            throw new TasklistAlreadyExistsException();
        }

        existingTasklist.setName(normalizedName);
        return tasklistOutputGateway.update(existingTasklist);
    }

    @Override
    @Transactional
    public void delete(String tasklistId, String authenticatedUserId) {
        Tasklist existingTasklist = getOwnedTasklist(tasklistId, authenticatedUserId);
        tasklistOutputGateway.deleteById(existingTasklist.getId());
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
