package br.com.jtech.tasklist.application.core.usecases;

import br.com.jtech.tasklist.application.core.domains.Task;
import br.com.jtech.tasklist.application.core.domains.Tasklist;
import br.com.jtech.tasklist.application.ports.output.TaskOutputGateway;
import br.com.jtech.tasklist.application.ports.output.TasklistOutputGateway;
import br.com.jtech.tasklist.config.infra.exceptions.ForbiddenException;
import br.com.jtech.tasklist.config.infra.exceptions.TaskAlreadyExistsException;
import br.com.jtech.tasklist.config.infra.exceptions.TaskNotFoundException;
import br.com.jtech.tasklist.config.infra.exceptions.TasklistNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskUseCaseTest {

    @Mock
    private TaskOutputGateway taskOutputGateway;

    @Mock
    private TasklistOutputGateway tasklistOutputGateway;

    @InjectMocks
    private TaskUseCase taskUseCase;

    @Test
    void createShouldTrimTitleAndAssociateAuthenticatedUser() {
        when(tasklistOutputGateway.findById("tasklist-id"))
                .thenReturn(Optional.of(Tasklist.builder()
                        .id("tasklist-id")
                        .name("Trabalho")
                        .ownerId("user-id")
                        .build()));
        when(taskOutputGateway.existsByTasklistIdAndTitle("tasklist-id", "Comprar leite")).thenReturn(false);
        when(taskOutputGateway.create(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId("task-id");
            return task;
        });

        Task result = taskUseCase.create(Task.builder()
                .title("  Comprar leite  ")
                .description("  mercado  ")
                .tasklistId("tasklist-id")
                .build(), "user-id");

        assertThat(result.getId()).isEqualTo("task-id");
        verify(taskOutputGateway).create(argThat(task ->
                task.getOwnerId().equals("user-id")
                        && task.getTasklistId().equals("tasklist-id")
                        && task.getTitle().equals("Comprar leite")
                        && task.getDescription().equals("mercado")
                        && task.getCompleted().equals(false)
        ));
    }

    @Test
    void createShouldThrowConflictWhenNormalizedTitleAlreadyExistsInTasklist() {
        when(tasklistOutputGateway.findById("tasklist-id"))
                .thenReturn(Optional.of(Tasklist.builder()
                        .id("tasklist-id")
                        .ownerId("user-id")
                        .build()));
        when(taskOutputGateway.existsByTasklistIdAndTitle("tasklist-id", "Comprar leite")).thenReturn(true);

        assertThatThrownBy(() -> taskUseCase.create(Task.builder()
                        .title("Comprar leite")
                        .tasklistId("tasklist-id")
                        .build(), "user-id"))
                .isInstanceOf(TaskAlreadyExistsException.class);
    }

    @Test
    void createShouldThrowForbiddenWhenTasklistBelongsToAnotherUser() {
        when(tasklistOutputGateway.findById("tasklist-id"))
                .thenReturn(Optional.of(Tasklist.builder()
                        .id("tasklist-id")
                        .ownerId("other-user")
                        .build()));

        assertThatThrownBy(() -> taskUseCase.create(Task.builder()
                        .title("Comprar leite")
                        .tasklistId("tasklist-id")
                .build(), "user-id"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createShouldThrowNotFoundWhenTasklistDoesNotExist() {
        when(tasklistOutputGateway.findById("tasklist-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskUseCase.create(Task.builder()
                        .title("Comprar leite")
                        .tasklistId("tasklist-id")
                        .build(), "user-id"))
                .isInstanceOf(TasklistNotFoundException.class);

        verify(taskOutputGateway, never()).create(any());
    }

    @Test
    void findAllShouldReturnAllTasksWhenTasklistFilterIsBlank() {
        List<Task> tasks = List.of(
                Task.builder().id("1").ownerId("user-id").tasklistId("a").build(),
                Task.builder().id("2").ownerId("user-id").tasklistId("b").build()
        );

        when(taskOutputGateway.findAllByOwnerId("user-id")).thenReturn(tasks);

        assertThat(taskUseCase.findAll("user-id", "   ")).containsExactlyElementsOf(tasks);
        verify(taskOutputGateway).findAllByOwnerId("user-id");
        verify(tasklistOutputGateway, never()).findById(any());
    }

    @Test
    void findAllShouldFilterByOwnedTasklistWhenTasklistIdIsProvided() {
        List<Task> tasks = List.of(Task.builder().id("1").ownerId("user-id").tasklistId("tasklist-id").build());
        when(tasklistOutputGateway.findById("tasklist-id"))
                .thenReturn(Optional.of(Tasklist.builder()
                        .id("tasklist-id")
                        .ownerId("user-id")
                        .build()));
        when(taskOutputGateway.findAllByOwnerIdAndTasklistId("user-id", "tasklist-id")).thenReturn(tasks);

        assertThat(taskUseCase.findAll("user-id", "tasklist-id")).containsExactlyElementsOf(tasks);
    }

    @Test
    void findAllShouldThrowForbiddenWhenTasklistBelongsToAnotherUser() {
        when(tasklistOutputGateway.findById("tasklist-id"))
                .thenReturn(Optional.of(Tasklist.builder()
                        .id("tasklist-id")
                        .ownerId("other-user")
                        .build()));

        assertThatThrownBy(() -> taskUseCase.findAll("user-id", "tasklist-id"))
                .isInstanceOf(ForbiddenException.class);

        verify(taskOutputGateway, never()).findAllByOwnerIdAndTasklistId(any(), any());
    }

    @Test
    void findByIdShouldReturnTaskWhenItBelongsToAuthenticatedUser() {
        Task storedTask = Task.builder()
                .id("task-id")
                .ownerId("user-id")
                .tasklistId("tasklist-id")
                .title("Task A")
                .build();

        when(taskOutputGateway.findById("task-id")).thenReturn(Optional.of(storedTask));

        Task result = taskUseCase.findById("task-id", "user-id");

        assertThat(result).isEqualTo(storedTask);
    }

    @Test
    void findByIdShouldThrowForbiddenWhenTaskBelongsToAnotherUser() {
        when(taskOutputGateway.findById("task-id"))
                .thenReturn(Optional.of(Task.builder()
                        .id("task-id")
                        .ownerId("other-user")
                        .build()));

        assertThatThrownBy(() -> taskUseCase.findById("task-id", "user-id"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findByIdShouldThrowNotFoundWhenTaskDoesNotExist() {
        when(taskOutputGateway.findById("task-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskUseCase.findById("task-id", "user-id"))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void updateShouldRequireCompletedFlag() {
        when(taskOutputGateway.findById("task-id"))
                .thenReturn(Optional.of(Task.builder()
                        .id("task-id")
                        .ownerId("user-id")
                        .tasklistId("tasklist-id")
                        .build()));
        when(tasklistOutputGateway.findById("tasklist-id"))
                .thenReturn(Optional.of(Tasklist.builder()
                        .id("tasklist-id")
                        .ownerId("user-id")
                        .build()));

        assertThatThrownBy(() -> taskUseCase.update("task-id", Task.builder()
                        .title("Novo titulo")
                        .tasklistId("tasklist-id")
                        .build(), "user-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Completed must not be null");
    }

    @Test
    void updateShouldIgnoreCurrentTaskWhenCheckingDuplicate() {
        when(taskOutputGateway.findById("task-id"))
                .thenReturn(Optional.of(Task.builder()
                        .id("task-id")
                        .ownerId("user-id")
                        .tasklistId("tasklist-id")
                        .build()));
        when(tasklistOutputGateway.findById("tasklist-id"))
                .thenReturn(Optional.of(Tasklist.builder()
                        .id("tasklist-id")
                        .ownerId("user-id")
                        .build()));
        when(taskOutputGateway.existsByTasklistIdAndTitleAndIdNot("tasklist-id", "Atualizada", "task-id"))
                .thenReturn(false);
        when(taskOutputGateway.update(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task result = taskUseCase.update("task-id", Task.builder()
                .title(" Atualizada ")
                .description(" desc ")
                .completed(true)
                .tasklistId("tasklist-id")
                .build(), "user-id");

        assertThat(result.getTitle()).isEqualTo("Atualizada");
        assertThat(result.getDescription()).isEqualTo("desc");
        assertThat(result.getCompleted()).isTrue();
    }

    @Test
    void updateShouldThrowNotFoundWhenTaskDoesNotExist() {
        when(taskOutputGateway.findById("task-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskUseCase.update("task-id", Task.builder()
                        .title("Atualizada")
                        .completed(true)
                        .tasklistId("tasklist-id")
                        .build(), "user-id"))
                .isInstanceOf(TaskNotFoundException.class);

        verify(taskOutputGateway, never()).update(any());
    }

    @Test
    void updateShouldThrowForbiddenWhenTaskBelongsToAnotherUser() {
        when(taskOutputGateway.findById("task-id"))
                .thenReturn(Optional.of(Task.builder()
                        .id("task-id")
                        .ownerId("other-user")
                        .tasklistId("tasklist-id")
                        .build()));

        assertThatThrownBy(() -> taskUseCase.update("task-id", Task.builder()
                        .title("Atualizada")
                        .completed(true)
                        .tasklistId("tasklist-id")
                        .build(), "user-id"))
                .isInstanceOf(ForbiddenException.class);

        verify(taskOutputGateway, never()).update(any());
    }

    @Test
    void updateShouldThrowNotFoundWhenTasklistDoesNotExist() {
        when(taskOutputGateway.findById("task-id"))
                .thenReturn(Optional.of(Task.builder()
                        .id("task-id")
                        .ownerId("user-id")
                        .tasklistId("tasklist-id")
                        .build()));
        when(tasklistOutputGateway.findById("tasklist-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskUseCase.update("task-id", Task.builder()
                        .title("Atualizada")
                        .completed(true)
                        .tasklistId("tasklist-id")
                        .build(), "user-id"))
                .isInstanceOf(TasklistNotFoundException.class);

        verify(taskOutputGateway, never()).update(any());
    }

    @Test
    void updateShouldThrowForbiddenWhenDestinationTasklistBelongsToAnotherUser() {
        when(taskOutputGateway.findById("task-id"))
                .thenReturn(Optional.of(Task.builder()
                        .id("task-id")
                        .ownerId("user-id")
                        .tasklistId("tasklist-id")
                        .build()));
        when(tasklistOutputGateway.findById("tasklist-id"))
                .thenReturn(Optional.of(Tasklist.builder()
                        .id("tasklist-id")
                        .ownerId("other-user")
                        .build()));

        assertThatThrownBy(() -> taskUseCase.update("task-id", Task.builder()
                        .title("Atualizada")
                        .completed(true)
                        .tasklistId("tasklist-id")
                        .build(), "user-id"))
                .isInstanceOf(ForbiddenException.class);

        verify(taskOutputGateway, never()).update(any());
    }

    @Test
    void updateShouldThrowConflictWhenAnotherTaskUsesSameNormalizedTitle() {
        when(taskOutputGateway.findById("task-id"))
                .thenReturn(Optional.of(Task.builder()
                        .id("task-id")
                        .ownerId("user-id")
                        .tasklistId("tasklist-id")
                        .title("Atual")
                        .build()));
        when(tasklistOutputGateway.findById("tasklist-id"))
                .thenReturn(Optional.of(Tasklist.builder()
                        .id("tasklist-id")
                        .ownerId("user-id")
                        .build()));
        when(taskOutputGateway.existsByTasklistIdAndTitleAndIdNot("tasklist-id", "Duplicada", "task-id"))
                .thenReturn(true);

        assertThatThrownBy(() -> taskUseCase.update("task-id", Task.builder()
                        .title(" Duplicada ")
                        .completed(true)
                        .tasklistId("tasklist-id")
                        .build(), "user-id"))
                .isInstanceOf(TaskAlreadyExistsException.class);

        verify(taskOutputGateway, never()).update(any());
    }

    @Test
    void deleteShouldThrowNotFoundWhenTaskDoesNotExist() {
        when(taskOutputGateway.findById("task-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskUseCase.delete("task-id", "user-id"))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void deleteShouldThrowForbiddenWhenTaskBelongsToAnotherUser() {
        when(taskOutputGateway.findById("task-id"))
                .thenReturn(Optional.of(Task.builder()
                        .id("task-id")
                        .ownerId("other-user")
                        .build()));

        assertThatThrownBy(() -> taskUseCase.delete("task-id", "user-id"))
                .isInstanceOf(ForbiddenException.class);

        verify(taskOutputGateway, never()).deleteById(any());
    }

    @Test
    void deleteShouldRemoveTaskWhenItBelongsToAuthenticatedUser() {
        when(taskOutputGateway.findById("task-id"))
                .thenReturn(Optional.of(Task.builder()
                        .id("task-id")
                        .ownerId("user-id")
                        .tasklistId("tasklist-id")
                        .build()));

        taskUseCase.delete("task-id", "user-id");

        verify(taskOutputGateway).deleteById("task-id");
    }

    @Test
    void findAllShouldThrowNotFoundWhenFilteringByMissingTasklist() {
        when(tasklistOutputGateway.findById("tasklist-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskUseCase.findAll("user-id", "tasklist-id"))
                .isInstanceOf(TasklistNotFoundException.class);
    }

    @Test
    void createShouldThrowBadRequestWhenTitleIsBlank() {
        when(tasklistOutputGateway.findById("tasklist-id"))
                .thenReturn(Optional.of(Tasklist.builder()
                        .id("tasklist-id")
                        .ownerId("user-id")
                        .build()));

        assertThatThrownBy(() -> taskUseCase.create(Task.builder()
                        .title("   ")
                        .tasklistId("tasklist-id")
                        .build(), "user-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Title must not be blank");

        verify(tasklistOutputGateway).findById("tasklist-id");
        verify(taskOutputGateway, never()).create(any());
    }

    @Test
    void updateShouldThrowBadRequestWhenTitleIsBlank() {
        when(taskOutputGateway.findById("task-id"))
                .thenReturn(Optional.of(Task.builder()
                        .id("task-id")
                        .ownerId("user-id")
                        .tasklistId("tasklist-id")
                        .build()));
        when(tasklistOutputGateway.findById("tasklist-id"))
                .thenReturn(Optional.of(Tasklist.builder()
                        .id("tasklist-id")
                        .ownerId("user-id")
                        .build()));

        assertThatThrownBy(() -> taskUseCase.update("task-id", Task.builder()
                        .title("   ")
                        .completed(true)
                        .tasklistId("tasklist-id")
                        .build(), "user-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Title must not be blank");

        verify(tasklistOutputGateway).findById("tasklist-id");
        verify(taskOutputGateway, never()).update(any());
    }

}
