package cli.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "run",
        description = "Run benchmarks on specified commit"
)
public class RunCommand implements Callable<Integer> {

    @Parameters(index = "0", defaultValue = "HEAD", description = "Commit reference")
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
    public Integer call() {
        return 0;
    }
}