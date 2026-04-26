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
class TaskControllerIntegrationTest {

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
    void createShouldReturnCreatedAndPersistTrimmedTitle() throws Exception {
        UserEntity user = saveUser("Alice", "alice@test.com");
        TasklistEntity tasklist = saveTasklist(user, "Trabalho");

        mockMvc.perform(post("/api/v1/tasks")
                        .with(csrf())
                        .header("Authorization", bearer(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskPayload("  Comprar leite  ", " mercado ", null, tasklist.getId().toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Comprar leite"))
                .andExpect(jsonPath("$.description").value("mercado"))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    void createShouldReturnConflictForNormalizedDuplicateInSameTasklist() throws Exception {
        UserEntity user = saveUser("Alice", "alice@test.com");
        TasklistEntity tasklist = saveTasklist(user, "Trabalho");
        saveTask(user, tasklist, "Comprar leite", false);

        mockMvc.perform(post("/api/v1/tasks")
                        .with(csrf())
                        .header("Authorization", bearer(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskPayload(" comprar leite ", null, null, tasklist.getId().toString()))))
                .andExpect(status().isConflict());
    }

    @Test
    void listShouldFilterByTasklistWhenRequested() throws Exception {
        UserEntity user = saveUser("Alice", "alice@test.com");
        TasklistEntity work = saveTasklist(user, "Trabalho");
        TasklistEntity personal = saveTasklist(user, "Pessoal");
        saveTask(user, work, "Task A", false);
        saveTask(user, personal, "Task B", false);

        mockMvc.perform(get("/api/v1/tasks")
                        .param("tasklistId", work.getId().toString())
                        .header("Authorization", bearer(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Task A"));
    }

    @Test
    void listShouldReturnForbiddenWhenFilteringByAnotherUsersTasklist() throws Exception {
        UserEntity user = saveUser("Alice", "alice@test.com");
        UserEntity otherUser = saveUser("Bob", "bob@test.com");
        TasklistEntity others = saveTasklist(otherUser, "Pessoal");

        mockMvc.perform(get("/api/v1/tasks")
                        .param("tasklistId", others.getId().toString())
                        .header("Authorization", bearer(user.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    void findByIdShouldReturnForbiddenWhenTaskBelongsToAnotherUser() throws Exception {
        UserEntity user = saveUser("Alice", "alice@test.com");
        UserEntity otherUser = saveUser("Bob", "bob@test.com");
        TaskEntity otherTask = saveTask(otherUser, saveTasklist(otherUser, "Pessoal"), "Task B", false);

        mockMvc.perform(get("/api/v1/tasks/{id}", otherTask.getId())
                        .header("Authorization", bearer(user.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void findByIdShouldReturnNotFoundWhenTaskDoesNotExist() throws Exception {
        UserEntity user = saveUser("Alice", "alice@test.com");

        mockMvc.perform(get("/api/v1/tasks/{id}", UUID.randomUUID())
                        .header("Authorization", bearer(user.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateShouldReturnConflictWhenAnotherTaskUsesSameNormalizedTitleInSameTasklist() throws Exception {
        UserEntity user = saveUser("Alice", "alice@test.com");
        TasklistEntity tasklist = saveTasklist(user, "Trabalho");
        TaskEntity first = saveTask(user, tasklist, "Task A", false);
        saveTask(user, tasklist, "Task B", false);

        mockMvc.perform(put("/api/v1/tasks/{id}", first.getId())
                        .with(csrf())
                        .header("Authorization", bearer(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskPayload(" task b ", "desc", true, tasklist.getId().toString()))))
                .andExpect(status().isConflict());
    }

    @Test
    void updateShouldReturnForbiddenWhenMovingTaskToAnotherUsersTasklist() throws Exception {
        UserEntity user = saveUser("Alice", "alice@test.com");
        UserEntity otherUser = saveUser("Bob", "bob@test.com");
        TasklistEntity ownTasklist = saveTasklist(user, "Trabalho");
        TasklistEntity othersTasklist = saveTasklist(otherUser, "Pessoal");
        TaskEntity task = saveTask(user, ownTasklist, "Task A", false);

        mockMvc.perform(put("/api/v1/tasks/{id}", task.getId())
                        .with(csrf())
                        .header("Authorization", bearer(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskPayload("Task A", "desc", true, othersTasklist.getId().toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createShouldReturnNotFoundWhenTasklistDoesNotExist() throws Exception {
        UserEntity user = saveUser("Alice", "alice@test.com");

        mockMvc.perform(post("/api/v1/tasks")
                        .with(csrf())
                        .header("Authorization", bearer(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskPayload("Task A", null, null, UUID.randomUUID().toString()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateShouldReturnBadRequestWhenCompletedIsMissing() throws Exception {
        UserEntity user = saveUser("Alice", "alice@test.com");
        TasklistEntity tasklist = saveTasklist(user, "Trabalho");
        TaskEntity task = saveTask(user, tasklist, "Task A", false);

        mockMvc.perform(put("/api/v1/tasks/{id}", task.getId())
                        .with(csrf())
                        .header("Authorization", bearer(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskPayload("Task A", "desc", null, tasklist.getId().toString()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteShouldRemoveOwnedTask() throws Exception {
        UserEntity user = saveUser("Alice", "alice@test.com");
        TaskEntity task = saveTask(user, saveTasklist(user, "Trabalho"), "Task A", false);

        mockMvc.perform(delete("/api/v1/tasks/{id}", task.getId())
                        .with(csrf())
                        .header("Authorization", bearer(user.getId())))
                .andExpect(status().isNoContent());
    }

    @Test
    void endpointsShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isString());
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

    private TaskEntity saveTask(UserEntity user, TasklistEntity tasklist, String title, boolean completed) {
        return taskRepository.save(TaskEntity.builder()
                .title(title)
                .description(null)
                .completed(completed)
                .owner(user)
                .tasklist(tasklist)
                .build());
    }

    private String bearer(UUID userId) {
        return "Bearer " + jwtService.generateAccessToken(userId.toString());
    }

    private record TaskPayload(String title, String description, Boolean completed, String tasklistId) {
    }

}
