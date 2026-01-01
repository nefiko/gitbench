package repository;

import entity.MethodEntity;
import entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MethodRepository extends JpaRepository<MethodEntity, Long> {
    Optional<MethodEntity> findByProjectAndSignatureId(ProjectEntity project, String uniqueId);
}
