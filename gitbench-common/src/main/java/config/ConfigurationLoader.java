package config;

import java.io.IOException;
import java.nio.file.Path;

public class ConfigurationLoader {

    public BenchmarkConfiguration loadBenchmarkConfig(Path projectPath) throws IOException {
        return null;
    }

    public void saveBenchmarkConfig(Path projectPath, BenchmarkConfiguration config) throws IOException {
        return;
    }

    public boolean configExists(Path projectPath) {
        return false;
    }
}