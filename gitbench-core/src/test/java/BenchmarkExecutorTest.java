
import config.BenchmarkConfiguration;
import config.ConfigurationLoader;
import executor.BenchmarkExecutor;
import model.CommitInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BenchmarkExecutorTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() throws Exception {
        Files.createDirectories(tempDir.resolve(".gitbench"));
    }

    @Test
    void execute_throwsException_whenNoConfig() {
        BenchmarkExecutor executor = new BenchmarkExecutor(tempDir);
        CommitInfo commit = createCommit();

        assertThatThrownBy(() -> executor.execute(commit))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No benchmarks configured");
    }

    @Test
    void execute_throwsException_whenConfigEmpty() throws Exception {
        createEmptyConfig();
        BenchmarkExecutor executor = new BenchmarkExecutor(tempDir);
        CommitInfo commit = createCommit();

        assertThatThrownBy(() -> executor.execute(commit))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No benchmarks configured");
    }

    @Test
    void execute_createsWorkDirectory() throws Exception {
        createConfigWithMethod();
        BenchmarkExecutor executor = new BenchmarkExecutor(tempDir);
        CommitInfo commit = createCommit();

        try {
            executor.execute(commit);
        } catch (Exception _) {
        }

        assertThat(Files.exists(tempDir.resolve(".gitbench/work"))).isTrue();
    }

    @Test
    void cleanup_removesWorkDirectory() throws Exception {
        Path workDir = tempDir.resolve(".gitbench/work");
        Files.createDirectories(workDir);
        Files.writeString(workDir.resolve("test.txt"), "content");

        BenchmarkExecutor executor = new BenchmarkExecutor(tempDir);
        executor.cleanup();

        assertThat(Files.exists(workDir)).isFalse();
    }

    @Test
    void cleanup_doesNothing_whenWorkDirNotExists() throws Exception {
        BenchmarkExecutor executor = new BenchmarkExecutor(tempDir);

        executor.cleanup();

        assertThat(Files.exists(tempDir.resolve(".gitbench/work"))).isFalse();
    }

    private void createEmptyConfig() throws Exception {
        ConfigurationLoader loader = new ConfigurationLoader();
        BenchmarkConfiguration config = new BenchmarkConfiguration();
        loader.saveBenchmarkConfig(tempDir, config);
    }

    private void createConfigWithMethod() throws Exception {
        ConfigurationLoader loader = new ConfigurationLoader();
        BenchmarkConfiguration config = new BenchmarkConfiguration();

        BenchmarkConfiguration.BenchmarkMethodConfiguration method =
                new BenchmarkConfiguration.BenchmarkMethodConfiguration();
        method.setClassName("com.example.TestClass");
        method.setMethodName("testMethod");
        method.setParams(List.of());

        config.getBenchmarks().add(method);
        loader.saveBenchmarkConfig(tempDir, config);
    }

    private CommitInfo createCommit() {
        CommitInfo commit = new CommitInfo();
        commit.setHash("abc123def456");
        commit.setMessage("Test commit");
        commit.setAuthorName("Test Author");
        return commit;
    }
}