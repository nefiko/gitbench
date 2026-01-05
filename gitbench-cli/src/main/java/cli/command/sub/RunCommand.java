package cli.command.sub;

import executor.BenchmarkExecutor;
import git.GitService;
import model.BenchmarkResult;
import model.CommitInfo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "run",
        description = "Run benchmarks on specified commit",
        mixinStandardHelpOptions = true
)
public class RunCommand implements Callable<Integer> {

    @Parameters(index = "0", defaultValue = "HEAD", description = "Commit reference (hash, branch, tag, HEAD)")
    private String commitRef;

    @Option(names = {"-p", "--project"}, defaultValue = ".", description = "Project path")
    private Path projectPath;

    @Override
    public Integer call() {
        Path absolutePath = projectPath.toAbsolutePath();

        if (!Files.exists(absolutePath.resolve(".git"))) {
            System.err.println("Error: Not a Git repository.");
            return 1;
        }

        if (!Files.exists(absolutePath.resolve(".gitbench/benchmarks.yml"))) {
            System.err.println("Error: No benchmarks.yml found. Run 'gitbench scan --save' first.");
            return 1;
        }

        try (GitService gitService = new GitService(absolutePath)) {
            CommitInfo commit = gitService.getCommitInfo(commitRef);

            System.out.println("Running benchmarks");
            System.out.println("==================");
            System.out.println("Commit: " + commit.getShortHash() + " - " + truncate(commit.getMessage(), 50));
            System.out.println();

            BenchmarkExecutor executor = new BenchmarkExecutor(absolutePath);
            List<BenchmarkResult> results = executor.execute(commit);

            System.out.println();
            System.out.println("Results:");
            System.out.println("--------");
            printResults(results);

            executor.cleanup();

            return 0;
        } catch (Exception e) {
            System.err.println("Error running benchmarks: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    private void printResults(List<BenchmarkResult> results) {
        System.out.printf("%-40s %-12s %-12s%n", "Method", "Avg (ms)", "Std Dev");
        System.out.println("-".repeat(66));

        for (BenchmarkResult result : results) {
            String methodName = result.getMethodSignature().getClassName() + "#" + result.getMethodSignature().getMethodName();
            methodName = truncate(methodName, 40);

            if (result.isSuccessful()) {
                System.out.printf("%-40s %-12.3f %-12.3f%n",
                        methodName,
                        result.getExecutionStats().getAverageTime(),
                        result.getExecutionStats().getStandardDeviation());
            } else {
                System.out.printf("%-40s FAILED: %s%n", methodName, result.getErrorMessage());
            }
        }
    }

    private String truncate(String text, int maxLength) {
        String firstLine = text.split("\n")[0];
        if (firstLine.length() <= maxLength) {
            return firstLine;
        }
        return firstLine.substring(0, maxLength - 3) + "...";
    }
}