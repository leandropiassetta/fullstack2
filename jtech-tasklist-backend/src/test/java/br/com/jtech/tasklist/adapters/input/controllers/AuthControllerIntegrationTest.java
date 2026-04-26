package br.com.jtech.tasklist.adapters.input.controllers;

import br.com.jtech.tasklist.adapters.output.repositories.RefreshTokenRepository;
import br.com.jtech.tasklist.adapters.output.repositories.UserRepository;
import br.com.jtech.tasklist.adapters.output.repositories.entities.RefreshTokenEntity;
import br.com.jtech.tasklist.adapters.output.repositories.entities.UserEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerShouldCreateUserWithEncodedPassword() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterPayload("Alice", "alice@test.com", "password123"))))
                .andExpect(status().isCreated());

        UserEntity user = userRepository.findByEmail("alice@test.com").orElseThrow();
        assertThat(user.getName()).isEqualTo("Alice");
        assertThat(passwordEncoder.matches("password123", user.getPasswordHash())).isTrue();
    }

    @Test
    void registerShouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        userRepository.save(UserEntity.builder()
                .name("Alice")
                .email("alice@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterPayload("Alice", "alice@test.com", "password123"))))
                .andExpect(status().isConflict());
    }

    @Test
    void registerShouldReturnBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterPayload("", "invalid-email", "123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginShouldReturnAccessAndRefreshTokensAndPersistRefreshToken() throws Exception {
        userRepository.save(UserEntity.builder()
                .name("Alice")
                .email("alice@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("alice@test.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString());

        List<RefreshTokenEntity> refreshTokens = refreshTokenRepository.findAll();
        assertThat(refreshTokens).hasSize(1);
        assertThat(refreshTokens.get(0).getRevoked()).isFalse();
    }

    @Test
    void loginShouldReturnUnauthorizedForInvalidCredentials() throws Exception {
        userRepository.save(UserEntity.builder()
                .name("Alice")
                .email("alice@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("alice@test.com", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginShouldReturnUnauthorizedForNonExistingEmail() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("missing@test.com", "password123"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginShouldReturnBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshShouldRotateRefreshToken() throws Exception {
        userRepository.save(UserEntity.builder()
                .name("Alice")
                .email("alice@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build());

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("alice@test.com", "password123"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String oldRefreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();

        String refreshResponse = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshPayload(oldRefreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String newRefreshToken = objectMapper.readTree(refreshResponse).get("refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);

        List<RefreshTokenEntity> refreshTokens = refreshTokenRepository.findAll();
        assertThat(refreshTokens).hasSize(2);
        assertThat(refreshTokens.stream().filter(RefreshTokenEntity::getRevoked)).hasSize(1);
        assertThat(refreshTokens.stream().filter(token -> !token.getRevoked())).hasSize(1);
    }

    @Test
    void refreshShouldRejectReusedRevokedToken() throws Exception {
        userRepository.save(UserEntity.builder()
                .name("Alice")
                .email("alice@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build());

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("alice@test.com", "password123"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String oldRefreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshPayload(oldRefreshToken))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshPayload(oldRefreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshShouldReturnUnauthorizedForUnknownToken() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshPayload("unknown-token"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshShouldReturnBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshPayload(""))))
                .andExpect(status().isBadRequest());
    }

    private record RegisterPayload(String name, String email, String password) {
    }

    private record LoginPayload(String email, String password) {
    }

    private record RefreshPayload(String refreshToken) {
    }

}
