import exception.GitOperationException;
import lombok.Data;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Data
public class GitWorkspaceManager implements Closeable {

    private final GitService gitService;
    private final Path baseWorkspacePath;
    private final Map<String, Path> workspaces = new HashMap<>();
    private final boolean cleanupOnClose;
    private final String originalBranch;

    public GitWorkspaceManager(GitService gitService, Path workspacePath, boolean cleanupOnClose) {
        this.gitService = gitService;
        this.baseWorkspacePath = workspacePath;
        this.cleanupOnClose = cleanupOnClose;

        try {
            Files.createDirectories(baseWorkspacePath);
            this.originalBranch = gitService.getCurrentBranch();
        } catch (IOException e) {
            throw new GitOperationException("Failed to create workspace directory", e);
        }
    }

    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                        }
                    });
        }
    }

    @Override
    public void close() {
        if (originalBranch != null) {
            try {
                gitService.checkoutBranch(originalBranch);
            } catch (Exception e) {
            }
        }

        if (cleanupOnClose) {
            for (Path workspace : workspaces.values()) {
                try {
                    deleteDirectoryRecursively(workspace);
                } catch (IOException e) {
                }
            }
            workspaces.clear();

            try {
                deleteDirectoryRecursively(baseWorkspacePath);
            } catch (IOException e) {
            }
        }
    }
}
