package br.com.jtech.tasklist.application.core.usecases;

import br.com.jtech.tasklist.application.core.domains.AuthTokens;
import br.com.jtech.tasklist.application.core.domains.RefreshToken;
import br.com.jtech.tasklist.application.ports.output.FindRefreshTokenOutputGateway;
import br.com.jtech.tasklist.application.ports.output.RevokeRefreshTokenOutputGateway;
import br.com.jtech.tasklist.application.ports.output.SaveRefreshTokenOutputGateway;
import br.com.jtech.tasklist.config.infra.exceptions.UnauthorizedException;
import br.com.jtech.tasklist.config.infra.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

    @Mock
    private FindRefreshTokenOutputGateway findRefreshTokenGateway;

    @Mock
    private RevokeRefreshTokenOutputGateway revokeRefreshTokenGateway;

    @Mock
    private SaveRefreshTokenOutputGateway saveRefreshTokenGateway;

    @Mock
    private JwtService jwtService;

    private RefreshTokenUseCase refreshTokenUseCase;

    @BeforeEach
    void setUp() {
        refreshTokenUseCase = new RefreshTokenUseCase(
                findRefreshTokenGateway,
                revokeRefreshTokenGateway,
                saveRefreshTokenGateway,
                jwtService,
                604800000L
        );
    }

    @Test
    void refreshShouldRotateTokensSuccessfully() {
        RefreshToken storedToken = RefreshToken.builder()
                .id("token-id")
                .userId("user-id")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        when(findRefreshTokenGateway.findActiveByTokenHash(anyString())).thenReturn(Optional.of(storedToken));
        when(jwtService.generateAccessToken("user-id")).thenReturn("new-access-token");

        AuthTokens result = refreshTokenUseCase.refresh("raw-refresh-token");

        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.getRefreshToken()).isNotBlank();
        verify(revokeRefreshTokenGateway).revokeByTokenHash(anyString());

        ArgumentCaptor<String> tokenHashCaptor = ArgumentCaptor.forClass(String.class);
        verify(saveRefreshTokenGateway).save(tokenHashCaptor.capture(), eq("user-id"), any(LocalDateTime.class), eq(false));
        assertThat(tokenHashCaptor.getValue()).isNotBlank();
    }

    @Test
    void refreshShouldThrowNotFoundWhenTokenDoesNotExist() {
        when(findRefreshTokenGateway.findActiveByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenUseCase.refresh("raw-refresh-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid refresh token");

        verify(revokeRefreshTokenGateway, never()).revokeByTokenHash(anyString());
        verify(saveRefreshTokenGateway, never()).save(anyString(), anyString(), any(), anyBoolean());
    }

    @Test
    void refreshShouldRevokeAndThrowUnauthorizedWhenTokenIsExpired() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id("token-id")
                .userId("user-id")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .revoked(false)
                .build();

        when(findRefreshTokenGateway.findActiveByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenUseCase.refresh("raw-refresh-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token expired");

        verify(revokeRefreshTokenGateway).revokeByTokenHash(anyString());
        verify(saveRefreshTokenGateway, never()).save(anyString(), anyString(), any(), anyBoolean());
    }

}
