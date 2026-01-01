import config.GitBenchConfiguration;
import config.ScannerConfiguration;
import entity.MethodEntity;
import entity.ProjectEntity;
import model.HotspotCandidate;
import model.ScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.MethodRepository;
import repository.ProjectRepository;
import service.GitBenchService;
import util.EntityMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitBenchServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private MethodRepository methodRepository;

    @Mock
    private EntityMapper entityMapper;

    private GitBenchService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        service = new GitBenchService(projectRepository, methodRepository, entityMapper);
    }

    @Test
    void initProject_createsNewProject_whenNotExists() {
        when(projectRepository.findByPath(any())).thenReturn(Optional.empty());
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProjectEntity result = service.initProject(tempDir);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(tempDir.getFileName().toString());
        assertThat(result.getPath()).isEqualTo(tempDir.toAbsolutePath().toString());
        verify(projectRepository).save(any());
    }

    @Test
    void initProject_returnsExisting_whenAlreadyExists() {
        ProjectEntity existing = new ProjectEntity();
        existing.setId(1L);
        existing.setName("existing");
        existing.setPath(tempDir.toAbsolutePath().toString());

        when(projectRepository.findByPath(any())).thenReturn(Optional.of(existing));

        ProjectEntity result = service.initProject(tempDir);

        assertThat(result).isSameAs(existing);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void scan_returnsResultWithCandidates() throws Exception {
        ProjectEntity project = new ProjectEntity();
        project.setPath(tempDir.toAbsolutePath().toString());

        GitBenchConfiguration config = new GitBenchConfiguration();
        config.setScanner(new ScannerConfiguration());

        String code = """
                package com.example;
                
                import org.springframework.stereotype.Service;
                
                @Service
                public class TestService {
                    public void doWork() {
                    }
                }
                """;
        createJavaFile("com/example/TestService.java", code);

        when(methodRepository.findByProjectAndSignatureId(any(), any())).thenReturn(Optional.empty());
        when(entityMapper.toEntity(any(HotspotCandidate.class), any())).thenReturn(new MethodEntity());

        ScanResult result = service.scan(project, config);

        assertThat(result).isNotNull();
        assertThat(result.getProjectPath()).isEqualTo(tempDir);
        assertThat(result.getCandidates()).hasSize(1);
        assertThat(result.getStartTime()).isNotNull();
        assertThat(result.getEndTime()).isNotNull();
    }

    @Test
    void scan_savesFoundMethods() throws Exception {
        ProjectEntity project = new ProjectEntity();
        project.setPath(tempDir.toAbsolutePath().toString());

        GitBenchConfiguration config = new GitBenchConfiguration();
        config.setScanner(new ScannerConfiguration());

        String code = """
                package com.example;
                
                import org.springframework.stereotype.Service;
                
                @Service
                public class MyService {
                    public void method1() {}
                    public void method2() {}
                }
                """;
        createJavaFile("com/example/MyService.java", code);

        when(methodRepository.findByProjectAndSignatureId(any(), any())).thenReturn(Optional.empty());
        when(entityMapper.toEntity(any(HotspotCandidate.class), any())).thenReturn(new MethodEntity());

        service.scan(project, config);

        verify(methodRepository, times(2)).save(any());
    }

    @Test
    void scan_updatesExistingMethod() throws Exception {
        ProjectEntity project = new ProjectEntity();
        project.setPath(tempDir.toAbsolutePath().toString());

        GitBenchConfiguration config = new GitBenchConfiguration();
        config.setScanner(new ScannerConfiguration());

        String code = """
                package com.example;
                
                import org.springframework.stereotype.Service;
                
                @Service
                public class MyService {
                    public void existingMethod() {}
                }
                """;
        createJavaFile("com/example/MyService.java", code);

        MethodEntity existingMethod = new MethodEntity();
        existingMethod.setId(99L);
        existingMethod.setPriorityScore(0);

        when(methodRepository.findByProjectAndSignatureId(any(), any())).thenReturn(Optional.of(existingMethod));

        service.scan(project, config);

        ArgumentCaptor<MethodEntity> captor = ArgumentCaptor.forClass(MethodEntity.class);
        verify(methodRepository).save(captor.capture());

        MethodEntity saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(99L);
        assertThat(saved.getPriorityScore()).isGreaterThan(0);
    }

    @Test
    void scan_setsHotspotReasons() throws Exception {
        ProjectEntity project = new ProjectEntity();
        project.setPath(tempDir.toAbsolutePath().toString());

        GitBenchConfiguration config = new GitBenchConfiguration();
        config.setScanner(new ScannerConfiguration());

        String code = """
                package com.example;
                
                import org.springframework.stereotype.Service;
                import org.springframework.transaction.annotation.Transactional;
                
                @Service
                public class MyService {
                    @Transactional
                    public void transactionalMethod() {}
                }
                """;
        createJavaFile("com/example/MyService.java", code);

        MethodEntity method = new MethodEntity();
        when(methodRepository.findByProjectAndSignatureId(any(), any())).thenReturn(Optional.empty());
        when(entityMapper.toEntity(any(HotspotCandidate.class), any())).thenReturn(method);

        service.scan(project, config);

        ArgumentCaptor<MethodEntity> captor = ArgumentCaptor.forClass(MethodEntity.class);
        verify(methodRepository).save(captor.capture());

        String reasons = captor.getValue().getHotspotReasons();
        assertThat(reasons).contains("SERVICE_ANNOTATION");
        assertThat(reasons).contains("TRANSACTIONAL_METHOD");
    }

    @Test
    void scan_returnsEmptyResult_forEmptyProject() {
        ProjectEntity project = new ProjectEntity();
        project.setPath(tempDir.toAbsolutePath().toString());

        GitBenchConfiguration config = new GitBenchConfiguration();
        config.setScanner(new ScannerConfiguration());

        ScanResult result = service.scan(project, config);

        assertThat(result.getCandidates()).isEmpty();
        verify(methodRepository, never()).save(any());
    }

    private void createJavaFile(String relativePath, String content) throws Exception {
        Path srcDir = tempDir.resolve("src/main/java");
        Path file = srcDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
