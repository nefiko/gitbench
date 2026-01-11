package web.dto;

import lombok.Data;
import model.BenchmarkResult;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
public class BenchmarkResultDto {

    private String id;

    private String className;
    private String methodName;
    private String commitHash;

    private Double averageTime;
    private Double standardDeviation;

    private Instant runTimestamp;
    private BenchmarkResult.BenchmarkMode mode;
    private TimeUnit timeUnit;
    private Map<String, String> metadata;

    private boolean successful;
    private String errorMessage;

    public static BenchmarkResultDto fromModel(BenchmarkResult br) {
        BenchmarkResultDto dto = new BenchmarkResultDto();
        dto.id = br.getId();

        if (br.getMethodSignature() != null) {
            dto.className = br.getMethodSignature().getClassName();
            dto.methodName = br.getMethodSignature().getMethodName();
        }

        if (br.getCommitInfo() != null) {
            dto.commitHash = br.getCommitInfo().getShortHash();
        }

        if (br.getExecutionStats() != null) {
            dto.averageTime = br.getExecutionStats().getAverageTime();
            dto.standardDeviation = br.getExecutionStats().getStandardDeviation();
        }

        dto.runTimestamp = br.getRunTimestamp();
        dto.mode = br.getMode();
        dto.timeUnit = br.getTimeUnit();
        dto.metadata = br.getMetadata();
        dto.successful = br.isSuccessful();
        dto.errorMessage = br.getErrorMessage();

        return dto;
    }
}
