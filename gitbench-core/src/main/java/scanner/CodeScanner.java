package scanner;

import config.ScannerConfiguration;
import model.HotspotCandidate;

import java.nio.file.Path;
import java.util.List;

public interface CodeScanner {

    List<HotspotCandidate> scan(Path projectPath, ScannerConfiguration config);

    List<HotspotCandidate> scanFile(Path filePath, ScannerConfiguration config);

    String getName();
}
