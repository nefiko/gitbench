package cli.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;

@Command(
        name = "scan",
        description = "Scans project for benchmark candidates"
)
public class ScanCommand implements Runnable {

    @Parameters(index = "0", defaultValue = ".", description = "Project path")
    private Path projectPath;

    @Option(names = {"-v", "--verbose"}, description = "Show detailed output")
    private boolean verbose;

    @Option(names = {"--include"}, description = "Packages to include")
    private String[] includePackages;

    @Option(names = {"--exclude"}, description = "Packages to exclude")
    private String[] excludePackages;

    @Override
    public void run() {
        System.out.println("Scanning project: " + projectPath.toAbsolutePath());

        if (includePackages != null) {
            System.out.println("Including packages: " + String.join(", ", includePackages));
        }

        if (excludePackages != null) {
            System.out.println("Excluding packages: " + String.join(", ", excludePackages));
        }
    }
}
