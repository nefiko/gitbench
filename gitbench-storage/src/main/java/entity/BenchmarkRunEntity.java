package entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "benchmark_runs", indexes = {
        @Index(name = "idx_run_project", columnList = "project_id"),
        @Index(name = "idx_run_commit", columnList = "commit_id"),
        @Index(name = "idx_run_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BenchmarkRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false, unique = true)
    private String runId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commit_id", nullable = false)
    private CommitEntity commit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RunStatus status = RunStatus.PENDING;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "warmup_iterations")
    @Builder.Default
    private int warmupIterations = 3;

    @Column(name = "measurement_iterations")
    @Builder.Default
    private int measurementIterations = 5;

    @Builder.Default
    private int forks = 1;

    @Column(name = "jvm_args")
    private String jvmArgs;

    @Column(name = "java_version")
    private String javaVersion;

    @Column(name = "os_info")
    private String osInfo;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "total_methods")
    @Builder.Default
    private int totalMethods = 0;

    @Column(name = "successful_count")
    @Builder.Default
    private int successfulCount = 0;

    @Column(name = "failed_count")
    @Builder.Default
    private int failedCount = 0;

    @Column(name = "regression_count")
    @Builder.Default
    private int regressionCount = 0;

    @Column(name = "improvement_count")
    @Builder.Default
    private int improvementCount = 0;

    @OneToMany(mappedBy = "benchmarkRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BenchmarkResultEntity> results = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public enum RunStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}