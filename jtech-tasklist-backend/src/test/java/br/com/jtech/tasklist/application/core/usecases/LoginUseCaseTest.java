package br.com.jtech.tasklist.application.core.usecases;

import br.com.jtech.tasklist.application.core.domains.AuthTokens;
import br.com.jtech.tasklist.application.core.domains.User;
import br.com.jtech.tasklist.application.ports.output.FindUserByEmailOutputGateway;
import br.com.jtech.tasklist.application.ports.output.SaveRefreshTokenOutputGateway;
import br.com.jtech.tasklist.config.infra.exceptions.UnauthorizedException;
import br.com.jtech.tasklist.config.infra.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private FindUserByEmailOutputGateway findUserGateway;

    @Mock
    private SaveRefreshTokenOutputGateway saveRefreshTokenGateway;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private LoginUseCase loginUseCase;

    @BeforeEach
    void setUp() {
        loginUseCase = new LoginUseCase(
                findUserGateway,
                saveRefreshTokenGateway,
                jwtService,
                passwordEncoder,
                604800000L
        );
    }

    @Test
    void loginShouldReturnAccessAndRefreshTokens() {
        User user = User.builder()
                .id("user-id")
                .email("alice@test.com")
                .passwordHash("encoded-password")
                .build();

        when(findUserGateway.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain-password", "encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken("user-id")).thenReturn("access-token");

        AuthTokens result = loginUseCase.login("alice@test.com", "plain-password");

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isNotBlank();

        ArgumentCaptor<String> tokenHashCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> expiresAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(saveRefreshTokenGateway).save(tokenHashCaptor.capture(), eq("user-id"), expiresAtCaptor.capture(), eq(false));
        assertThat(tokenHashCaptor.getValue()).isNotBlank();
        assertThat(expiresAtCaptor.getValue()).isAfter(LocalDateTime.now());
    }

    @Test
    void loginShouldReturnUnauthorizedWhenEmailDoesNotExist() {
        when(findUserGateway.findByEmail("alice@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginUseCase.login("alice@test.com", "plain-password"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");

        verify(saveRefreshTokenGateway, never()).save(anyString(), anyString(), any(), anyBoolean());
    }

    @Test
    void loginShouldReturnUnauthorizedWhenPasswordDoesNotMatch() {
        User user = User.builder()
                .id("user-id")
                .email("alice@test.com")
                .passwordHash("encoded-password")
                .build();

        when(findUserGateway.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> loginUseCase.login("alice@test.com", "wrong-password"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");

        verify(saveRefreshTokenGateway, never()).save(anyString(), anyString(), any(), anyBoolean());
    }

}
