package cli.command;

import cli.command.sub.StatusCommand;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StatusCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void status_returnsOne_whenNotGitRepo() {
        CommandLine cmd = new CommandLine(new StatusCommand());

        int exitCode = cmd.execute("--project", tempDir.toString());

        assertThat(exitCode).isEqualTo(1);
    }

    @Test
    void status_returnsZero_whenGitRepo() throws Exception {
        initGitRepo();

        CommandLine cmd = new CommandLine(new StatusCommand());
        int exitCode = cmd.execute("--project", tempDir.toString());

        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void status_defaultsToCurrentDirectory() {
        CommandLine cmd = new CommandLine(new StatusCommand());

        int exitCode = cmd.execute();

        assertThat(exitCode).isEqualTo(1);
    }

    private void initGitRepo() throws Exception {
        Git git = Git.init().setDirectory(tempDir.toFile()).call();
        Files.writeString(tempDir.resolve("README.md"), "# Test");
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Initial commit").call();
        git.close();
    }
}
