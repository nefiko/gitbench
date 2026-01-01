package cli.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;

@Command(
        name = "history",
        description = "Show benchmark history and compare commits"
)
public class HistoryCommand implements Runnable {

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
    public void run() {
        System.out.println("Showing benchmark history for: " + projectPath.toAbsolutePath());

        if (fromCommit != null) {
            System.out.println("Comparing " + fromCommit + " to " + toCommit);
        } else {
            System.out.println("Showing last " + lastN + " commits");
        }

        if (methodFilter != null) {
            System.out.println("Filtering by method: " + methodFilter);
        }
    }
}
