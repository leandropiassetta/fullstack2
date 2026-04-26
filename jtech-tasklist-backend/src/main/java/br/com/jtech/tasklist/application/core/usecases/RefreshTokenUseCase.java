package br.com.jtech.tasklist.application.core.usecases;

import br.com.jtech.tasklist.application.core.domains.AuthTokens;
import br.com.jtech.tasklist.application.core.domains.RefreshToken;
import br.com.jtech.tasklist.application.ports.input.RefreshTokenInputGateway;
import br.com.jtech.tasklist.application.ports.output.FindRefreshTokenOutputGateway;
import br.com.jtech.tasklist.application.ports.output.RevokeRefreshTokenOutputGateway;
import br.com.jtech.tasklist.application.ports.output.SaveRefreshTokenOutputGateway;
import br.com.jtech.tasklist.config.infra.exceptions.UnauthorizedException;
import br.com.jtech.tasklist.config.infra.security.JwtService;
import br.com.jtech.tasklist.config.infra.utils.TokenHashUtil;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public class RefreshTokenUseCase implements RefreshTokenInputGateway {

    private final FindRefreshTokenOutputGateway findRefreshTokenGateway;
    private final RevokeRefreshTokenOutputGateway revokeRefreshTokenGateway;
    private final SaveRefreshTokenOutputGateway saveRefreshTokenGateway;
    private final JwtService jwtService;
    private final long refreshExpiration;

    public RefreshTokenUseCase(FindRefreshTokenOutputGateway findRefreshTokenGateway,
                               RevokeRefreshTokenOutputGateway revokeRefreshTokenGateway,
                               SaveRefreshTokenOutputGateway saveRefreshTokenGateway,
                               JwtService jwtService,
                               long refreshExpiration) {
        this.findRefreshTokenGateway = findRefreshTokenGateway;
        this.revokeRefreshTokenGateway = revokeRefreshTokenGateway;
        this.saveRefreshTokenGateway = saveRefreshTokenGateway;
        this.jwtService = jwtService;
        this.refreshExpiration = refreshExpiration;
    }

    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthTokens refresh(String rawRefreshToken) {
        String tokenHash = TokenHashUtil.hash(rawRefreshToken);

        RefreshToken token = findRefreshTokenGateway.findActiveByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            revokeRefreshTokenGateway.revokeByTokenHash(tokenHash);
            throw new UnauthorizedException("Refresh token expired");
        }

        revokeRefreshTokenGateway.revokeByTokenHash(tokenHash);

        String accessToken = jwtService.generateAccessToken(token.getUserId());
        String newRawRefreshToken = UUID.randomUUID().toString();
        String newTokenHash = TokenHashUtil.hash(newRawRefreshToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshExpiration / 1000);

        saveRefreshTokenGateway.save(newTokenHash, token.getUserId(), expiresAt, false);

        return AuthTokens.builder()
                .accessToken(accessToken)
                .refreshToken(newRawRefreshToken)
                .build();
    }

}
