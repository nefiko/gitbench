package executor;

import compiler.BenchmarkCompiler;
import config.BenchmarkConfiguration;
import config.ConfigurationLoader;
import generator.BenchmarkGenerator;
import generator.BenchmarkGenerator.GeneratedBenchmark;
import model.BenchmarkResult;
import model.CommitInfo;
import runner.JmhRunner;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        System.out.println("Compiling project sources...");
        compileProjectSources();

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

        JmhRunner runner = new JmhRunner(compiler.getClassDir(), classpath);
        return runner.run(benchmarkClasses, commit);
    }

    private void compileProjectSources() throws IOException {
        Path sourceDir = projectPath.resolve("src/main/java");
        Path targetDir = projectPath.resolve("target/classes");

        if (!Files.exists(sourceDir)) {
            System.out.println("  No src/main/java found, skipping project compilation.");
            return;
        }

        Files.createDirectories(targetDir);

        List<Path> javaFiles;
        try (var stream = Files.walk(sourceDir)) {
            javaFiles = stream
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
        }

        if (javaFiles.isEmpty()) {
            System.out.println("  No Java files found.");
            return;
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("No Java compiler available. Run with JDK, not JRE.");
        }

        String[] args = new String[javaFiles.size() + 4];
        args[0] = "-d";
        args[1] = targetDir.toString();
        args[2] = "-sourcepath";
        args[3] = sourceDir.toString();
        for (int i = 0; i < javaFiles.size(); i++) {
            args[i + 4] = javaFiles.get(i).toString();
        }

        int result = compiler.run(null, null, null, args);
        if (result != 0) {
            throw new RuntimeException("Failed to compile project sources.");
        }

        System.out.println("  Compiled " + javaFiles.size() + " source files.");
    }

    private String buildClasspath() {
        String separator = File.pathSeparator;

        return projectPath.resolve("target/classes") +
                separator +
                workDir.resolve("classes") +
                separator +
                System.getProperty("java.class.path");
    }

    public void cleanup() throws IOException {
        if (Files.exists(workDir)) {
            try (Stream<Path> paths = Files.walk(workDir)) {
                paths
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException _) {
                            }
                        });
            }
        }
    }

}