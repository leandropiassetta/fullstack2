package br.com.jtech.tasklist.adapters.output;

import br.com.jtech.tasklist.adapters.output.repositories.RefreshTokenRepository;
import br.com.jtech.tasklist.adapters.output.repositories.UserRepository;
import br.com.jtech.tasklist.adapters.output.repositories.entities.RefreshTokenEntity;
import br.com.jtech.tasklist.application.ports.output.SaveRefreshTokenOutputGateway;
import br.com.jtech.tasklist.config.infra.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SaveRefreshTokenAdapter implements SaveRefreshTokenOutputGateway {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Override
    public void save(String tokenHash, String userId, LocalDateTime expiresAt, boolean revoked) {
        var user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(UserNotFoundException::new);

        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(expiresAt)
                .revoked(revoked)
                .build();

        refreshTokenRepository.save(entity);
    }

}
