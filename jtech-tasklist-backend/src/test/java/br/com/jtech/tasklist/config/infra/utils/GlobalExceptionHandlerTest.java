package br.com.jtech.tasklist.config.infra.utils;

import br.com.jtech.tasklist.config.infra.exceptions.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleUnexpectedShouldReturnInternalServerErrorWithApiErrorContract() {
        RuntimeException exception = new RuntimeException("boom");

        ResponseEntity<ApiError> response = handler.handleUnexpected(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("Unexpected error");
        assertThat(response.getBody().getDebugMessage()).isEqualTo("boom");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
}
