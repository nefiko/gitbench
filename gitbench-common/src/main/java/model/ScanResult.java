package model;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ScanResult {

    private Path projectPath;

    @Builder.Default
    private List<HotspotCandidate> candidates = new ArrayList<>();

    private int filesScanned;
    private int classesAnalyzed;
    private int methodsAnalyzed;
    private Instant startTime;
    private Instant endTime;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    private String scannerName;

    @Builder.Default
    private Map<String, Integer> reasonCounts = new HashMap<>();

    public Duration getDuration() {
        if (startTime == null || endTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(startTime, endTime);
    }

    public String getSummary() {
        return String.format(
                "Scan complete: %d candidates from %d files (%d classes, %d methods) in %dms",
                candidates.size(),
                filesScanned,
                classesAnalyzed,
                methodsAnalyzed,
                getDuration().toMillis()
        );
    }

    public boolean isSuccessful() {
        return errors.isEmpty();
    }

    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
    }
}