package service;

import model.BenchmarkResult;
import model.MethodSignature;

import java.util.List;
import java.util.Optional;

public interface BenchmarkStorageService {

    void saveResults(List<BenchmarkResult> results, String projectName);

    List<BenchmarkResult> getResultsForCommit(String commitHash, String projectName);

    List<BenchmarkResult> getResultsForMethod(MethodSignature method, String projectName);

    Optional<BenchmarkResult> getLatestResultForMethod(MethodSignature method, String projectName);

    List<BenchmarkResult> getHistory(String projectName, int limit);

    Optional<BenchmarkResult> getBaseline(MethodSignature method, String parentCommitHash, String projectName);

}