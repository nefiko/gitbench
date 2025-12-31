package util;

import entity.*;
import model.BenchmarkResult;
import model.CommitInfo;
import model.HotspotCandidate;
import model.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class EntityMapper {

    public CommitInfo toModel(CommitEntity entity) {
        if (entity == null) return null;

        return CommitInfo.builder()
                .hash(entity.getHash())
                .shortHash(entity.getShortHash())
                .message(entity.getMessage())
                .authorName(entity.getAuthorName())
                .authorEmail(entity.getAuthorEmail())
                .timestamp(entity.getCommitTimestamp())
                .parentHashes(entity.getParentHash() != null
                        ? List.of(entity.getParentHash())
                        : Collections.emptyList())
                .build();
    }

    public CommitEntity toEntity(CommitInfo model, ProjectEntity project) {
        if (model == null) return null;

        return CommitEntity.builder()
                .project(project)
                .hash(model.getHash())
                .shortHash(model.getShortHash())
                .message(model.getMessage())
                .authorName(model.getAuthorName())
                .authorEmail(model.getAuthorEmail())
                .commitTimestamp(model.getTimestamp())
                .parentHash(model.getFirstParentHash())
                .merge(model.isMergeCommit())
                .build();
    }

    public MethodSignature toModel(MethodEntity entity) {
        if (entity == null) return null;

        List<String> paramTypes = entity.getParameterTypes() != null && !entity.getParameterTypes().isEmpty()
                ? Arrays.asList(entity.getParameterTypes().split(","))
                : Collections.emptyList();

        return MethodSignature.builder()
                .className(entity.getClassName())
                .methodName(entity.getMethodName())
                .parameterTypes(paramTypes)
                .returnType(entity.getReturnType())
                .sourceFilePath(entity.getSourceFile())
                .build();
    }

    public MethodEntity toEntity(MethodSignature model, ProjectEntity project) {
        if (model == null) return null;

        String paramTypes = model.getParameterTypes() != null
                ? String.join(",", model.getParameterTypes())
                : "";

        return MethodEntity.builder()
                .project(project)
                .signatureId(model.getUniqueId())
                .className(model.getClassName())
                .methodName(model.getMethodName())
                .parameterTypes(paramTypes)
                .returnType(model.getReturnType())
                .sourceFile(model.getSourceFilePath())
                .build();
    }

    public MethodEntity toEntity(HotspotCandidate candidate, ProjectEntity project) {
        if (candidate == null) return null;

        MethodEntity entity = toEntity(candidate.getMethodSignature(), project);
        entity.setPriorityScore(candidate.getPriorityScore());
        entity.setWhitelisted(candidate.isManuallyAdded());

        if (candidate.getReasons() != null && !candidate.getReasons().isEmpty()) {
            String reasons = candidate.getReasons().stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(","));
            entity.setHotspotReasons(reasons);
        }

        return entity;
    }

    public BenchmarkResult toModel(BenchmarkResultEntity entity) {
        if (entity == null) return null;

        BenchmarkResult.ExecutionStats execStats = BenchmarkResult.ExecutionStats.builder()
                .averageTime(entity.getAvgTime() != null ? entity.getAvgTime() : 0.0)
                .minTime(entity.getMinTime() != null ? entity.getMinTime() : 0.0)
                .maxTime(entity.getMaxTime() != null ? entity.getMaxTime() : 0.0)
                .standardDeviation(entity.getStdDev() != null ? entity.getStdDev() : 0.0)
                .throughput(entity.getThroughput() != null ? entity.getThroughput() : 0.0)
                .iterations(entity.getIterations() != null ? entity.getIterations() : 0)
                .build();

        BenchmarkResult.MemoryStats memStats = BenchmarkResult.MemoryStats.builder()
                .heapUsed(entity.getHeapUsed() != null ? entity.getHeapUsed() : 0L)
                .gcTimeMs(entity.getGcTimeMs() != null ? entity.getGcTimeMs() : 0L)
                .allocationRate(entity.getAllocationRate() != null ? entity.getAllocationRate() : 0.0)
                .build();

        return BenchmarkResult.builder()
                .id(entity.getId().toString())
                .methodSignature(toModel(entity.getMethod()))
                .commitInfo(toModel(entity.getCommit()))
                .executionStats(execStats)
                .memoryStats(memStats)
                .runTimestamp(entity.getCreatedAt())
                .mode(toBenchmarkMode(entity.getBenchmarkMode()))
                .timeUnit(toTimeUnit(entity.getTimeUnit()))
                .successful(entity.isSuccessful())
                .errorMessage(entity.getErrorMessage())
                .build();
    }

    public BenchmarkResultEntity toEntity(BenchmarkResult model,
                                          BenchmarkRunEntity run,
                                          MethodEntity method,
                                          CommitEntity commit) {
        if (model == null) return null;

        BenchmarkResultEntity entity = BenchmarkResultEntity.builder()
                .benchmarkRun(run)
                .method(method)
                .commit(commit)
                .successful(model.isSuccessful())
                .errorMessage(model.getErrorMessage())
                .benchmarkMode(model.getMode() != null ? model.getMode().name() : null)
                .timeUnit(model.getTimeUnit() != null ? model.getTimeUnit().name() : "MILLISECONDS")
                .build();

        if (model.getExecutionStats() != null) {
            BenchmarkResult.ExecutionStats stats = model.getExecutionStats();
            entity.setAvgTime(stats.getAverageTime());
            entity.setMinTime(stats.getMinTime());
            entity.setMaxTime(stats.getMaxTime());
            entity.setStdDev(stats.getStandardDeviation());
            entity.setThroughput(stats.getThroughput());
            entity.setIterations(stats.getIterations());

            if (stats.getPercentiles() != null) {
                entity.setP50(stats.getPercentiles().get("p50"));
                entity.setP90(stats.getPercentiles().get("p90"));
                entity.setP99(stats.getPercentiles().get("p99"));
            }
        }

        if (model.getMemoryStats() != null) {
            BenchmarkResult.MemoryStats mem = model.getMemoryStats();
            entity.setHeapUsed(mem.getHeapUsed());
            entity.setGcTimeMs(mem.getGcTimeMs());
            entity.setAllocationRate(mem.getAllocationRate());
        }

        return entity;
    }

    public BenchmarkRunEntity toEntity(ProjectEntity project, CommitEntity commit) {
        return BenchmarkRunEntity.builder()
                .runId(UUID.randomUUID().toString())
                .project(project)
                .commit(commit)
                .status(BenchmarkRunEntity.RunStatus.PENDING)
                .javaVersion(System.getProperty("java.version"))
                .osInfo(System.getProperty("os.name") + " " + System.getProperty("os.version"))
                .build();
    }

    private BenchmarkResult.BenchmarkMode toBenchmarkMode(String mode) {
        if (mode == null) return BenchmarkResult.BenchmarkMode.AVERAGE_TIME;
        try {
            return BenchmarkResult.BenchmarkMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            return BenchmarkResult.BenchmarkMode.AVERAGE_TIME;
        }
    }

    private TimeUnit toTimeUnit(String unit) {
        if (unit == null) return TimeUnit.MILLISECONDS;
        try {
            return TimeUnit.valueOf(unit);
        } catch (IllegalArgumentException e) {
            return TimeUnit.MILLISECONDS;
        }
    }
}