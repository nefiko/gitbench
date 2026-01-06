package repository;

import entity.CommitEntity;
import entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommitRepository extends JpaRepository<CommitEntity, Long> {
    Optional<CommitEntity> findByProjectAndHash(ProjectEntity project, String hash);
}
