package scanner.impl;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import config.ScannerConfiguration;
import enums.HotspotReason;
import model.ComplexityMetrics;
import model.HotspotCandidate;
import model.MethodSignature;
import scanner.CodeScanner;
import utils.FileUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StaticCodeScanner implements CodeScanner {

    private static final Set<String> SPRING_STEREOTYPE_ANNOTATIONS = Set.of(
            "Service", "Repository", "Component", "Controller", "RestController"
    );

    private static final Set<String> ENDPOINT_ANNOTATIONS = Set.of(
            "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping", "RequestMapping"
    );

    private final JavaParser parser = new JavaParser();

    @Override
    public List<HotspotCandidate> scan(Path projectPath, ScannerConfiguration config) {
        List<HotspotCandidate> candidates = new ArrayList<>();

        try {
            var javaFiles = FileUtils.findJavaFiles(projectPath.resolve("src"));
            for (Path file : javaFiles) {
                candidates.addAll(scanFile(file, config));
            }
        } catch (Exception e) {
            return candidates;
        }

        candidates = filter(candidates, config);
        candidates.sort((a, b) -> Integer.compare(b.getPriorityScore(), a.getPriorityScore()));

        if (config.getMaxMethods() > 0 && candidates.size() > config.getMaxMethods()) {
            return candidates.subList(0, config.getMaxMethods());
        }

        return candidates;
    }

    @Override
    public List<HotspotCandidate> scanFile(Path filePath, ScannerConfiguration config) {
        List<HotspotCandidate> candidates = new ArrayList<>();

        try {
            var result = parser.parse(filePath);
            if (result.getResult().isEmpty()) {
                return candidates;
            }

            CompilationUnit cu = result.getResult().get();
            String pkg = cu.getPackageDeclaration()
                    .map(NodeWithName::getNameAsString)
                    .orElse("");

            if (!shouldScan(pkg, config)) {
                return candidates;
            }

            for (var type : cu.getTypes()) {
                if (type instanceof ClassOrInterfaceDeclaration clazz && !clazz.isInterface()) {
                    candidates.addAll(scanClass(clazz, pkg, filePath, config));
                }
            }
        } catch (Exception _) {
        }

        return candidates;
    }

    @Override
    public String getName() {
        return "";
    }

    private List<HotspotCandidate> scanClass(ClassOrInterfaceDeclaration clazz, String pkg,
                                             Path filePath, ScannerConfiguration config) {
        List<HotspotCandidate> candidates = new ArrayList<>();
        String className = pkg.isEmpty() ? clazz.getNameAsString() : pkg + "." + clazz.getNameAsString();

        Set<HotspotReason> classReasons = new HashSet<>();
        for (var ann : clazz.getAnnotations()) {
            if (SPRING_STEREOTYPE_ANNOTATIONS.contains(ann.getNameAsString())) {
                classReasons.add(HotspotReason.SERVICE_ANNOTATION);
            }
        }

        for (MethodDeclaration method : clazz.getMethods()) {
            if (method.isPrivate()) continue;

            var candidate = analyzeMethod(method, className, classReasons, filePath, config);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }

        return candidates;
    }

    private HotspotCandidate analyzeMethod(MethodDeclaration method, String className,
                                           Set<HotspotReason> classReasons, Path filePath,
                                           ScannerConfiguration config) {
        Set<HotspotReason> reasons = new HashSet<>(classReasons);

        for (AnnotationExpr annotation : method.getAnnotations()) {
            String name = annotation.getNameAsString();
            if (ENDPOINT_ANNOTATIONS.contains(name)) {
                reasons.add(HotspotReason.CONTROLLER_ENDPOINT);
            }
            if (name.equals("Transactional")) {
                reasons.add(HotspotReason.TRANSACTIONAL_METHOD);
            }
            if (name.equals("Async")) {
                reasons.add(HotspotReason.ASYNC_METHOD);
            }
        }

        ComplexityMetrics metrics = analyzeComplexity(method);

        if (metrics.getCyclomaticComplexity() >= config.getMinComplexity()) {
            reasons.add(HotspotReason.HIGH_CYCLOMATIC_COMPLEXITY);
        }
        if (metrics.getNumberOfLoops() > 0) {
            reasons.add(HotspotReason.CONTAINS_LOOP);
        }

        MethodSignature signature = new MethodSignature();
        signature.setClassName(className);
        signature.setMethodName(method.getNameAsString());
        signature.setParameterTypes(method.getParameters().stream()
                .map(p -> p.getType().asString())
                .collect(Collectors.toList()));
        signature.setReturnType(method.getType().asString());
        signature.setSourceFilePath(filePath.toString());
        signature.setLineNumber(method.getBegin().map(p -> p.line).orElse(0));

        boolean isWhitelisted = config.getWhitelist().contains(signature.getUniqueId());

        if (reasons.isEmpty() && !isWhitelisted) {
            return null;
        }

        HotspotCandidate candidate = new HotspotCandidate();
        candidate.setMethodSignature(signature);
        candidate.setReasons(reasons);
        candidate.setComplexityMetrics(metrics);

        for (HotspotReason reason : reasons) {
            candidate.addReason(reason);
        }

        return candidate;
    }

    private ComplexityMetrics analyzeComplexity(MethodDeclaration method) {
        if (method.getBody().isEmpty()) {
            return ComplexityMetrics.builder().cyclomaticComplexity(1).build();
        }

        var body = method.getBody().get();
        var analyzer = new ComplexityAnalyzer();
        body.accept(analyzer, null);

        int loc = body.getEnd()
                .map(e -> e.line - body.getBegin().map(b -> b.line).orElse(0))
                .orElse(0);

        return ComplexityMetrics.builder()
                .cyclomaticComplexity(1 + analyzer.branches)
                .linesOfCode(loc)
                .numberOfLoops(analyzer.loops)
                .numberOfBranches(analyzer.branches)
                .nestingDepth(analyzer.maxDepth)
                .build();
    }

    private boolean shouldScan(String pkg, ScannerConfiguration config) {
        for (String excluded : config.getExcludePackages()) {
            if (pkg.startsWith(excluded)) return false;
        }
        if (config.getIncludePackages().isEmpty()) return true;
        for (String included : config.getIncludePackages()) {
            if (pkg.startsWith(included)) return true;
        }
        return false;
    }

    private List<HotspotCandidate> filter(List<HotspotCandidate> candidates, ScannerConfiguration config) {
        return candidates.stream()
                .filter(c -> {
                    String id = c.getMethodSignature().getUniqueId();
                    if (config.getBlacklist().contains(id)) return false;
                    if (config.getWhitelist().contains(id)) {
                        c.setManuallyAdded(true);
                        return true;
                    }
                    return !c.getReasons().isEmpty();
                })
                .collect(Collectors.toList());
    }
}