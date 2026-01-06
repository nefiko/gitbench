package web.controller;

import model.BenchmarkResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.BenchmarkStorageService;
import service.StorageServiceFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/compare")
@CrossOrigin(origins = "*")
public class CompareController {

    private final BenchmarkStorageService storageService;

    public CompareController() {
        this.storageService = StorageServiceFactory.createDefault();
    }

    @GetMapping("/{projectName}")
    public ResponseEntity<Map<String, Object>> compareCommits(@PathVariable String projectName,
                                                              @RequestParam String baseCommit,
                                                              @RequestParam String targetCommit,
                                                              @RequestParam(defaultValue = "5.0") double threshold) {
        List<BenchmarkResult> baseResults = storageService.getResultsForCommit(baseCommit, projectName);
        List<BenchmarkResult> targetResults = storageService.getResultsForCommit(targetCommit, projectName);

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("baseCommit", baseCommit);
        comparison.put("targetCommit", targetCommit);
        comparison.put("baseResults", baseResults);
        comparison.put("targetResults", targetResults);
        comparison.put("threshold", threshold);

        int regressions = 0;
        int improvements = 0;
        int unchanged = 0;

        Map<String, BenchmarkResult> baseByMethod = new HashMap<>();
        for (BenchmarkResult r : baseResults) {
            if (r.getMethodSignature() != null) {
                String key = r.getMethodSignature().getClassName() + "#" + r.getMethodSignature().getMethodName();
                baseByMethod.put(key, r);
            }
        }

        for (BenchmarkResult target : targetResults) {
            if (target.getMethodSignature() == null || !target.isSuccessful()) {
                continue;
            }

            String key = target.getMethodSignature().getClassName() + "#" + target.getMethodSignature().getMethodName();
            BenchmarkResult base = baseByMethod.get(key);

            if (base == null || !base.isSuccessful()) {
                continue;
            }

            double baseTime = base.getExecutionStats().getAverageTime();
            double targetTime = target.getExecutionStats().getAverageTime();
            double changePercent = ((targetTime - baseTime) / baseTime) * 100;

            if (changePercent > threshold) {
                regressions++;
            } else if (changePercent < -threshold) {
                improvements++;
            } else {
                unchanged++;
            }
        }

        comparison.put("regressions", regressions);
        comparison.put("improvements", improvements);
        comparison.put("unchanged", unchanged);

        return ResponseEntity.ok(comparison);
    }
}