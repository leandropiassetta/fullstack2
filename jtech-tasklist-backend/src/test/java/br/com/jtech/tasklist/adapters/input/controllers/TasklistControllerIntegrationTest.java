package br.com.jtech.tasklist.adapters.input.controllers;

import br.com.jtech.tasklist.adapters.output.repositories.TaskRepository;
import br.com.jtech.tasklist.adapters.output.repositories.TasklistRepository;
import br.com.jtech.tasklist.adapters.output.repositories.UserRepository;
import br.com.jtech.tasklist.adapters.output.repositories.entities.TaskEntity;
import br.com.jtech.tasklist.adapters.output.repositories.entities.TasklistEntity;
import br.com.jtech.tasklist.adapters.output.repositories.entities.UserEntity;
import br.com.jtech.tasklist.config.infra.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TasklistControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TasklistRepository tasklistRepository;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        tasklistRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createShouldReturnCreatedAndPersistTrimmedName() throws Exception {
        UserEntity user = saveUser("Alice", "alice@test.com");

        mockMvc.perform(post("/api/v1/tasklists")
                        .with(csrf())
                        .header("Authorization", bearer(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TasklistPayload("  Trabalho  "))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Trabalho"));
    }

    @Test
    void createShouldReturnConflictForCaseInsensitiveDuplicate() throws Exception {
        UserEntity user = saveUser("Alice", "alice@test.com");
        saveTasklist(user, "Trabalho");

        mockMvc.perform(post("/api/v1/tasklists")
                        .with(csrf())
                        .header("Authorization", bearer(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TasklistPayload(" trabalho "))))
                .andExpect(status().isConflict());
    }

    @Test
    void findAllShouldReturnOnlyAuthenticatedUsersTasklists() throws Exception {
        UserEntity user = saveUser("Alice", "alice@test.com");
        UserEntity otherUser = saveUser("Bob", "bob@test.com");
        saveTasklist(user, "Trabalho");
        saveTasklist(otherUser, "Pessoal");

        mockMvc.perform(get("/api/v1/tasklists")
                        .header("Authorization", bearer(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Trabalho"));
    }

    @Test
    void findByIdShouldReturnForbiddenWhenTasklistBelongsToAnotherUser() throws Exception {
        UserEntity user = saveUser("Alice", "alice@test.com");
        UserEntity otherUser = saveUser("Bob", "bob@test.com");
        TasklistEntity otherTasklist = saveTasklist(otherUser, "Pessoal");

        mockMvc.perform(get("/api/v1/tasklists/{id}", otherTasklist.getId())
                        .header("Authorization", bearer(user.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateShouldReturnConflictWhenAnotherTasklistUsesSameNormalizedName() throws Exception {
        UserEntity user = saveUser("Alice", "alice@test.com");
        TasklistEntity first = saveTasklist(user, "Trabalho");
        saveTasklist(user, "Estudos");

        mockMvc.perform(put("/api/v1/tasklists/{id}", first.getId())
                        .with(csrf())
                        .header("Authorization", bearer(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TasklistPayload(" estudos "))))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteShouldReturnNoContentAndCascadeDeleteTasksWhenTasklistHasTasks() throws Exception {
        UserEntity user = saveUser("Alice", "alice@test.com");
        TasklistEntity tasklist = saveTasklist(user, "Trabalho");
        TaskEntity task = saveTask(user, tasklist, "Tarefa 1");

        mockMvc.perform(delete("/api/v1/tasklists/{id}", tasklist.getId())
                        .with(csrf())
                        .header("Authorization", bearer(user.getId())))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(taskRepository.findById(task.getId())).isEmpty();
        org.assertj.core.api.Assertions.assertThat(tasklistRepository.findById(tasklist.getId())).isEmpty();
    }

    @Test
    void deleteShouldReturnNoContentWhenTasklistHasNoTasks() throws Exception {
        UserEntity user = saveUser("Alice", "alice@test.com");
        TasklistEntity tasklist = saveTasklist(user, "Trabalho");

        mockMvc.perform(delete("/api/v1/tasklists/{id}", tasklist.getId())
                        .with(csrf())
                        .header("Authorization", bearer(user.getId())))
                .andExpect(status().isNoContent());
    }

    @Test
    void endpointsShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/tasklists"))
                .andExpect(status().isUnauthorized());
    }

    private UserEntity saveUser(String name, String email) {
        return userRepository.save(UserEntity.builder()
                .name(name)
                .email(email)
                .passwordHash("hash")
                .build());
    }

    private TasklistEntity saveTasklist(UserEntity user, String name) {
        return tasklistRepository.save(TasklistEntity.builder()
                .name(name)
                .owner(user)
                .build());
    }

    private TaskEntity saveTask(UserEntity user, TasklistEntity tasklist, String title) {
        return taskRepository.save(TaskEntity.builder()
                .title(title)
                .completed(false)
                .owner(user)
                .tasklist(tasklist)
                .build());
    }

    private String bearer(UUID userId) {
        return "Bearer " + jwtService.generateAccessToken(userId.toString());
    }

    private record TasklistPayload(String name) {
    }

}
