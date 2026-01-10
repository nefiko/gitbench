package cli.command.sub;

import git.GitService;
import model.BenchmarkResult;
import model.CommitInfo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import service.impl.FileBenchmarkStorageService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(
        name = "history",
        description = "Show benchmark history and compare commits",
        mixinStandardHelpOptions = true
)
public class HistoryCommand implements Callable<Integer> {

    @Option(names = {"-p", "--project"}, defaultValue = ".", description = "Project path")
    private Path projectPath;

    @Option(names = {"-n", "--last"}, defaultValue = "5", description = "Number of recent commits to show")
    private int lastN;

    @Option(names = {"--from"}, description = "Start commit reference")
    private String fromCommit;

    @Option(names = {"--to"}, defaultValue = "HEAD", description = "End commit reference")
    private String toCommit;

    @Option(names = {"--method"}, description = "Filter by method name")
    private String methodFilter;

    @Override
    public Integer call() {
        Path absolutePath = projectPath.toAbsolutePath();

        if (!Files.exists(absolutePath.resolve(".git"))) {
            System.err.println("Error: Not a Git repository.");
            return 1;
        }

        try (GitService gitService = new GitService(absolutePath)) {
            List<CommitInfo> commits;

            if (fromCommit != null) {
                commits = gitService.getCommitsBetween(fromCommit, toCommit);
                System.out.println("Commits from " + fromCommit + " to " + toCommit);
            } else {
                commits = gitService.getCommitHistory("HEAD", lastN);
                System.out.println("Last " + lastN + " commits");
            }

            if (methodFilter != null) {
                System.out.println("Filtering by method: " + methodFilter);
            }

            System.out.println();

            String projectName = absolutePath.getFileName().toString();
            FileBenchmarkStorageService storage = new FileBenchmarkStorageService(absolutePath);
            List<BenchmarkResult> allResults = storage.getHistory(projectName, 1000);

            Map<String, List<BenchmarkResult>> resultsByCommit = allResults.stream()
                    .filter(r -> r.getCommitInfo() != null)
                    .filter(r -> methodFilter == null || matchesFilter(r, methodFilter))
                    .collect(Collectors.groupingBy(r -> r.getCommitInfo().getShortHash()));

            System.out.println("Commit      Author          Avg Time    Change");
            System.out.println("----------- --------------- ----------- ----------------");

            java.util.Collections.reverse(commits);

            CommitInfo previousCommit = null;
            for (CommitInfo commit : commits) {
                String shortHash = commit.getShortHash();
                String author = truncate(commit.getAuthorName(), 15);

                List<BenchmarkResult> commitResults = resultsByCommit.get(shortHash);

                if (commitResults != null && !commitResults.isEmpty()) {
                    double avgTime = commitResults.stream()
                            .filter(BenchmarkResult::isSuccessful)
                            .filter(r -> r.getExecutionStats() != null)
                            .mapToDouble(r -> r.getExecutionStats().getAverageTime())
                            .average()
                            .orElse(0.0);

                    String change = calculateChange(commit, previousCommit, resultsByCommit);
                    System.out.printf("%-11s %-15s %-11.3f %s%n", shortHash, author, avgTime, change);
                } else {
                    System.out.printf("%-11s %-15s %-11s %s%n", shortHash, author, "-", "(no data)");
                }

                previousCommit = commit;
            }

            return 0;
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

    private String calculateChange(CommitInfo current, CommitInfo previous, Map<String, List<BenchmarkResult>> resultsByCommit) {
        if (previous == null) {
            return "-";
        }

        List<BenchmarkResult> currentResults = resultsByCommit.get(current.getShortHash());
        List<BenchmarkResult> previousResults = resultsByCommit.get(previous.getShortHash());

        if (currentResults == null || previousResults == null) {
            return "-";
        }

        double currentAvg = currentResults.stream()
                .filter(BenchmarkResult::isSuccessful)
                .filter(r -> r.getExecutionStats() != null)
                .mapToDouble(r -> r.getExecutionStats().getAverageTime())
                .average()
                .orElse(0.0);

        double previousAvg = previousResults.stream()
                .filter(BenchmarkResult::isSuccessful)
                .filter(r -> r.getExecutionStats() != null)
                .mapToDouble(r -> r.getExecutionStats().getAverageTime())
                .average()
                .orElse(0.0);

        if (previousAvg == 0) {
            return "-";
        }

        double changePercent = ((currentAvg - previousAvg) / previousAvg) * 100;

        if (changePercent > 5) {
            return String.format("+%.1f%% (regression)", changePercent);
        } else if (changePercent < -5) {
            return String.format("%.1f%% (improvement)", changePercent);
        } else {
            return String.format("%.1f%%", changePercent);
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}