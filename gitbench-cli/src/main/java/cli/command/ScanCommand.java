package cli.command;

import config.ScannerConfiguration;
import model.HotspotCandidate;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import scanner.impl.StaticCodeScanner;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Command(
        name = "scan",
        description = "Scan project for benchmark candidates"
)
public class ScanCommand implements Runnable {

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

    @Override
    public void run() {
        Path absolutePath = projectPath.toAbsolutePath();
        System.out.println("Scanning project: " + absolutePath);

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
                System.out.println("Priority: " + candidate.getPriorityScore());
                System.out.println("Reasons: " + candidate.getReasons());
            }
        }
    }
}
