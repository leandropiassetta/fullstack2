package br.com.jtech.tasklist.config.infra.exceptions;

public class TasklistAlreadyExistsException extends RuntimeException {
    public TasklistAlreadyExistsException() {
        super("Tasklist name already in use");
    }

    public TasklistAlreadyExistsException(String message) {
        super(message);
    }
}
