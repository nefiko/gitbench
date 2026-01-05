package runner;

import model.BenchmarkResult;
import model.CommitInfo;
import model.MethodSignature;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class JmhRunner {

    public List<BenchmarkResult> run(List<String> benchmarkClasses, CommitInfo commit) {
        List<BenchmarkResult> results = new ArrayList<>();

        for (String benchmarkClass : benchmarkClasses) {
            BenchmarkResult result = runOne(benchmarkClass, commit);
            results.add(result);
        }

        return results;
    }

    private BenchmarkResult runOne(String benchmarkClass, CommitInfo commit) {
        try {
            Options options = new OptionsBuilder()
                    .include(benchmarkClass)
                    .shouldFailOnError(true)
                    .build();

            Collection<RunResult> runResults = new Runner(options).run();

            if (runResults.isEmpty()) {
                return createFailedResult(benchmarkClass, commit, "No results returned");
            }

            RunResult runResult = runResults.iterator().next();
            return parseResult(runResult, benchmarkClass, commit);

        } catch (RunnerException e) {
            return createFailedResult(benchmarkClass, commit, e.getMessage());
        } catch (RuntimeException e) {
            return createFailedResult(benchmarkClass, commit, e.getMessage());
        }
    }

    private BenchmarkResult parseResult(RunResult runResult, String benchmarkClass, CommitInfo commit) {
        double mean = runResult.getPrimaryResult().getScore();
        double stdDev = runResult.getPrimaryResult().getStatistics().getStandardDeviation();
        double min = runResult.getPrimaryResult().getStatistics().getMin();
        double max = runResult.getPrimaryResult().getStatistics().getMax();

        MethodSignature signature = parseMethodSignature(benchmarkClass);

        BenchmarkResult.ExecutionStats stats = BenchmarkResult.ExecutionStats.builder()
                .averageTime(mean)
                .minTime(min)
                .maxTime(max)
                .standardDeviation(stdDev)
                .build();

        return BenchmarkResult.builder()
                .id(UUID.randomUUID().toString())
                .methodSignature(signature)
                .commitInfo(commit)
                .executionStats(stats)
                .runTimestamp(Instant.now())
                .timeUnit(TimeUnit.MILLISECONDS)
                .mode(BenchmarkResult.BenchmarkMode.AVERAGE_TIME)
                .successful(true)
                .build();
    }

    public MethodSignature parseMethodSignature(String benchmarkClass) {
        String simpleName = benchmarkClass.substring(benchmarkClass.lastIndexOf('.') + 1);
        String withoutSuffix = simpleName.replace("_Benchmark", "");
        int lastUnderscore = withoutSuffix.lastIndexOf('_');

        String className = withoutSuffix.substring(0, lastUnderscore);
        String methodName = withoutSuffix.substring(lastUnderscore + 1);

        MethodSignature signature = new MethodSignature();
        signature.setClassName(className);
        signature.setMethodName(methodName);
        return signature;
    }

    private BenchmarkResult createFailedResult(String benchmarkClass, CommitInfo commit, String error) {
        MethodSignature signature = parseMethodSignature(benchmarkClass);

        return BenchmarkResult.builder()
                .id(UUID.randomUUID().toString())
                .methodSignature(signature)
                .commitInfo(commit)
                .runTimestamp(Instant.now())
                .successful(false)
                .errorMessage(error)
                .build();
    }
}