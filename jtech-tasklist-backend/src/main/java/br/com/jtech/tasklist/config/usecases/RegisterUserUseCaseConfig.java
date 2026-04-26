package br.com.jtech.tasklist.config.usecases;

import br.com.jtech.tasklist.application.core.usecases.RegisterUserUseCase;
import br.com.jtech.tasklist.application.ports.output.FindUserByEmailOutputGateway;
import br.com.jtech.tasklist.application.ports.output.RegisterUserOutputGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class RegisterUserUseCaseConfig {

    @Bean
    public RegisterUserUseCase registerUserUseCase(FindUserByEmailOutputGateway findUserGateway,
                                                    RegisterUserOutputGateway registerGateway,
                                                    PasswordEncoder passwordEncoder) {
        return new RegisterUserUseCase(findUserGateway, registerGateway, passwordEncoder);
    }

}
