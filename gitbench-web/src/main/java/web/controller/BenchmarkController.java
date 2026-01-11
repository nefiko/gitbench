package web.controller;

import model.BenchmarkResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.impl.FileBenchmarkStorageService;
import web.dto.BenchmarkResultDto;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/benchmarks")
@CrossOrigin(origins = "*")
public class BenchmarkController {

    @GetMapping("/history")
    public ResponseEntity<List<BenchmarkResultDto>> getHistory(
            @RequestParam(name = "projectPath") String projectPath,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        Path path = Paths.get(projectPath);
        FileBenchmarkStorageService storageService = new FileBenchmarkStorageService(path);
        String projectName = path.getFileName().toString();

        List<BenchmarkResult> results = storageService.getHistory(projectName, limit);
        List<BenchmarkResultDto> dtos = results.stream()
                .map(BenchmarkResultDto::fromModel)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/commit/{commitHash}")
    public ResponseEntity<List<BenchmarkResultDto>> getResultsForCommit(
            @RequestParam(name = "projectPath") String projectPath,
            @PathVariable(name = "commitHash") String commitHash) {
        Path path = Paths.get(projectPath);
        FileBenchmarkStorageService storageService = new FileBenchmarkStorageService(path);
        String projectName = path.getFileName().toString();

        List<BenchmarkResult> results = storageService.getResultsForCommit(commitHash, projectName);
        List<BenchmarkResultDto> dtos = results.stream()
                .map(BenchmarkResultDto::fromModel)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/method")
    public ResponseEntity<List<BenchmarkResultDto>> getResultsForMethod(
            @RequestParam(name = "projectPath") String projectPath,
            @RequestParam(name = "className") String className,
            @RequestParam(name = "methodName") String methodName) {
        Path path = Paths.get(projectPath);
        FileBenchmarkStorageService storageService = new FileBenchmarkStorageService(path);
        String projectName = path.getFileName().toString();

        model.MethodSignature method = new model.MethodSignature();
        method.setClassName(className);
        method.setMethodName(methodName);
        List<BenchmarkResult> results = storageService.getResultsForMethod(method, projectName);
        List<BenchmarkResultDto> dtos = results.stream()
                .map(BenchmarkResultDto::fromModel)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}