package web.controller;

import model.BenchmarkResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.impl.FileBenchmarkStorageService;
import web.dto.ProjectSummaryDto;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectController {

    @GetMapping("/summary")
    public ResponseEntity<ProjectSummaryDto> getProjectSummary(@RequestParam(name = "projectPath") String projectPath) {
        Path path = Paths.get(projectPath);
        FileBenchmarkStorageService storageService = new FileBenchmarkStorageService(path);
        String projectName = path.getFileName().toString();

        ProjectSummaryDto summary = new ProjectSummaryDto();
        summary.setName(projectName);

        List<BenchmarkResult> history = storageService.getHistory(projectName, 100);

        summary.setTotalBenchmarks(history.size());
        summary.setConfigured(!history.isEmpty());

        long successful = history.stream().filter(BenchmarkResult::isSuccessful).count();
        summary.setSuccessfulRuns((int) successful);
        summary.setFailedRuns(history.size() - (int) successful);

        if (!history.isEmpty()) {
            BenchmarkResult latest = history.get(0);
            summary.setLastRunTimestamp(latest.getRunTimestamp());
            if (latest.getCommitInfo() != null) {
                summary.setLastCommitHash(latest.getCommitInfo().getHash());
            }
        }

        return ResponseEntity.ok(summary);
    }
}