package cli.command;

import cli.command.sub.CompareCommand;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CompareCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void compare_returnsOne_whenNotGitRepo() {
        CommandLine cmd = new CommandLine(new CompareCommand());

        int exitCode = cmd.execute("HEAD~1", "HEAD", "--project", tempDir.toString());

        assertThat(exitCode).isEqualTo(1);
    }

    @Test
    void compare_returnsZero_whenValidCommits() throws Exception {
        initGitRepoWithCommits();

        CommandLine cmd = new CommandLine(new CompareCommand());
        int exitCode = cmd.execute("HEAD~1", "HEAD", "--project", tempDir.toString());

        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void compare_acceptsThresholdOption() throws Exception {
        initGitRepoWithCommits();

        CommandLine cmd = new CommandLine(new CompareCommand());
        int exitCode = cmd.execute("HEAD~1", "HEAD", "--project", tempDir.toString(), "--threshold", "10");

        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void compare_acceptsMethodFilter() throws Exception {
        initGitRepoWithCommits();

        CommandLine cmd = new CommandLine(new CompareCommand());
        int exitCode = cmd.execute("HEAD~1", "HEAD", "--project", tempDir.toString(), "--method", "process");

        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void compare_defaultsTargetToHead() throws Exception {
        initGitRepoWithCommits();

        CommandLine cmd = new CommandLine(new CompareCommand());
        int exitCode = cmd.execute("HEAD~1", "--project", tempDir.toString());

        assertThat(exitCode).isEqualTo(0);
    }

    private void initGitRepoWithCommits() throws Exception {
        Git git = Git.init().setDirectory(tempDir.toFile()).call();

        Files.writeString(tempDir.resolve("file1.txt"), "content1");
        git.add().addFilepattern(".").call();
        git.commit().setMessage("First commit").call();

        Files.writeString(tempDir.resolve("file2.txt"), "content2");
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Second commit").call();

        git.close();
    }
}