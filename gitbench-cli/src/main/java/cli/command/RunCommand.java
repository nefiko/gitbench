package cli.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;

@Command(
        name = "run",
        description = "Run benchmarks on specified commit"
)
public class RunCommand implements Runnable {

    @Parameters(index = "0", defaultValue = "HEAD", description = "Commit reference (hash, branch, tag, HEAD)")
    private String commitRef;

    @Option(names = {"-p", "--project"}, defaultValue = ".", description = "Project path")
    private Path projectPath;

    @Option(names = {"-w", "--warmup"}, defaultValue = "3", description = "Warmup iterations")
    private int warmupIterations;

    @Option(names = {"-i", "--iterations"}, defaultValue = "5", description = "Measurement iterations")
    private int measurementIterations;

    @Option(names = {"-f", "--forks"}, defaultValue = "1", description = "Number of forks")
    private int forks;

    @Option(names = {"--method"}, description = "Specific method to benchmark")
    private String methodFilter;

    @Override
    public void run() {
        System.out.println("Running benchmarks on commit: " + commitRef);
        System.out.println("Project: " + projectPath.toAbsolutePath());
        System.out.println("Warmup: " + warmupIterations + ", Iterations: " + measurementIterations + ", Forks: " + forks);

        if (methodFilter != null) {
            System.out.println("Filtering method: " + methodFilter);
        }
    }
}
