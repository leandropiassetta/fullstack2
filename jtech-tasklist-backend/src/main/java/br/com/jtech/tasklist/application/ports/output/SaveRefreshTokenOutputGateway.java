package br.com.jtech.tasklist.application.ports.output;

import java.time.LocalDateTime;

public interface SaveRefreshTokenOutputGateway {
    void save(String tokenHash, String userId, LocalDateTime expiresAt, boolean revoked);
}
