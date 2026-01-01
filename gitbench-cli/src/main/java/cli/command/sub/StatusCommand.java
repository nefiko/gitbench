package cli.command.sub;

import git.GitService;
import model.CommitInfo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
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

            System.out.println("GitBench Status");
            System.out.println("===============");
            System.out.println();
            System.out.println("Project: " + absolutePath.getFileName());
            System.out.println("Path:    " + absolutePath);
            System.out.println();
            System.out.println("Git:");
            System.out.println("  Branch: " + branch);
            System.out.println("  Commit: " + currentCommit.getShortHash() + " - " + truncate(currentCommit.getMessage(), 40));
            System.out.println("  Status: " + (hasChanges ? "uncommitted changes" : "clean"));
            System.out.println();
            System.out.println("Benchmarks:");
            System.out.println("  No benchmark data yet. Run 'gitbench run' to create benchmarks.");

            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
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