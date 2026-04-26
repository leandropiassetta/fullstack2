package br.com.jtech.tasklist.adapters.output.repositories;

import br.com.jtech.tasklist.adapters.output.repositories.entities.TaskEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {

    @Query("""
            select t
            from TaskEntity t
            where t.owner.id = :ownerId
            order by t.createdAt desc
            """)
    List<TaskEntity> findAllByOwnerIdOrderByCreatedAtDesc(@Param("ownerId") UUID ownerId);

    @Query("""
            select t
            from TaskEntity t
            where t.owner.id = :ownerId
              and t.tasklist.id = :tasklistId
            order by t.createdAt desc
            """)
    List<TaskEntity> findAllByOwnerIdAndTasklistIdOrderByCreatedAtDesc(@Param("ownerId") UUID ownerId,
                                                                       @Param("tasklistId") UUID tasklistId);

    @Query("""
            select count(t) > 0
            from TaskEntity t
            where t.tasklist.id = :tasklistId
              and lower(trim(t.title)) = lower(trim(:title))
            """)
    boolean existsByTasklistIdAndNormalizedTitle(@Param("tasklistId") UUID tasklistId, @Param("title") String title);

    @Query("""
            select count(t) > 0
            from TaskEntity t
            where t.tasklist.id = :tasklistId
              and lower(trim(t.title)) = lower(trim(:title))
              and t.id <> :taskId
            """)
    boolean existsByTasklistIdAndNormalizedTitleAndIdNot(@Param("tasklistId") UUID tasklistId,
                                                         @Param("title") String title,
                                                         @Param("taskId") UUID taskId);

    @Query("""
            select count(t) > 0
            from TaskEntity t
            where t.tasklist.id = :tasklistId
            """)
    boolean existsByTasklistId(@Param("tasklistId") UUID tasklistId);

    @Modifying
    @Query("""
            delete
            from TaskEntity t
            where t.tasklist.id = :tasklistId
            """)
    void deleteAllByTasklistId(@Param("tasklistId") UUID tasklistId);

}
