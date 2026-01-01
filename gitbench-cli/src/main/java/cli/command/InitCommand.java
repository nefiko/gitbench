package cli.command;

import git.GitService;
import model.CommitInfo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "init",
        description = "Initialize project for GitBench tracking"
)
public class InitCommand implements Callable<Integer> {

    @Parameters(index = "0", defaultValue = ".", description = "Project path")
    private Path projectPath;

    @Option(names = {"--name"}, description = "Project name (defaults to directory name)")
    private String projectName;

    @Option(names = {"--force"}, description = "Reinitialize if already exists")
    private boolean force;

    @Override
    public Integer call() {
        Path absolutePath = projectPath.toAbsolutePath();
        String name = projectName != null ? projectName : absolutePath.getFileName().toString();

        System.out.println("Initializing GitBench for project: " + name);
        System.out.println("Path: " + absolutePath);

        if (!Files.exists(absolutePath.resolve(".git"))) {
            System.err.println("Error: Not a Git repository. Please run 'git init' first.");
            return 1;
        }

        try (GitService gitService = new GitService(absolutePath)) {
            CommitInfo currentCommit = gitService.getCurrentCommit();
            String branch = gitService.getCurrentBranch();

            System.out.println();
            System.out.println("Git repository detected:");
            System.out.println("Branch: " + branch);
            System.out.println("Current commit: " + currentCommit.getShortHash());
            System.out.println();
            System.out.println("Project initialized successfully.");
            System.out.println("Run 'gitbench scan' to find benchmark candidates.");
            return 0;
        } catch (Exception e) {
            System.err.println("Error initializing project: " + e.getMessage());
            return 1;
        }
    }
}