package br.com.jtech.tasklist.adapters.input.controllers;

import br.com.jtech.tasklist.adapters.input.protocols.TaskRequest;
import br.com.jtech.tasklist.adapters.input.protocols.TaskResponse;
import br.com.jtech.tasklist.application.core.domains.Task;
import br.com.jtech.tasklist.application.ports.input.TaskInputGateway;
import br.com.jtech.tasklist.config.infra.utils.AuthenticatedUserHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskInputGateway taskInputGateway;

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        Task task = taskInputGateway.create(
                Task.of(request),
                AuthenticatedUserHelper.getAuthenticatedUserId()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TaskResponse.of(task));
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> findAll(@RequestParam(required = false) String tasklistId) {
        List<TaskResponse> tasks = taskInputGateway.findAll(
                        AuthenticatedUserHelper.getAuthenticatedUserId(),
                        tasklistId
                )
                .stream()
                .map(TaskResponse::of)
                .toList();

        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> findById(@PathVariable String id) {
        Task task = taskInputGateway.findById(id, AuthenticatedUserHelper.getAuthenticatedUserId());
        return ResponseEntity.ok(TaskResponse.of(task));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(@PathVariable String id,
                                               @Valid @RequestBody TaskRequest request) {
        Task task = taskInputGateway.update(
                id,
                Task.of(request),
                AuthenticatedUserHelper.getAuthenticatedUserId()
        );

        return ResponseEntity.ok(TaskResponse.of(task));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        taskInputGateway.delete(id, AuthenticatedUserHelper.getAuthenticatedUserId());
        return ResponseEntity.noContent().build();
    }

}
