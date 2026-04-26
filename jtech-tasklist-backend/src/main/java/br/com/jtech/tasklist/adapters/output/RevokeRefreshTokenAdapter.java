package br.com.jtech.tasklist.adapters.output;

import br.com.jtech.tasklist.adapters.output.repositories.RefreshTokenRepository;
import br.com.jtech.tasklist.application.ports.output.RevokeRefreshTokenOutputGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RevokeRefreshTokenAdapter implements RevokeRefreshTokenOutputGateway {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public void revokeByTokenHash(String tokenHash) {
        refreshTokenRepository.revokeByTokenHash(tokenHash);
    }

}
