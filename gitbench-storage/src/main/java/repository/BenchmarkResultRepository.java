package repository;

import entity.BenchmarkResultEntity;
import entity.BenchmarkRunEntity;
import entity.CommitEntity;
import entity.MethodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BenchmarkResultRepository extends JpaRepository<BenchmarkResultEntity, Long> {

    List<BenchmarkResultEntity> findByBenchmarkRun(BenchmarkRunEntity run);

    List<BenchmarkResultEntity> findByMethodOrderByCreatedAtDesc(MethodEntity method);

    Optional<BenchmarkResultEntity> findByMethodAndCommit(MethodEntity method, CommitEntity commit);

    Optional<BenchmarkResultEntity> findFirstByMethodOrderByCreatedAtDesc(MethodEntity method);

    @Query("SELECT r FROM BenchmarkResultEntity r " +
            "WHERE r.benchmarkRun = :run " +
            "AND r.changeType IN ('SIGNIFICANT_REGRESSION', 'MINOR_REGRESSION')")
    List<BenchmarkResultEntity> findRegressions(@Param("run") BenchmarkRunEntity run);

    @Query("SELECT r FROM BenchmarkResultEntity r " +
            "WHERE r.benchmarkRun = :run " +
            "AND r.changeType IN ('SIGNIFICANT_IMPROVEMENT', 'MINOR_IMPROVEMENT')")
    List<BenchmarkResultEntity> findImprovements(@Param("run") BenchmarkRunEntity run);

    @Query("SELECT r FROM BenchmarkResultEntity r " +
            "WHERE r.method = :method AND r.successful = true " +
            "ORDER BY r.createdAt ASC")
    List<BenchmarkResultEntity> findHistoryForMethod(@Param("method") MethodEntity method);

    @Query("SELECT r FROM BenchmarkResultEntity r " +
            "WHERE r.method.id = :methodId AND r.successful = true " +
            "ORDER BY r.createdAt DESC " +
            "LIMIT :limit")
    List<BenchmarkResultEntity> findRecentResultsForMethod(@Param("methodId") Long methodId,
                                                           @Param("limit") int limit);

    @Query("SELECT AVG(r.avgTime) FROM BenchmarkResultEntity r " +
            "WHERE r.method = :method AND r.successful = true")
    Double getAverageTimeForMethod(@Param("method") MethodEntity method);

    @Query("SELECT r FROM BenchmarkResultEntity r " +
            "WHERE r.benchmarkRun = :run AND r.successful = true " +
            "ORDER BY r.avgTime DESC " +
            "LIMIT :limit")
    List<BenchmarkResultEntity> findSlowestInRun(@Param("run") BenchmarkRunEntity run,
                                                 @Param("limit") int limit);

    @Query("SELECT r FROM BenchmarkResultEntity r " +
            "WHERE r.benchmarkRun = :run AND r.changePercent IS NOT NULL " +
            "ORDER BY r.changePercent DESC " +
            "LIMIT :limit")
    List<BenchmarkResultEntity> findBiggestRegressions(@Param("run") BenchmarkRunEntity run,
                                                       @Param("limit") int limit);

    long countByBenchmarkRunAndSuccessfulTrue(BenchmarkRunEntity run);

    @Query("SELECT COUNT(r) FROM BenchmarkResultEntity r " +
            "WHERE r.benchmarkRun = :run " +
            "AND r.changeType IN ('SIGNIFICANT_REGRESSION', 'MINOR_REGRESSION')")
    long countRegressions(@Param("run") BenchmarkRunEntity run);

    @Query("SELECT r FROM BenchmarkResultEntity r " +
            "WHERE r.method = :method " +
            "AND r.commit.hash = :parentHash " +
            "AND r.successful = true " +
            "ORDER BY r.createdAt DESC " +
            "LIMIT 1")
    Optional<BenchmarkResultEntity> findBaselineResult(@Param("method") MethodEntity method,
                                                       @Param("parentHash") String parentHash);
}
