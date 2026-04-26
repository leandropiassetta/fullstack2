package br.com.jtech.tasklist.application.ports.output;

import br.com.jtech.tasklist.application.core.domains.User;

public interface RegisterUserOutputGateway {
    User register(User user);
}
