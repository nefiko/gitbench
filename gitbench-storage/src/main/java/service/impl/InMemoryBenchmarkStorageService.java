package service.impl;

import model.BenchmarkResult;
import model.MethodSignature;
import service.BenchmarkStorageService;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryBenchmarkStorageService implements BenchmarkStorageService {

    private final Map<String, List<BenchmarkResult>> resultsByProject = new HashMap<>();

    @Override
    public void saveResults(List<BenchmarkResult> results, String projectName) {
        resultsByProject.computeIfAbsent(projectName, k -> new ArrayList<>()).addAll(results);
    }

    @Override
    public List<BenchmarkResult> getResultsForCommit(String commitHash, String projectName) {
        return getProjectResults(projectName).stream()
                .filter(r -> r.getCommitInfo() != null && commitHash.equals(r.getCommitInfo().getHash()))
                .collect(Collectors.toList());
    }

    @Override
    public List<BenchmarkResult> getResultsForMethod(MethodSignature method, String projectName) {
        return getProjectResults(projectName).stream()
                .filter(r -> matchesMethod(r, method))
                .sorted(Comparator.comparing(BenchmarkResult::getRunTimestamp).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<BenchmarkResult> getLatestResultForMethod(MethodSignature method, String projectName) {
        return getProjectResults(projectName).stream()
                .filter(r -> matchesMethod(r, method))
                .max(Comparator.comparing(BenchmarkResult::getRunTimestamp));
    }

    @Override
    public List<BenchmarkResult> getHistory(String projectName, int limit) {
        return getProjectResults(projectName).stream()
                .sorted(Comparator.comparing(BenchmarkResult::getRunTimestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<BenchmarkResult> getBaseline(MethodSignature method, String parentCommitHash, String projectName) {
        return getProjectResults(projectName).stream()
                .filter(r -> matchesMethod(r, method))
                .filter(r -> r.getCommitInfo() != null && parentCommitHash.equals(r.getCommitInfo().getHash()))
                .filter(BenchmarkResult::isSuccessful)
                .max(Comparator.comparing(BenchmarkResult::getRunTimestamp));
    }

    public void clear() {
        resultsByProject.clear();
    }

    public void clear(String projectName) {
        resultsByProject.remove(projectName);
    }

    private List<BenchmarkResult> getProjectResults(String projectName) {
        return resultsByProject.getOrDefault(projectName, Collections.emptyList());
    }

    private boolean matchesMethod(BenchmarkResult result, MethodSignature method) {
        if (result.getMethodSignature() == null) {
            return false;
        }
        return Objects.equals(result.getMethodSignature().getClassName(), method.getClassName())
                && Objects.equals(result.getMethodSignature().getMethodName(), method.getMethodName());
    }
}