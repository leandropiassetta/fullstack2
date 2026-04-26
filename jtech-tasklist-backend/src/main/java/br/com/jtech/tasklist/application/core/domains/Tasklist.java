package br.com.jtech.tasklist.application.core.domains;

import br.com.jtech.tasklist.adapters.input.protocols.TasklistRequest;
import br.com.jtech.tasklist.adapters.output.repositories.entities.TasklistEntity;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Tasklist {

    private String id;
    private String name;
    private String ownerId;

    public static List<Tasklist> of(List<TasklistEntity> entities) {
        return entities.stream().map(Tasklist::of).toList();
    }

    public static Tasklist of(TasklistEntity entity) {
        return Tasklist.builder()
                .id(entity.getId().toString())
                .name(entity.getName())
                .ownerId(entity.getOwner().getId().toString())
                .build();
    }

    public static Tasklist of(TasklistRequest request) {
        String normalizedName = normalizeName(request.getName());
        return Tasklist.builder()
                .name(normalizedName)
                .build();
    }

    public boolean belongsTo(String userId) {
        return this.ownerId != null && this.ownerId.equals(userId);
    }

    public static String normalizeName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Name must not be null");
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }

        return normalizedValue;
    }

}
