package br.com.jtech.tasklist.config.infra.utils;

import br.com.jtech.tasklist.config.infra.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex) {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST);
        error.setMessage("Error on request");
        error.setTimestamp(LocalDateTime.now());
        error.setSubErrors(subErrors(ex));
        error.setDebugMessage(ex.getLocalizedMessage());
        return buildResponseEntity(error);
    }

    @ExceptionHandler({TasklistNotFoundException.class, TaskNotFoundException.class,
            UserNotFoundException.class, RefreshTokenNotFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex) {
        ApiError error = new ApiError(HttpStatus.NOT_FOUND);
        error.setMessage(ex.getMessage());
        error.setTimestamp(LocalDateTime.now());
        return buildResponseEntity(error);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleConflict(UserAlreadyExistsException ex) {
        ApiError error = new ApiError(HttpStatus.CONFLICT);
        error.setMessage(ex.getMessage());
        error.setTimestamp(LocalDateTime.now());
        return buildResponseEntity(error);
    }

    @ExceptionHandler({TasklistAlreadyExistsException.class, TaskAlreadyExistsException.class})
    public ResponseEntity<ApiError> handleTasklistConflict(RuntimeException ex) {
        ApiError error = new ApiError(HttpStatus.CONFLICT);
        error.setMessage(ex.getMessage());
        error.setTimestamp(LocalDateTime.now());
        return buildResponseEntity(error);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex) {
        ApiError error = new ApiError(HttpStatus.UNAUTHORIZED);
        error.setMessage(ex.getMessage());
        error.setTimestamp(LocalDateTime.now());
        return buildResponseEntity(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST);
        error.setMessage(ex.getMessage());
        error.setTimestamp(LocalDateTime.now());
        return buildResponseEntity(error);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex) {
        ApiError error = new ApiError(HttpStatus.FORBIDDEN);
        error.setMessage(ex.getMessage());
        error.setTimestamp(LocalDateTime.now());
        return buildResponseEntity(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        ApiError error = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR);
        error.setMessage("Unexpected error");
        error.setDebugMessage(ex.getLocalizedMessage());
        error.setTimestamp(LocalDateTime.now());
        return buildResponseEntity(error);
    }

    private ResponseEntity<ApiError> buildResponseEntity(ApiError apiError) {
        return new ResponseEntity<>(apiError, apiError.getStatus());
    }

    private List<ApiSubError> subErrors(MethodArgumentNotValidException ex) {
        List<ApiSubError> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            ApiValidationError api = new ApiValidationError(
                    ex.getObjectName(),
                    fieldError.getField(),
                    fieldError.getRejectedValue(),
                    fieldError.getDefaultMessage()
            );
            errors.add(api);
        }
        return errors;
    }

}
