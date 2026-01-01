package cli.command;

import git.GitService;
import model.CommitInfo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "history",
        description = "Show benchmark history and compare commits"
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
                System.out.println("Comparing " + fromCommit + " to " + toCommit);
            } else {
                commits = gitService.getCommitHistory("HEAD", lastN);
                System.out.println("Showing last " + lastN + " commits");
            }

            if (methodFilter != null) {
                System.out.println("Filtering by method: " + methodFilter);
            }

            System.out.println();
            System.out.println("Commit      Author          Message");
            System.out.println("----------- --------------- --------------------------------");

            for (CommitInfo commit : commits) {
                String shortHash = commit.getShortHash();
                String author = truncate(commit.getAuthorName(), 15);
                String message = truncate(commit.getMessage().split("\n")[0], 32);
                System.out.printf("%-11s %-15s %s%n", shortHash, author, message);
            }

            System.out.println();
            System.out.println("Benchmark data will be shown when available...");
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}