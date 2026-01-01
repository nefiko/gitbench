package model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BenchmarkResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;

    private MethodSignature methodSignature;

    private CommitInfo commitInfo;

    private ExecutionStats executionStats;

    private MemoryStats memoryStats;

    private Instant runTimestamp;

    private BenchmarkMode mode;

    private TimeUnit timeUnit;

    private Map<String, String> metadata;

    private boolean successful;

    private String errorMessage;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExecutionStats implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private double averageTime;

        private double minTime;

        private double maxTime;

        private double standardDeviation;

        private double throughput;

        private int iterations;

        private int warmupIterations;

        private int forks;

        private Map<String, Double> percentiles;

        private double[] confidenceInterval;
    }


    @Data
    @Builder
    public static class MemoryStats implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private long heapUsed;

        private long nonHeapUsed;

        private long gcCollections;

        private long gcTimeMs;

        private double allocationRate;
    }

    public enum BenchmarkMode {
        THROUGHPUT,
        AVERAGE_TIME,
        SAMPLE_TIME,
        SINGLE_SHOT_TIME,
        ALL
    }
}