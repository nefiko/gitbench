package utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class FileUtils {

    private static final Set<String> SKIP_DIRS = Set.of(
            "target", "build", "node_modules", ".git", ".gitbench", "out", ".mvn", ".gradle", ".idea"
    );

    private FileUtils() {
    }

    public static List<Path> findJavaFiles(Path directory) throws IOException {
        List<Path> files = new ArrayList<>();

        if (!Files.exists(directory)) {
            return files;
        }

        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".java")) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (SKIP_DIRS.contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return files;
    }
}
