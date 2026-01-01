package cli.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;

@Command(
        name = "init",
        description = "Initialize project for GitBench tracking"
)
public class InitCommand implements Runnable {

    @Parameters(index = "0", defaultValue = ".", description = "Project path")
    private Path projectPath;

    @Option(names = {"--name"}, description = "Project name (defaults to directory name)")
    private String projectName;

    @Option(names = {"--force"}, description = "Reinitialize if already exists")
    private boolean force;

    @Override
    public void run() {
        Path absolutePath = projectPath.toAbsolutePath();
        String name = projectName != null ? projectName : absolutePath.getFileName().toString();
        
        System.out.println("Initializing GitBench for project: " + name);
        System.out.println("Path: " + absolutePath);
        
        if (force) {
            System.out.println("Force reinitializing...");
        }
        
        System.out.println("Project initialized successfully.");
    }
}
