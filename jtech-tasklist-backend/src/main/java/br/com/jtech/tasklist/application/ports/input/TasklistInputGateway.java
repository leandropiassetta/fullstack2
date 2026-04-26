package br.com.jtech.tasklist.application.ports.input;

import br.com.jtech.tasklist.application.core.domains.Tasklist;

import java.util.List;

public interface TasklistInputGateway {

    Tasklist create(Tasklist tasklist, String authenticatedUserId);

    List<Tasklist> findAll(String authenticatedUserId);

    Tasklist findById(String tasklistId, String authenticatedUserId);

    Tasklist update(String tasklistId, Tasklist tasklist, String authenticatedUserId);

    void delete(String tasklistId, String authenticatedUserId);

}
