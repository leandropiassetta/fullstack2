package br.com.jtech.tasklist.config.infra.exceptions;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException() {
        super("Email already in use");
    }
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
