package service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import model.BenchmarkResult;
import model.MethodSignature;
import service.BenchmarkStorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FileBenchmarkStorageService implements BenchmarkStorageService {

    private final Path storagePath;
    private final ObjectMapper objectMapper;
    private List<BenchmarkResult> results;

    public FileBenchmarkStorageService(Path projectPath) {
        this.storagePath = projectPath.resolve(".gitbench/results.json");
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.results = loadResults();
    }

    private List<BenchmarkResult> loadResults() {
        if (!Files.exists(storagePath)) {
            return new ArrayList<>();
        }
        try {
            String json = Files.readString(storagePath);
            return objectMapper.readValue(json, new TypeReference<List<BenchmarkResult>>() {});
        } catch (IOException e) {
            System.err.println("Warning: Could not load results from " + storagePath + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void saveToFile() {
        try {
            System.out.println("DEBUG: Saving to " + storagePath.toAbsolutePath());
            Files.createDirectories(storagePath.getParent());
            String json = objectMapper.writeValueAsString(results);
            System.out.println("DEBUG: JSON length = " + json.length() + ", results count = " + results.size());
            Files.writeString(storagePath, json);
            System.out.println("DEBUG: File written successfully");
        } catch (IOException e) {
            System.err.println("Error saving results to " + storagePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void saveResults(List<BenchmarkResult> newResults, String projectName) {
        results.addAll(newResults);
        saveToFile();
    }

    @Override
    public List<BenchmarkResult> getResultsForCommit(String commitHash, String projectName) {
        return results.stream()
                .filter(r -> r.getCommitInfo() != null)
                .filter(r -> r.getCommitInfo().getHash().startsWith(commitHash) ||
                        r.getCommitInfo().getShortHash().equals(commitHash) ||
                        commitHash.startsWith(r.getCommitInfo().getShortHash()))
                .collect(Collectors.toList());
    }

    @Override
    public List<BenchmarkResult> getResultsForMethod(MethodSignature method, String projectName) {
        return results.stream()
                .filter(r -> r.getMethodSignature() != null)
                .filter(r -> r.getMethodSignature().getClassName().equals(method.getClassName()) &&
                        r.getMethodSignature().getMethodName().equals(method.getMethodName()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<BenchmarkResult> getLatestResultForMethod(MethodSignature method, String projectName) {
        return results.stream()
                .filter(r -> r.getMethodSignature() != null)
                .filter(r -> r.getMethodSignature().getClassName().equals(method.getClassName()) &&
                        r.getMethodSignature().getMethodName().equals(method.getMethodName()))
                .max((a, b) -> a.getRunTimestamp().compareTo(b.getRunTimestamp()));
    }

    @Override
    public List<BenchmarkResult> getHistory(String projectName, int limit) {
        return results.stream()
                .sorted((a, b) -> b.getRunTimestamp().compareTo(a.getRunTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<BenchmarkResult> getBaseline(MethodSignature method, String parentCommitHash, String projectName) {
        return results.stream()
                .filter(r -> r.getCommitInfo() != null && r.getMethodSignature() != null)
                .filter(r -> (r.getCommitInfo().getHash().startsWith(parentCommitHash) ||
                        r.getCommitInfo().getShortHash().equals(parentCommitHash)))
                .filter(r -> r.getMethodSignature().getClassName().equals(method.getClassName()) &&
                        r.getMethodSignature().getMethodName().equals(method.getMethodName()))
                .findFirst();
    }

    public List<String> getDistinctCommits() {
        return results.stream()
                .filter(r -> r.getCommitInfo() != null)
                .map(r -> r.getCommitInfo().getShortHash())
                .distinct()
                .collect(Collectors.toList());
    }
}