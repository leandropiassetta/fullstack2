package br.com.jtech.tasklist.application.core.usecases;

import br.com.jtech.tasklist.application.core.domains.Tasklist;
import br.com.jtech.tasklist.application.ports.output.TasklistOutputGateway;
import br.com.jtech.tasklist.config.infra.exceptions.ForbiddenException;
import br.com.jtech.tasklist.config.infra.exceptions.TasklistAlreadyExistsException;
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
class TasklistUseCaseTest {

    @Mock
    private TasklistOutputGateway tasklistOutputGateway;

    @InjectMocks
    private TasklistUseCase tasklistUseCase;

    @Test
    void createShouldTrimNameAndAssociateAuthenticatedUser() {
        Tasklist createdTasklist = Tasklist.builder()
                .id("tasklist-id")
                .name("Trabalho")
                .ownerId("user-id")
                .build();

        when(tasklistOutputGateway.existsByOwnerIdAndName("user-id", "Trabalho")).thenReturn(false);
        when(tasklistOutputGateway.create(any(Tasklist.class))).thenReturn(createdTasklist);

        Tasklist result = tasklistUseCase.create(Tasklist.builder().name("  Trabalho  ").build(), "user-id");

        assertThat(result).isEqualTo(createdTasklist);
        verify(tasklistOutputGateway).create(argThat(tasklist ->
                tasklist.getOwnerId().equals("user-id") && tasklist.getName().equals("Trabalho")
        ));
    }

    @Test
    void createShouldThrowConflictWhenNameAlreadyExistsForUser() {
        when(tasklistOutputGateway.existsByOwnerIdAndName("user-id", "Trabalho")).thenReturn(true);

        assertThatThrownBy(() -> tasklistUseCase.create(Tasklist.builder().name("Trabalho").build(), "user-id"))
                .isInstanceOf(TasklistAlreadyExistsException.class);

        verify(tasklistOutputGateway, never()).create(any());
    }

    @Test
    void createShouldThrowBadRequestWhenNameIsBlank() {
        assertThatThrownBy(() -> tasklistUseCase.create(Tasklist.builder().name("   ").build(), "user-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name must not be blank");

        verify(tasklistOutputGateway, never()).existsByOwnerIdAndName(any(), any());
        verify(tasklistOutputGateway, never()).create(any());
    }

    @Test
    void findByIdShouldThrowForbiddenWhenTasklistBelongsToAnotherUser() {
        when(tasklistOutputGateway.findById("tasklist-id"))
                .thenReturn(Optional.of(Tasklist.builder()
                        .id("tasklist-id")
                        .name("Pessoal")
                        .ownerId("other-user")
                        .build()));

        assertThatThrownBy(() -> tasklistUseCase.findById("tasklist-id", "user-id"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findByIdShouldThrowNotFoundWhenTasklistDoesNotExist() {
        when(tasklistOutputGateway.findById("tasklist-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tasklistUseCase.findById("tasklist-id", "user-id"))
                .isInstanceOf(TasklistNotFoundException.class);
    }

    @Test
    void findByIdShouldReturnTasklistWhenItBelongsToAuthenticatedUser() {
        Tasklist storedTasklist = Tasklist.builder()
                .id("tasklist-id")
                .name("Pessoal")
                .ownerId("user-id")
                .build();

        when(tasklistOutputGateway.findById("tasklist-id")).thenReturn(Optional.of(storedTasklist));

        Tasklist result = tasklistUseCase.findById("tasklist-id", "user-id");

        assertThat(result).isEqualTo(storedTasklist);
    }

    @Test
    void updateShouldIgnoreCurrentRecordWhenCheckingDuplicate() {
        Tasklist existing = Tasklist.builder()
                .id("tasklist-id")
                .name("Atual")
                .ownerId("user-id")
                .build();

        when(tasklistOutputGateway.findById("tasklist-id")).thenReturn(Optional.of(existing));
        when(tasklistOutputGateway.existsByOwnerIdAndNameAndIdNot("user-id", "Atual", "tasklist-id")).thenReturn(false);
        when(tasklistOutputGateway.update(any(Tasklist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tasklist result = tasklistUseCase.update(
                "tasklist-id",
                Tasklist.builder().name("  Atual  ").build(),
                "user-id"
        );

        assertThat(result.getName()).isEqualTo("Atual");
    }

    @Test
    void updateShouldThrowNotFoundWhenTasklistDoesNotExist() {
        when(tasklistOutputGateway.findById("tasklist-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tasklistUseCase.update(
                "tasklist-id",
                Tasklist.builder().name("Atualizada").build(),
                "user-id"
        )).isInstanceOf(TasklistNotFoundException.class);

        verify(tasklistOutputGateway, never()).update(any());
    }

    @Test
    void updateShouldThrowForbiddenWhenTasklistBelongsToAnotherUser() {
        when(tasklistOutputGateway.findById("tasklist-id"))
                .thenReturn(Optional.of(Tasklist.builder()
                        .id("tasklist-id")
                        .name("Atual")
                        .ownerId("other-user")
                        .build()));

        assertThatThrownBy(() -> tasklistUseCase.update(
                "tasklist-id",
                Tasklist.builder().name("Atualizada").build(),
                "user-id"
        )).isInstanceOf(ForbiddenException.class);

        verify(tasklistOutputGateway, never()).update(any());
    }

    @Test
    void updateShouldThrowConflictWhenAnotherTasklistUsesSameNormalizedName() {
        Tasklist existing = Tasklist.builder()
                .id("tasklist-id")
                .name("Atual")
                .ownerId("user-id")
                .build();

        when(tasklistOutputGateway.findById("tasklist-id")).thenReturn(Optional.of(existing));
        when(tasklistOutputGateway.existsByOwnerIdAndNameAndIdNot("user-id", "Trabalho", "tasklist-id"))
                .thenReturn(true);

        assertThatThrownBy(() -> tasklistUseCase.update(
                "tasklist-id",
                Tasklist.builder().name("  Trabalho  ").build(),
                "user-id"
        )).isInstanceOf(TasklistAlreadyExistsException.class);

        verify(tasklistOutputGateway, never()).update(any());
    }

    @Test
    void updateShouldThrowBadRequestWhenNameIsBlank() {
        when(tasklistOutputGateway.findById("tasklist-id")).thenReturn(Optional.of(Tasklist.builder()
                .id("tasklist-id")
                .name("Atual")
                .ownerId("user-id")
                .build()));

        assertThatThrownBy(() -> tasklistUseCase.update(
                "tasklist-id",
                Tasklist.builder().name("   ").build(),
                "user-id"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name must not be blank");

        verify(tasklistOutputGateway, never()).update(any());
    }

    @Test
    void deleteShouldRemoveTasklistEvenWhenItHasTasks() {
        when(tasklistOutputGateway.findById("tasklist-id"))
                .thenReturn(Optional.of(Tasklist.builder()
                        .id("tasklist-id")
                        .name("Estudos")
                        .ownerId("user-id")
                        .build()));

        tasklistUseCase.delete("tasklist-id", "user-id");

        verify(tasklistOutputGateway).deleteById("tasklist-id");
    }

    @Test
    void deleteShouldThrowNotFoundWhenTasklistDoesNotExist() {
        when(tasklistOutputGateway.findById("tasklist-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tasklistUseCase.delete("tasklist-id", "user-id"))
                .isInstanceOf(TasklistNotFoundException.class);

        verify(tasklistOutputGateway, never()).deleteById(any());
    }

    @Test
    void deleteShouldThrowForbiddenWhenTasklistBelongsToAnotherUser() {
        when(tasklistOutputGateway.findById("tasklist-id"))
                .thenReturn(Optional.of(Tasklist.builder()
                        .id("tasklist-id")
                        .name("Estudos")
                        .ownerId("other-user")
                        .build()));

        assertThatThrownBy(() -> tasklistUseCase.delete("tasklist-id", "user-id"))
                .isInstanceOf(ForbiddenException.class);

        verify(tasklistOutputGateway, never()).deleteById(any());
    }

    @Test
    void findAllShouldReturnOnlyGatewayResult() {
        List<Tasklist> tasklists = List.of(
                Tasklist.builder().id("1").name("A").ownerId("user-id").build(),
                Tasklist.builder().id("2").name("B").ownerId("user-id").build()
        );

        when(tasklistOutputGateway.findAllByOwnerId("user-id")).thenReturn(tasklists);

        assertThat(tasklistUseCase.findAll("user-id")).containsExactlyElementsOf(tasklists);
    }

}
