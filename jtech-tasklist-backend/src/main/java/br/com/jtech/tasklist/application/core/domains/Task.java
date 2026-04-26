package br.com.jtech.tasklist.application.core.domains;

import br.com.jtech.tasklist.adapters.input.protocols.TaskRequest;
import br.com.jtech.tasklist.adapters.output.repositories.entities.TaskEntity;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    private String id;
    private String title;
    private String description;
    private Boolean completed;
    private String tasklistId;
    private String ownerId;
    private LocalDateTime createdAt;

    public static List<Task> of(List<TaskEntity> entities) {
        return entities.stream().map(Task::of).toList();
    }

    public static Task of(TaskEntity entity) {
        return Task.builder()
                .id(entity.getId().toString())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .completed(entity.getCompleted())
                .ownerId(entity.getOwner().getId().toString())
                .tasklistId(entity.getTasklist().getId().toString())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static Task of(TaskRequest request) {
        return Task.builder()
                .title(normalizeTitle(request.getTitle()))
                .description(normalizeDescription(request.getDescription()))
                .completed(request.getCompleted())
                .tasklistId(request.getTasklistId())
                .build();
    }

    public void markAsCompleted() {
        this.completed = true;
    }

    public void renameTitle(String newTitle) {
        if (newTitle == null || newTitle.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        this.title = newTitle;
    }

    public boolean belongsTo(String userId) {
        return this.ownerId != null && this.ownerId.equals(userId);
    }

    public static String normalizeTitle(String title) {
        if (title == null) {
            throw new IllegalArgumentException("Title must not be null");
        }

        String normalizedTitle = title.trim();
        if (normalizedTitle.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }

        return normalizedTitle;
    }

    public static String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }

        String normalizedDescription = description.trim();
        return normalizedDescription.isEmpty() ? null : normalizedDescription;
    }

    public static Boolean requireCompleted(Boolean completed) {
        if (completed == null) {
            throw new IllegalArgumentException("Completed must not be null");
        }

        return completed;
    }

}
