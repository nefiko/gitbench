package repository;

import entity.BenchmarkRunEntity;
import entity.CommitEntity;
import entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BenchmarkRunRepository extends JpaRepository<BenchmarkRunEntity, Long> {

    Optional<BenchmarkRunEntity> findByRunId(String runId);

    List<BenchmarkRunEntity> findByProjectOrderByCreatedAtDesc(ProjectEntity project);

    List<BenchmarkRunEntity> findByCommitOrderByCreatedAtDesc(CommitEntity commit);

    Optional<BenchmarkRunEntity> findFirstByCommitOrderByCreatedAtDesc(CommitEntity commit);

    List<BenchmarkRunEntity> findByProjectAndStatus(ProjectEntity project,
                                                    BenchmarkRunEntity.RunStatus status);

    @Query("SELECT r FROM BenchmarkRunEntity r " +
            "WHERE r.project.id = :projectId " +
            "ORDER BY r.createdAt DESC " +
            "LIMIT :limit")
    List<BenchmarkRunEntity> findRecentRuns(@Param("projectId") Long projectId,
                                            @Param("limit") int limit);

    @Query("SELECT r FROM BenchmarkRunEntity r " +
            "WHERE r.project = :project AND r.regressionCount > 0 " +
            "ORDER BY r.createdAt DESC")
    List<BenchmarkRunEntity> findRunsWithRegressions(@Param("project") ProjectEntity project);

    @Query("SELECT r FROM BenchmarkRunEntity r " +
            "WHERE r.project = :project AND r.status = 'COMPLETED' " +
            "ORDER BY r.createdAt DESC")
    List<BenchmarkRunEntity> findCompletedRuns(@Param("project") ProjectEntity project);

    long countByProjectAndStatus(ProjectEntity project, BenchmarkRunEntity.RunStatus status);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM BenchmarkRunEntity r " +
            "WHERE r.commit = :commit AND r.status = 'COMPLETED'")
    boolean hasCompletedRun(@Param("commit") CommitEntity commit);

    @Query("SELECT COALESCE(SUM(r.regressionCount), 0) FROM BenchmarkRunEntity r " +
            "WHERE r.project = :project AND r.status = 'COMPLETED'")
    long getTotalRegressionCount(@Param("project") ProjectEntity project);
}