package entity;

import enums.ChangeType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "benchmark_results", indexes = {
        @Index(name = "idx_result_run", columnList = "benchmark_run_id"),
        @Index(name = "idx_result_method", columnList = "method_id"),
        @Index(name = "idx_result_commit", columnList = "commit_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BenchmarkResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benchmark_run_id", nullable = false)
    private BenchmarkRunEntity benchmarkRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "method_id", nullable = false)
    private MethodEntity method;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commit_id", nullable = false)
    private CommitEntity commit;

    @Column(nullable = false)
    @Builder.Default
    private boolean successful = false;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "avg_time")
    private Double avgTime;

    @Column(name = "min_time")
    private Double minTime;

    @Column(name = "max_time")
    private Double maxTime;

    @Column(name = "std_dev")
    private Double stdDev;

    @Column(name = "p50")
    private Double p50;

    @Column(name = "p90")
    private Double p90;

    @Column(name = "p99")
    private Double p99;

    private Double throughput;

    @Column(name = "time_unit")
    @Builder.Default
    private String timeUnit = "ms";

    @Column(name = "benchmark_mode")
    private String benchmarkMode;

    private Integer iterations;

    @Column(name = "heap_used")
    private Long heapUsed;

    @Column(name = "gc_time_ms")
    private Long gcTimeMs;

    @Column(name = "allocation_rate")
    private Double allocationRate;

    @Column(name = "change_percent")
    private Double changePercent;

    @Column(name = "change_absolute")
    private Double changeAbsolute;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type")
    private ChangeType changeType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}