package br.com.jtech.tasklist.application.ports.output;

import br.com.jtech.tasklist.application.core.domains.RefreshToken;

import java.util.Optional;

public interface FindRefreshTokenOutputGateway {
    Optional<RefreshToken> findActiveByTokenHash(String tokenHash);
}
