package config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigurationLoader {

    private static final String CONFIG_DIR = ".gitbench";
    private static final String BENCHMARKS_FILE = "benchmarks.yml";

    private final ObjectMapper yamlMapper;

    public ConfigurationLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public BenchmarkConfiguration loadBenchmarkConfig(Path projectPath) throws IOException {
        Path configFile = projectPath.resolve(CONFIG_DIR).resolve(BENCHMARKS_FILE);

        if (!Files.exists(configFile)) {
            return new BenchmarkConfiguration();
        }

        return yamlMapper.readValue(configFile.toFile(), BenchmarkConfiguration.class);
    }

    public void saveBenchmarkConfiguration(Path projectPath, BenchmarkConfiguration config) throws IOException {
        Path configDir = projectPath.resolve(CONFIG_DIR);
        Files.createDirectories(configDir);

        Path configFile = configDir.resolve(BENCHMARKS_FILE);
        yamlMapper.writeValue(configFile.toFile(), config);
    }

    public boolean configExists(Path projectPath) {
        Path configFile = projectPath.resolve(CONFIG_DIR).resolve(BENCHMARKS_FILE);
        return Files.exists(configFile);
    }
}