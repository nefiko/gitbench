package cli.command;

import picocli.CommandLine;

@CommandLine.Command(
        name = "gitbench",
        description = "Starts up the GitBench CLI interface"
)
public class GitBenchCommand implements Runnable {

    static void main(String[] args) {
        int exitCode = new CommandLine(new GitBenchCommand()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        System.out.println("Starting GitBench CLI interface...");
    }
}
