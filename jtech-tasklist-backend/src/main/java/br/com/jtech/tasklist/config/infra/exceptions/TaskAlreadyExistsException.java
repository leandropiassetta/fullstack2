package br.com.jtech.tasklist.config.infra.exceptions;

public class TaskAlreadyExistsException extends RuntimeException {
    public TaskAlreadyExistsException() {
        super("Task title already in use for this tasklist");
    }

    public TaskAlreadyExistsException(String message) {
        super(message);
    }
}
