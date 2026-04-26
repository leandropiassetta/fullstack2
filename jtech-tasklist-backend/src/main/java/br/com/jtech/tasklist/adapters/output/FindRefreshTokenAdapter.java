package br.com.jtech.tasklist.adapters.output;

import br.com.jtech.tasklist.adapters.output.repositories.RefreshTokenRepository;
import br.com.jtech.tasklist.application.core.domains.RefreshToken;
import br.com.jtech.tasklist.application.ports.output.FindRefreshTokenOutputGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FindRefreshTokenAdapter implements FindRefreshTokenOutputGateway {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public Optional<RefreshToken> findActiveByTokenHash(String tokenHash) {
        return refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .map(RefreshToken::of);
    }

}
