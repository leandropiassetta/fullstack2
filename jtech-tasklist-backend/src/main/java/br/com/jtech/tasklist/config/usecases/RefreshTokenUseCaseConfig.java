package br.com.jtech.tasklist.config.usecases;

import br.com.jtech.tasklist.application.core.usecases.RefreshTokenUseCase;
import br.com.jtech.tasklist.application.ports.output.FindRefreshTokenOutputGateway;
import br.com.jtech.tasklist.application.ports.output.RevokeRefreshTokenOutputGateway;
import br.com.jtech.tasklist.application.ports.output.SaveRefreshTokenOutputGateway;
import br.com.jtech.tasklist.config.infra.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RefreshTokenUseCaseConfig {

    @Bean
    public RefreshTokenUseCase refreshTokenUseCase(FindRefreshTokenOutputGateway findRefreshTokenGateway,
                                                   RevokeRefreshTokenOutputGateway revokeRefreshTokenGateway,
                                                   SaveRefreshTokenOutputGateway saveRefreshTokenGateway,
                                                   JwtService jwtService,
                                                   @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        return new RefreshTokenUseCase(findRefreshTokenGateway, revokeRefreshTokenGateway,
                saveRefreshTokenGateway, jwtService, refreshExpiration);
    }

}
