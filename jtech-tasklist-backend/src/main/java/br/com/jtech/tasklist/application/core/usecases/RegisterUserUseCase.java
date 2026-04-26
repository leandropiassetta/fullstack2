package br.com.jtech.tasklist.application.core.usecases;

import br.com.jtech.tasklist.application.core.domains.User;
import br.com.jtech.tasklist.application.ports.input.RegisterUserInputGateway;
import br.com.jtech.tasklist.application.ports.output.FindUserByEmailOutputGateway;
import br.com.jtech.tasklist.application.ports.output.RegisterUserOutputGateway;
import br.com.jtech.tasklist.config.infra.exceptions.UserAlreadyExistsException;
import org.springframework.security.crypto.password.PasswordEncoder;

public class RegisterUserUseCase implements RegisterUserInputGateway {

    private final FindUserByEmailOutputGateway findUserGateway;
    private final RegisterUserOutputGateway registerGateway;
    private final PasswordEncoder passwordEncoder;

    public RegisterUserUseCase(FindUserByEmailOutputGateway findUserGateway,
                                RegisterUserOutputGateway registerGateway,
                                PasswordEncoder passwordEncoder) {
        this.findUserGateway = findUserGateway;
        this.registerGateway = registerGateway;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void register(User user) {
        if (findUserGateway.findByEmail(user.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException(user.getEmail());
        }
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        registerGateway.register(user);
    }

}
