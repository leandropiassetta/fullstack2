package br.com.jtech.tasklist.application.core.domains;

import br.com.jtech.tasklist.adapters.output.repositories.entities.RefreshTokenEntity;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    private String id;
    private String tokenHash;
    private String userId;
    private LocalDateTime expiresAt;
    private Boolean revoked;

    public static RefreshToken of(RefreshTokenEntity entity) {
        return RefreshToken.builder()
                .id(entity.getId().toString())
                .tokenHash(entity.getTokenHash())
                .userId(entity.getUser().getId().toString())
                .expiresAt(entity.getExpiresAt())
                .revoked(entity.getRevoked())
                .build();
    }

}
