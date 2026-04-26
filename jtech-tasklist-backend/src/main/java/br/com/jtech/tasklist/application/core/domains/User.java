package br.com.jtech.tasklist.application.core.domains;

import br.com.jtech.tasklist.adapters.input.protocols.RegisterUserRequest;
import br.com.jtech.tasklist.adapters.output.repositories.entities.UserEntity;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private String id;
    private String name;
    private String email;
    private String passwordHash;

    public static User of(RegisterUserRequest request) {
        return User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(request.getPassword())
                .build();
    }

    public static User of(UserEntity entity) {
        return User.builder()
                .id(entity.getId().toString())
                .name(entity.getName())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .build();
    }

}
