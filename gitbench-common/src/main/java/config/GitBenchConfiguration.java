package config;

import lombok.Data;

import java.nio.file.Path;

@Data
public class GitBenchConfiguration {

    private Path projectPath;
    private ScannerConfiguration scanner = new ScannerConfiguration();
    private BenchmarkConfiguration benchmark = new BenchmarkConfiguration();

    public GitBenchConfiguration() {
    }

    public GitBenchConfiguration(Path projectPath) {
        this.projectPath = projectPath;
    }
}