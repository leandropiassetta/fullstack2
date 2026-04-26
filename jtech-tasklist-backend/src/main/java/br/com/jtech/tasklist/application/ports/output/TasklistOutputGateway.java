package br.com.jtech.tasklist.application.ports.output;

import br.com.jtech.tasklist.application.core.domains.Tasklist;

import java.util.List;
import java.util.Optional;

public interface TasklistOutputGateway {

    Tasklist create(Tasklist tasklist);

    List<Tasklist> findAllByOwnerId(String ownerId);

    Optional<Tasklist> findById(String tasklistId);

    boolean existsByOwnerIdAndName(String ownerId, String name);

    boolean existsByOwnerIdAndNameAndIdNot(String ownerId, String name, String tasklistId);

    Tasklist update(Tasklist tasklist);

    boolean hasTasks(String tasklistId);

    void deleteById(String tasklistId);

}
