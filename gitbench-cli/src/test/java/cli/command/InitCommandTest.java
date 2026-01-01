package cli.command;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InitCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void init_failsWhenNotGitRepo() {
        CommandLine cmd = new CommandLine(new InitCommand());
        int exitCode = cmd.execute(tempDir.toString());

        assertThat(exitCode).isEqualTo(1);
    }

    @Test
    void init_succeedsForGitRepo() {
        CommandLine cmd = new CommandLine(new InitCommand());
        int exitCode = cmd.execute(tempDir.toString());

        assertThat(exitCode).isEqualTo(0);
    }
}
