package br.com.jtech.tasklist.application.ports.output;

public interface RevokeRefreshTokenOutputGateway {
    void revokeByTokenHash(String tokenHash);
}
