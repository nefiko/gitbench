package cli.command.sub;

import git.GitService;
import model.BenchmarkResult;
import model.CommitInfo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import service.impl.FileBenchmarkStorageService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(
        name = "compare",
        description = "Compare benchmarks between two commits",
        mixinStandardHelpOptions = true
)
public class CompareCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Base commit reference")
    private String baseCommit;

    @Parameters(index = "1", defaultValue = "HEAD", description = "Target commit reference")
    private String targetCommit;

    @Option(names = {"-p", "--project"}, defaultValue = ".", description = "Project path")
    private Path projectPath;

    @Option(names = {"--method"}, description = "Filter by method name")
    private String methodFilter;

    @Option(names = {"--threshold"}, defaultValue = "5", description = "Regression threshold percentage")
    private double threshold;

    @Override
    public Integer call() {
        Path absolutePath = projectPath.toAbsolutePath();

        if (!Files.exists(absolutePath.resolve(".git"))) {
            System.err.println("Error: Not a Git repository.");
            return 1;
        }

        try (GitService gitService = new GitService(absolutePath)) {
            CommitInfo base = gitService.getCommitInfo(baseCommit);
            CommitInfo target = gitService.getCommitInfo(targetCommit);

            System.out.println("Comparing benchmarks:");
            System.out.println("  Base:   " + base.getShortHash() + " - " + truncate(base.getMessage(), 40));
            System.out.println("  Target: " + target.getShortHash() + " - " + truncate(target.getMessage(), 40));
            System.out.println();

            if (methodFilter != null) {
                System.out.println("Filtering by method: " + methodFilter);
            }

            System.out.println("Regression threshold: " + threshold + "%");
            System.out.println();

            String projectName = absolutePath.getFileName().toString();
            FileBenchmarkStorageService storage = new FileBenchmarkStorageService(absolutePath);

            List<BenchmarkResult> baseResults = storage.getResultsForCommit(base.getShortHash(), projectName);
            List<BenchmarkResult> targetResults = storage.getResultsForCommit(target.getShortHash(), projectName);

            if (baseResults.isEmpty() && targetResults.isEmpty()) {
                System.out.println("No benchmark data found for either commit.");
                System.out.println("Run 'gitbench run' on both commits first.");
                return 0;
            }

            Map<String, BenchmarkResult> baseByMethod = baseResults.stream()
                    .filter(r -> methodFilter == null || matchesFilter(r, methodFilter))
                    .collect(Collectors.toMap(
                            r -> r.getMethodSignature().getClassName() + "#" + r.getMethodSignature().getMethodName(),
                            r -> r,
                            (a, b) -> a
                    ));

            Map<String, BenchmarkResult> targetByMethod = targetResults.stream()
                    .filter(r -> methodFilter == null || matchesFilter(r, methodFilter))
                    .collect(Collectors.toMap(
                            r -> r.getMethodSignature().getClassName() + "#" + r.getMethodSignature().getMethodName(),
                            r -> r,
                            (a, b) -> a
                    ));

            System.out.println("Method                          Base(ms)    Target(ms)  Change");
            System.out.println("------------------------------- ----------- ----------- ----------------");

            int regressions = 0;
            int improvements = 0;

            for (String methodName : targetByMethod.keySet()) {
                BenchmarkResult targetResult = targetByMethod.get(methodName);
                BenchmarkResult baseResult = baseByMethod.get(methodName);

                String displayName = truncate(methodName, 31);

                if (!targetResult.isSuccessful()) {
                    System.out.printf("%-31s %-11s %-11s %s%n", displayName, "-", "FAILED", targetResult.getErrorMessage());
                    continue;
                }

                double targetTime = targetResult.getExecutionStats().getAverageTime();

                if (baseResult == null || !baseResult.isSuccessful()) {
                    System.out.printf("%-31s %-11s %-11.3f %s%n", displayName, "-", targetTime, "(new)");
                    continue;
                }

                double baseTime = baseResult.getExecutionStats().getAverageTime();
                double changePercent = baseTime == 0 ? 0 : ((targetTime - baseTime) / baseTime) * 100;

                String changeStr;
                if (changePercent > threshold) {
                    changeStr = String.format("+%.1f%% REGRESSION", changePercent);
                    regressions++;
                } else if (changePercent < -threshold) {
                    changeStr = String.format("%.1f%% improvement", changePercent);
                    improvements++;
                } else {
                    changeStr = String.format("%.1f%%", changePercent);
                }

                System.out.printf("%-31s %-11.3f %-11.3f %s%n", displayName, baseTime, targetTime, changeStr);
            }

            System.out.println();
            System.out.println("Summary: " + regressions + " regression(s), " + improvements + " improvement(s)");

            return regressions > 0 ? 1 : 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    private boolean matchesFilter(BenchmarkResult result, String filter) {
        if (result.getMethodSignature() == null) {
            return false;
        }
        String fullName = result.getMethodSignature().getClassName() + "#" + result.getMethodSignature().getMethodName();
        return fullName.toLowerCase().contains(filter.toLowerCase());
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String firstLine = text.split("\n")[0];
        if (firstLine.length() <= maxLength) {
            return firstLine;
        }
        return firstLine.substring(0, maxLength - 3) + "...";
    }
}