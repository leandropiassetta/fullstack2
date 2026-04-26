package br.com.jtech.tasklist.application.ports.output;

import br.com.jtech.tasklist.application.core.domains.User;

import java.util.Optional;

public interface FindUserByEmailOutputGateway {
    Optional<User> findByEmail(String email);
}
