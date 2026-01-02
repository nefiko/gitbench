package cli.command.sub;

import config.BenchmarkConfiguration;
import config.ConfigurationLoader;
import config.ScannerConfiguration;
import model.HotspotCandidate;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import scanner.impl.StaticCodeScanner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "scan",
        description = "Scan project for benchmark candidates",
        mixinStandardHelpOptions = true
)
public class ScanCommand implements Callable<Integer> {

    @Parameters(index = "0", defaultValue = ".", description = "Project path")
    private Path projectPath;

    @Option(names = {"-v", "--verbose"}, description = "Show detailed output")
    private boolean verbose;

    @Option(names = {"--include"}, description = "Packages to include")
    private String[] includePackages;

    @Option(names = {"--exclude"}, description = "Packages to exclude")
    private String[] excludePackages;

    @Option(names = {"-n", "--limit"}, defaultValue = "20", description = "Maximum methods to show")
    private int limit;

    @Option(names = {"--save"}, description = "Save candidates to .gitbench/benchmarks.yml")
    private boolean save;

    @Override
    public Integer call() {
        Path absolutePath = projectPath.toAbsolutePath();

        if (!Files.exists(absolutePath)) {
            System.err.println("Error: Path does not exist: " + absolutePath);
            return 1;
        }

        System.out.println("Scanning project: " + absolutePath);

        try {
            ScannerConfiguration config = new ScannerConfiguration();
            if (includePackages != null) {
                config.setIncludePackages(Arrays.asList(includePackages));
            }
            if (excludePackages != null) {
                config.setExcludePackages(Arrays.asList(excludePackages));
            }
            config.setMaxMethods(limit);

            StaticCodeScanner scanner = new StaticCodeScanner();
            List<HotspotCandidate> candidates = scanner.scan(absolutePath, config);

            System.out.println();
            System.out.println("Found " + candidates.size() + " benchmark candidates:");
            System.out.println();

            for (HotspotCandidate candidate : candidates) {
                String signature = candidate.getMethodSignature().getClassName()
                        + "#" + candidate.getMethodSignature().getMethodName();
                System.out.println("  " + signature);

                if (verbose) {
                    System.out.println("    Priority: " + candidate.getPriorityScore());
                    System.out.println("    Reasons: " + candidate.getReasons());
                }
            }

            if (save && !candidates.isEmpty()) {
                saveToBenchmarksYml(absolutePath, candidates);
            } else if (!save && !candidates.isEmpty()) {
                System.out.println();
                System.out.println("Run 'gitbench scan --save' to save these candidates to .gitbench/benchmarks.yml");
            }

            return 0;
        } catch (Exception e) {
            System.err.println("Error scanning project: " + e.getMessage());
            return 1;
        }
    }

    private void saveToBenchmarksYml(Path projectPath, List<HotspotCandidate> candidates) {
        try {
            ConfigurationLoader loader = new ConfigurationLoader();
            BenchmarkConfiguration config = new BenchmarkConfiguration();

            for (HotspotCandidate candidate : candidates) {
                BenchmarkConfiguration.BenchmarkMethodConfiguration methodConfig = new BenchmarkConfiguration.BenchmarkMethodConfiguration();
                methodConfig.setClassName(candidate.getMethodSignature().getClassName());
                methodConfig.setMethodName(candidate.getMethodSignature().getMethodName());
                methodConfig.setSetup(""); // Todo
                methodConfig.setParams(List.of("")); // Todo
                config.getBenchmarks().add(methodConfig);
            }

            loader.saveBenchmarkConfig(projectPath, config);

            System.out.println();
            System.out.println("Saved to .gitbench/benchmarks.yml");
            System.out.println("Edit the file to provide setup code and parameters for each method.");
        } catch (Exception e) {
            System.err.println("Error saving config: " + e.getMessage());
        }
    }
}