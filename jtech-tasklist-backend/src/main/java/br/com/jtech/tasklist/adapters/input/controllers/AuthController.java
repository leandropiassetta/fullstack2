package br.com.jtech.tasklist.adapters.input.controllers;

import br.com.jtech.tasklist.adapters.input.protocols.LoginRequest;
import br.com.jtech.tasklist.adapters.input.protocols.LoginResponse;
import br.com.jtech.tasklist.adapters.input.protocols.RefreshTokenRequest;
import br.com.jtech.tasklist.adapters.input.protocols.RefreshTokenResponse;
import br.com.jtech.tasklist.adapters.input.protocols.RegisterUserRequest;
import br.com.jtech.tasklist.application.core.domains.AuthTokens;
import br.com.jtech.tasklist.application.ports.input.LoginInputGateway;
import br.com.jtech.tasklist.application.ports.input.RefreshTokenInputGateway;
import br.com.jtech.tasklist.application.ports.input.RegisterUserInputGateway;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static br.com.jtech.tasklist.application.core.domains.User.of;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserInputGateway registerUserInputGateway;
    private final LoginInputGateway loginInputGateway;
    private final RefreshTokenInputGateway refreshTokenInputGateway;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterUserRequest request) {
        registerUserInputGateway.register(of(request));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthTokens authTokens = loginInputGateway.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(LoginResponse.from(authTokens));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthTokens authTokens = refreshTokenInputGateway.refresh(request.getRefreshToken());
        return ResponseEntity.ok(RefreshTokenResponse.from(authTokens));
    }

}
