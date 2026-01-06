package cli.command.sub;

import git.GitService;
import model.BenchmarkResult;
import model.CommitInfo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import service.BenchmarkStorageService;
import service.StorageServiceFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "status",
        description = "Show project status and recent benchmark info",
        mixinStandardHelpOptions = true
)
public class StatusCommand implements Callable<Integer> {

    @Option(names = {"-p", "--project"}, defaultValue = ".", description = "Project path")
    private Path projectPath;

    @Override
    public Integer call() {
        Path absolutePath = projectPath.toAbsolutePath();

        if (!Files.exists(absolutePath.resolve(".git"))) {
            System.err.println("Error: Not a Git repository.");
            return 1;
        }

        try (GitService gitService = new GitService(absolutePath)) {
            String branch = gitService.getCurrentBranch();
            CommitInfo currentCommit = gitService.getCurrentCommit();
            boolean hasChanges = gitService.hasUncommittedChanges();

            String projectName = absolutePath.getFileName().toString();

            System.out.println("GitBench Status");
            System.out.println("===============");
            System.out.println();
            System.out.println("Project: " + projectName);
            System.out.println("Path:    " + absolutePath);
            System.out.println();
            System.out.println("Git:");
            System.out.println("  Branch: " + branch);
            System.out.println("  Commit: " + currentCommit.getShortHash() + " - " + truncate(currentCommit.getMessage(), 40));
            System.out.println("  Status: " + (hasChanges ? "uncommitted changes" : "clean"));
            System.out.println();

            boolean hasConfig = Files.exists(absolutePath.resolve(".gitbench/benchmarks.yml"));
            System.out.println("Configuration:");
            System.out.println("  benchmarks.yml: " + (hasConfig ? "found" : "not found"));
            System.out.println();

            BenchmarkStorageService storage = StorageServiceFactory.createDefault();
            List<BenchmarkResult> history = storage.getHistory(projectName, 10);

            System.out.println("Benchmarks:");
            if (history.isEmpty()) {
                System.out.println("  No benchmark data yet.");
                System.out.println("  Run 'gitbench run --persist' to create benchmarks.");
            } else {
                long successful = history.stream().filter(BenchmarkResult::isSuccessful).count();
                long failed = history.size() - successful;

                System.out.println("  Results in storage: " + history.size());
                System.out.println("  Successful: " + successful);
                System.out.println("  Failed: " + failed);

                BenchmarkResult latest = history.get(0);
                if (latest.getCommitInfo() != null) {
                    System.out.println("  Latest run: " + latest.getCommitInfo().getShortHash() +
                            " at " + latest.getRunTimestamp());
                }
            }

            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
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