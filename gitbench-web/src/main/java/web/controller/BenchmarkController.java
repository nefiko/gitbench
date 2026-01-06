package web.controller;

import model.BenchmarkResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.BenchmarkStorageService;
import service.StorageServiceFactory;

import java.util.List;

@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    private final BenchmarkStorageService benchmarkStorageService;

    public BenchmarkController() {
        this.benchmarkStorageService = StorageServiceFactory.createDefault();
    }

    @GetMapping("/commit/{projectName}/{commitHash}")
    public ResponseEntity<List<BenchmarkResult>> getResultsForCommit(@PathVariable String projectName,
                                                                     @PathVariable String commitHash) {
        List<BenchmarkResult> results = benchmarkStorageService.getResultsForCommit(commitHash, projectName);
        return ResponseEntity.ok(results);
    }
}
