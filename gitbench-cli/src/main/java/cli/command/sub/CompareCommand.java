package cli.command.sub;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "compare",
        description = "Compare benchmarks between two commits",
        mixinStandardHelpOptions = true
)
public class CompareCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Base commit reference")
    private String baseCommit;

    @Parameters(index = "1", defaultValue = "HEAD", description = "Target commit reference")
    private String targetCommit;

    @Option(names = {"-p", "--project"}, defaultValue = ".", description = "Project path")
    private Path projectPath;

    @Option(names = {"--method"}, description = "Filter by method name")
    private String methodFilter;

    @Option(names = {"--threshold"}, defaultValue = "5", description = "Regression threshold percentage")
    private double threshold;

    @Override
    public Integer call() {
        return 0;
    }
}