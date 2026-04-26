package br.com.jtech.tasklist.adapters.output;

import br.com.jtech.tasklist.adapters.output.repositories.UserRepository;
import br.com.jtech.tasklist.adapters.output.repositories.entities.UserEntity;
import br.com.jtech.tasklist.application.core.domains.User;
import br.com.jtech.tasklist.application.ports.output.RegisterUserOutputGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegisterUserAdapter implements RegisterUserOutputGateway {

    private final UserRepository userRepository;

    @Override
    public User register(User user) {
        UserEntity entity = UserEntity.builder()
                .name(user.getName())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .build();
        return User.of(userRepository.save(entity));
    }

}
