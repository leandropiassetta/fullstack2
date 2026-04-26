package br.com.jtech.tasklist.config.infra.exceptions;

public class TasklistNotFoundException extends RuntimeException {
    public TasklistNotFoundException() {
        super("Tasklist not found");
    }
    public TasklistNotFoundException(String message) {
        super(message);
    }
}
