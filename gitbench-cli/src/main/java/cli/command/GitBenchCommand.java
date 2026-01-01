package cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
        name = "gitbench",
        description = "Starts up the GitBench CLI interface",
        subcommands = {InitCommand.class, ScanCommand.class, RunCommand.class, HistoryCommand.class}
)
public class GitBenchCommand implements Callable<Integer> {

    static void main(String[] args) {
        int exitCode = new CommandLine(new GitBenchCommand()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }
}