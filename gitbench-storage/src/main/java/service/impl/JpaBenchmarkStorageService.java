package service.impl;

import entity.BenchmarkResultEntity;
import entity.CommitEntity;
import entity.MethodEntity;
import entity.ProjectEntity;
import model.BenchmarkResult;
import model.CommitInfo;
import model.MethodSignature;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.BenchmarkResultRepository;
import repository.CommitRepository;
import repository.MethodRepository;
import repository.ProjectRepository;
import service.BenchmarkStorageService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional
public class JpaBenchmarkStorageService implements BenchmarkStorageService {

    private final ProjectRepository projectRepository;
    private final MethodRepository methodRepository;
    private final CommitRepository commitRepository;
    private final BenchmarkResultRepository resultRepository;

    public JpaBenchmarkStorageService(
            ProjectRepository projectRepository,
            MethodRepository methodRepository,
            CommitRepository commitRepository,
            BenchmarkResultRepository resultRepository) {
        this.projectRepository = projectRepository;
        this.methodRepository = methodRepository;
        this.commitRepository = commitRepository;
        this.resultRepository = resultRepository;
    }

    @Override
    public void saveResults(List<BenchmarkResult> results, String projectName) {
        ProjectEntity project = getOrCreateProject(projectName);

        for (BenchmarkResult result : results) {
            MethodEntity method = getOrCreateMethod(result.getMethodSignature(), project);
            CommitEntity commit = getOrCreateCommit(result.getCommitInfo(), project);

            BenchmarkResultEntity entity = mapToEntity(result, method, commit);
            resultRepository.save(entity);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BenchmarkResult> getResultsForCommit(String commitHash, String projectName) {
        return projectRepository.findByName(projectName)
                .flatMap(project -> commitRepository.findByProjectAndHash(project, commitHash))
                .map(commit -> resultRepository.findByMethodOrderByCreatedAtDesc(commit.getBenchmarkRuns().get(0).getResults().get(0).getMethod()))
                .orElse(List.of())
                .stream()
                .map(this::mapToModel)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BenchmarkResult> getResultsForMethod(MethodSignature method, String projectName) {
        return projectRepository.findByName(projectName)
                .flatMap(project -> methodRepository.findByProjectAndClassNameAndMethodName(
                        project, method.getClassName(), method.getMethodName()))
                .map(resultRepository::findByMethodOrderByCreatedAtDesc)
                .orElse(List.of())
                .stream()
                .map(this::mapToModel)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BenchmarkResult> getLatestResultForMethod(MethodSignature method, String projectName) {
        return projectRepository.findByName(projectName)
                .flatMap(project -> methodRepository.findByProjectAndClassNameAndMethodName(
                        project, method.getClassName(), method.getMethodName()))
                .flatMap(resultRepository::findFirstByMethodOrderByCreatedAtDesc)
                .map(this::mapToModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BenchmarkResult> getHistory(String projectName, int limit) {
        return projectRepository.findByName(projectName)
                .map(project -> resultRepository.findAll().stream()
                        .filter(r -> r.getMethod().getProject().equals(project))
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .limit(limit)
                        .map(this::mapToModel)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BenchmarkResult> getBaseline(MethodSignature method, String parentCommitHash, String projectName) {
        return projectRepository.findByName(projectName)
                .flatMap(project -> methodRepository.findByProjectAndClassNameAndMethodName(
                        project, method.getClassName(), method.getMethodName()))
                .flatMap(m -> resultRepository.findBaselineResult(m, parentCommitHash))
                .map(this::mapToModel);
    }

    private ProjectEntity getOrCreateProject(String projectName) {
        return projectRepository.findByName(projectName)
                .orElseGet(() -> {
                    ProjectEntity project = new ProjectEntity();
                    project.setName(projectName);
                    return projectRepository.save(project);
                });
    }

    private MethodEntity getOrCreateMethod(MethodSignature signature, ProjectEntity project) {
        String signatureId = signature.getClassName() + "#" + signature.getMethodName();
        return methodRepository.findByProjectAndSignatureId(project, signatureId)
                .orElseGet(() -> {
                    MethodEntity method = new MethodEntity();
                    method.setProject(project);
                    method.setSignatureId(signatureId);
                    method.setClassName(signature.getClassName());
                    method.setMethodName(signature.getMethodName());
                    return methodRepository.save(method);
                });
    }

    private CommitEntity getOrCreateCommit(CommitInfo info, ProjectEntity project) {
        if (info == null) {
            return null;
        }
        return commitRepository.findByProjectAndHash(project, info.getHash())
                .orElseGet(() -> {
                    CommitEntity commit = new CommitEntity();
                    commit.setProject(project);
                    commit.setHash(info.getHash());
                    commit.setShortHash(info.getShortHash());
                    commit.setMessage(info.getMessage());
                    commit.setAuthorName(info.getAuthorName());
                    commit.setCommitTimestamp(info.getTimestamp());
                    return commitRepository.save(commit);
                });
    }

    private BenchmarkResultEntity mapToEntity(BenchmarkResult result, MethodEntity method, CommitEntity commit) {
        BenchmarkResultEntity entity = new BenchmarkResultEntity();
        entity.setMethod(method);
        entity.setCommit(commit);
        entity.setSuccessful(result.isSuccessful());
        entity.setErrorMessage(result.getErrorMessage());

        if (result.getExecutionStats() != null) {
            entity.setAvgTime(result.getExecutionStats().getAverageTime());
            entity.setMinTime(result.getExecutionStats().getMinTime());
            entity.setMaxTime(result.getExecutionStats().getMaxTime());
            entity.setStdDev(result.getExecutionStats().getStandardDeviation());
            entity.setIterations(result.getExecutionStats().getIterations());
        }

        if (result.getTimeUnit() != null) {
            entity.setTimeUnit(result.getTimeUnit().name());
        }

        if (result.getMode() != null) {
            entity.setBenchmarkMode(result.getMode().name());
        }

        return entity;
    }

    private BenchmarkResult mapToModel(BenchmarkResultEntity entity) {
        MethodSignature signature = new MethodSignature();
        signature.setClassName(entity.getMethod().getClassName());
        signature.setMethodName(entity.getMethod().getMethodName());

        CommitInfo commitInfo = null;
        if (entity.getCommit() != null) {
            commitInfo = new CommitInfo();
            commitInfo.setHash(entity.getCommit().getHash());
            commitInfo.setMessage(entity.getCommit().getMessage());
            commitInfo.setAuthorName(entity.getCommit().getAuthorName());
            commitInfo.setTimestamp(entity.getCommit().getCommitTimestamp());
        }

        BenchmarkResult.ExecutionStats stats = BenchmarkResult.ExecutionStats.builder()
                .averageTime(entity.getAvgTime() != null ? entity.getAvgTime() : 0)
                .minTime(entity.getMinTime() != null ? entity.getMinTime() : 0)
                .maxTime(entity.getMaxTime() != null ? entity.getMaxTime() : 0)
                .standardDeviation(entity.getStdDev() != null ? entity.getStdDev() : 0)
                .iterations(entity.getIterations() != null ? entity.getIterations() : 0)
                .build();

        return BenchmarkResult.builder()
                .id(entity.getId().toString())
                .methodSignature(signature)
                .commitInfo(commitInfo)
                .executionStats(stats)
                .runTimestamp(entity.getCreatedAt())
                .timeUnit(entity.getTimeUnit() != null ? TimeUnit.valueOf(entity.getTimeUnit()) : TimeUnit.MILLISECONDS)
                .successful(entity.isSuccessful())
                .errorMessage(entity.getErrorMessage())
                .build();
    }
}