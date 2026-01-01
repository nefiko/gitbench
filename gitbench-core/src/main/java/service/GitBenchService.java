package service;

import config.GitBenchConfiguration;
import entity.MethodEntity;
import entity.ProjectEntity;
import lombok.RequiredArgsConstructor;
import model.HotspotCandidate;
import model.MethodSignature;
import model.ScanResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.MethodRepository;
import repository.ProjectRepository;
import scanner.impl.StaticCodeScanner;
import util.EntityMapper;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GitBenchService {

    private final ProjectRepository projects;
    private final MethodRepository methodRepository;
    private final EntityMapper entityMapper;

    @Transactional
    public ProjectEntity initProject(Path path) {
        String pathStr = path.toAbsolutePath().toString();

        return projects.findByPath(pathStr).orElseGet(() -> {
            var project = ProjectEntity.builder()
                    .name(path.getFileName().toString())
                    .path(pathStr)
                    .build();
            return projects.save(project);
        });
    }

    @Transactional
    public ScanResult scan(ProjectEntity project, GitBenchConfiguration config) {
        var scanner = new StaticCodeScanner();
        var projectPath = Path.of(project.getPath());

        var start = Instant.now();
        var candidates = scanner.scan(projectPath, config.getScanner());
        var end = Instant.now();

        for (var candidate : candidates) {
            saveMethod(project, candidate);
        }

        return ScanResult.builder()
                .projectPath(projectPath)
                .candidates(candidates)
                .startTime(start)
                .endTime(end)
                .build();
    }

    private void saveMethod(ProjectEntity project, HotspotCandidate candidate) {
        MethodSignature sig = candidate.getMethodSignature();
        Optional<MethodEntity> existing = methodRepository.findByProjectAndSignatureId(project, sig.getUniqueId());

        MethodEntity method;
        method = existing.orElseGet(() -> entityMapper.toEntity(candidate, project));

        method.setPriorityScore(candidate.getPriorityScore());
        if (candidate.getReasons() != null) {
            String reasons = candidate.getReasons().stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(","));
            method.setHotspotReasons(reasons);
        }

        methodRepository.save(method);
    }
}
