package br.com.jtech.tasklist.config.infra.exceptions;

public class RefreshTokenNotFoundException extends RuntimeException {
    public RefreshTokenNotFoundException() {
        super("Refresh token not found or invalid");
    }
    public RefreshTokenNotFoundException(String message) {
        super(message);
    }
}
