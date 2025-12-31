package model;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
@Builder
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
        THROUGHPUT,        // Operations per unit time
        AVERAGE_TIME,      // Average time per operation
        SAMPLE_TIME,       // Samples the time for each operation
        SINGLE_SHOT_TIME,  // Single invocation time
        ALL                // All modes
    }
}