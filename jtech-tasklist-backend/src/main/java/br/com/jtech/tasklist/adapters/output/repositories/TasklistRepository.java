package br.com.jtech.tasklist.adapters.output.repositories;

import br.com.jtech.tasklist.adapters.output.repositories.entities.TasklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TasklistRepository extends JpaRepository<TasklistEntity, UUID> {

    List<TasklistEntity> findAllByOwnerIdOrderByNameAsc(UUID ownerId);

    @Query("""
            select count(t) > 0
            from TasklistEntity t
            where t.owner.id = :ownerId
              and lower(trim(t.name)) = lower(trim(:name))
            """)
    boolean existsByOwnerIdAndNormalizedName(@Param("ownerId") UUID ownerId, @Param("name") String name);

    @Query("""
            select count(t) > 0
            from TasklistEntity t
            where t.owner.id = :ownerId
              and lower(trim(t.name)) = lower(trim(:name))
              and t.id <> :tasklistId
            """)
    boolean existsByOwnerIdAndNormalizedNameAndIdNot(@Param("ownerId") UUID ownerId,
                                                     @Param("name") String name,
                                                     @Param("tasklistId") UUID tasklistId);

}
