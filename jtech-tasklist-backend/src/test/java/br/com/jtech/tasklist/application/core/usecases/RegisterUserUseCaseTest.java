package br.com.jtech.tasklist.application.core.usecases;

import br.com.jtech.tasklist.application.core.domains.User;
import br.com.jtech.tasklist.application.ports.output.FindUserByEmailOutputGateway;
import br.com.jtech.tasklist.application.ports.output.RegisterUserOutputGateway;
import br.com.jtech.tasklist.config.infra.exceptions.UserAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    private FindUserByEmailOutputGateway findUserGateway;

    @Mock
    private RegisterUserOutputGateway registerGateway;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegisterUserUseCase registerUserUseCase;

    @Test
    void registerShouldEncodePasswordBeforePersisting() {
        User user = User.builder()
                .name("Alice")
                .email("alice@test.com")
                .passwordHash("plain-password")
                .build();

        when(findUserGateway.findByEmail("alice@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");

        registerUserUseCase.register(user);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(registerGateway).register(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded-password");
    }

    @Test
    void registerShouldThrowConflictWhenEmailAlreadyExists() {
        User existingUser = User.builder().email("alice@test.com").build();
        User user = User.builder()
                .name("Alice")
                .email("alice@test.com")
                .passwordHash("plain-password")
                .build();

        when(findUserGateway.findByEmail("alice@test.com")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> registerUserUseCase.register(user))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("alice@test.com");

        verify(passwordEncoder, never()).encode(anyString());
        verify(registerGateway, never()).register(any());
    }

    @Test
    void registerShouldPreserveNameAndEmailWhenPersisting() {
        User user = User.builder()
                .name("Alice")
                .email("alice@test.com")
                .passwordHash("plain-password")
                .build();

        when(findUserGateway.findByEmail("alice@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");

        registerUserUseCase.register(user);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(registerGateway).register(userCaptor.capture());
        assertThat(userCaptor.getValue().getName()).isEqualTo("Alice");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    void registerShouldNeverPersistRawPassword() {
        User user = User.builder()
                .name("Alice")
                .email("alice@test.com")
                .passwordHash("plain-password")
                .build();

        when(findUserGateway.findByEmail("alice@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");

        registerUserUseCase.register(user);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(registerGateway).register(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo("plain-password");
    }

    @Test
    void registerShouldCheckEmailExistenceBeforeEncodingPassword() {
        User user = User.builder()
                .name("Alice")
                .email("alice@test.com")
                .passwordHash("plain-password")
                .build();

        when(findUserGateway.findByEmail("alice@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");

        registerUserUseCase.register(user);

        InOrder inOrder = inOrder(findUserGateway, passwordEncoder, registerGateway);
        inOrder.verify(findUserGateway).findByEmail("alice@test.com");
        inOrder.verify(passwordEncoder).encode("plain-password");
        inOrder.verify(registerGateway).register(any(User.class));
    }

}
