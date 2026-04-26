package br.com.jtech.tasklist.config.usecases;

import br.com.jtech.tasklist.application.core.usecases.LoginUseCase;
import br.com.jtech.tasklist.application.ports.output.FindUserByEmailOutputGateway;
import br.com.jtech.tasklist.application.ports.output.SaveRefreshTokenOutputGateway;
import br.com.jtech.tasklist.config.infra.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class LoginUseCaseConfig {

    @Bean
    public LoginUseCase loginUseCase(FindUserByEmailOutputGateway findUserGateway,
                                      SaveRefreshTokenOutputGateway saveRefreshTokenGateway,
                                      JwtService jwtService,
                                      PasswordEncoder passwordEncoder,
                                      @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        return new LoginUseCase(findUserGateway, saveRefreshTokenGateway,
                jwtService, passwordEncoder, refreshExpiration);
    }

}
