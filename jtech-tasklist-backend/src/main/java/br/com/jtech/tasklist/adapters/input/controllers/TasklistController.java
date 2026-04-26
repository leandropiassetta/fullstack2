package br.com.jtech.tasklist.adapters.input.controllers;

import br.com.jtech.tasklist.adapters.input.protocols.TasklistRequest;
import br.com.jtech.tasklist.adapters.input.protocols.TasklistResponse;
import br.com.jtech.tasklist.application.core.domains.Tasklist;
import br.com.jtech.tasklist.application.ports.input.TasklistInputGateway;
import br.com.jtech.tasklist.config.infra.utils.AuthenticatedUserHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasklists")
@RequiredArgsConstructor
public class TasklistController {

    private final TasklistInputGateway tasklistInputGateway;

    @PostMapping
    public ResponseEntity<TasklistResponse> create(@Valid @RequestBody TasklistRequest request) {
        Tasklist tasklist = tasklistInputGateway.create(
                Tasklist.of(request),
                AuthenticatedUserHelper.getAuthenticatedUserId()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TasklistResponse.of(tasklist));
    }

    @GetMapping
    public ResponseEntity<List<TasklistResponse>> findAll() {
        List<TasklistResponse> tasklists = tasklistInputGateway.findAll(AuthenticatedUserHelper.getAuthenticatedUserId())
                .stream()
                .map(TasklistResponse::of)
                .toList();

        return ResponseEntity.ok(tasklists);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TasklistResponse> findById(@PathVariable String id) {
        Tasklist tasklist = tasklistInputGateway.findById(id, AuthenticatedUserHelper.getAuthenticatedUserId());
        return ResponseEntity.ok(TasklistResponse.of(tasklist));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TasklistResponse> update(@PathVariable String id,
                                                   @Valid @RequestBody TasklistRequest request) {
        Tasklist tasklist = tasklistInputGateway.update(
                id,
                Tasklist.of(request),
                AuthenticatedUserHelper.getAuthenticatedUserId()
        );

        return ResponseEntity.ok(TasklistResponse.of(tasklist));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        tasklistInputGateway.delete(id, AuthenticatedUserHelper.getAuthenticatedUserId());
        return ResponseEntity.noContent().build();
    }

}
