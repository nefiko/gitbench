package cli.command;

import cli.command.sub.ScanCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ScanCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void scan_returnsZero_whenPathExists() {
        CommandLine cmd = new CommandLine(new ScanCommand());

        int exitCode = cmd.execute(tempDir.toString());

        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void scan_returnsOne_whenPathNotExists() {
        CommandLine cmd = new CommandLine(new ScanCommand());

        int exitCode = cmd.execute("/nonexistent/path/12345");

        assertThat(exitCode).isEqualTo(1);
    }

    @Test
    void scan_acceptsLimitOption() {
        CommandLine cmd = new CommandLine(new ScanCommand());

        int exitCode = cmd.execute(tempDir.toString(), "--limit", "10");

        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void scan_acceptsVerboseOption() {
        CommandLine cmd = new CommandLine(new ScanCommand());

        int exitCode = cmd.execute(tempDir.toString(), "--verbose");

        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void scan_acceptsIncludeOption() {
        CommandLine cmd = new CommandLine(new ScanCommand());

        int exitCode = cmd.execute(tempDir.toString(), "--include", "com.example");

        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void scan_acceptsExcludeOption() {
        CommandLine cmd = new CommandLine(new ScanCommand());

        int exitCode = cmd.execute(tempDir.toString(), "--exclude", "com.test");

        assertThat(exitCode).isEqualTo(0);
    }
}
