package br.com.jtech.tasklist.application.ports.input;

import br.com.jtech.tasklist.application.core.domains.AuthTokens;

public interface RefreshTokenInputGateway {
    AuthTokens refresh(String rawRefreshToken);
}
