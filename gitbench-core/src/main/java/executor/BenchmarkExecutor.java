package executor;

import compiler.BenchmarkCompiler;
import config.BenchmarkConfiguration;
import config.ConfigurationLoader;
import generator.BenchmarkGenerator;
import generator.BenchmarkGenerator.GeneratedBenchmark;
import model.BenchmarkResult;
import model.CommitInfo;
import runner.JmhRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class BenchmarkExecutor {

    private final Path projectPath;
    private final Path workDir;

    public BenchmarkExecutor(Path projectPath) {
        this.projectPath = projectPath;
        this.workDir = projectPath.resolve(".gitbench").resolve("work");
    }

    public List<BenchmarkResult> execute(CommitInfo commit) throws IOException {
        ConfigurationLoader loader = new ConfigurationLoader();
        BenchmarkConfiguration config = loader.loadBenchmarkConfig(projectPath);

        if (config.getBenchmarks().isEmpty()) {
            throw new RuntimeException("No benchmarks configured. Run 'gitbench scan --save' first.");
        }

        Files.createDirectories(workDir);

        System.out.println("Generating benchmark code...");
        BenchmarkGenerator generator = new BenchmarkGenerator();
        List<GeneratedBenchmark> generated = generator.generate(config);

        System.out.println("Compiling benchmarks...");
        BenchmarkCompiler compiler = new BenchmarkCompiler(workDir);
        String classpath = buildClasspath();
        compiler.compile(generated, classpath);

        System.out.println("Running benchmarks...");
        List<String> benchmarkClasses = generated.stream()
                .map(GeneratedBenchmark::className)
                .collect(Collectors.toList());

        JmhRunner runner = new JmhRunner();
        return runner.run(benchmarkClasses, commit);
    }

    private String buildClasspath() {
        String separator = System.getProperty("path.separator");
        StringBuilder cp = new StringBuilder();

        cp.append(projectPath.resolve("target/classes"));
        cp.append(separator);
        cp.append(workDir.resolve("classes"));
        cp.append(separator);
        cp.append(System.getProperty("java.class.path"));

        return cp.toString();
    }

    public void cleanup() throws IOException {
        if (Files.exists(workDir)) {
            Files.walk(workDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                        }
                    });
        }
    }
}
