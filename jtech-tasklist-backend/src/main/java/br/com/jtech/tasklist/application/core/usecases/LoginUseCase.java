package br.com.jtech.tasklist.application.core.usecases;

import br.com.jtech.tasklist.application.core.domains.AuthTokens;
import br.com.jtech.tasklist.application.core.domains.User;
import br.com.jtech.tasklist.application.ports.input.LoginInputGateway;
import br.com.jtech.tasklist.application.ports.output.FindUserByEmailOutputGateway;
import br.com.jtech.tasklist.application.ports.output.SaveRefreshTokenOutputGateway;
import br.com.jtech.tasklist.config.infra.exceptions.UnauthorizedException;
import br.com.jtech.tasklist.config.infra.security.JwtService;
import br.com.jtech.tasklist.config.infra.utils.TokenHashUtil;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

public class LoginUseCase implements LoginInputGateway {

    private final FindUserByEmailOutputGateway findUserGateway;
    private final SaveRefreshTokenOutputGateway saveRefreshTokenGateway;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final long refreshExpiration;

    public LoginUseCase(FindUserByEmailOutputGateway findUserGateway,
                        SaveRefreshTokenOutputGateway saveRefreshTokenGateway,
                        JwtService jwtService,
                        PasswordEncoder passwordEncoder,
                        long refreshExpiration) {
        this.findUserGateway = findUserGateway;
        this.saveRefreshTokenGateway = saveRefreshTokenGateway;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.refreshExpiration = refreshExpiration;
    }

    @Override
    public AuthTokens login(String email, String rawPassword) {
        User user = findUserGateway.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(user.getId());
        String rawRefreshToken = UUID.randomUUID().toString();
        String tokenHash = TokenHashUtil.hash(rawRefreshToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshExpiration / 1000);

        saveRefreshTokenGateway.save(tokenHash, user.getId(), expiresAt, false);

        return AuthTokens.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

}
