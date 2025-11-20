package com.devhub.ocr.QA.A0.QAA0_0100.mod;

import com.devhub.ocr.app.systems.mod.FileService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Small helper service to persist QAA0_1000 per user using a file under the application's upload
 * directory. All file paths are derived from FileService.getUploadDir() to comply with project
 * storage conventions.
 */
@Service
public class QAA0_1000Service {

    private final FileService fileService;

    public QAA0_1000Service(FileService fileService) {
        this.fileService = fileService;
    }

    private Path pathForUser(Long userId) throws IOException {
        if (userId == null) throw new IOException("userId required");
        // place files under uploads/com/devhub/ocr/QA/A0/QAA0_0100/QAA0_1000/
        Path dir = Paths.get(fileService.getUploadDir(), "com", "devhub", "ocr", "QA", "A0", "QAA0_0100", "QAA0_1000");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir.resolve("user-" + userId + ".dat");
    }

    public QAA0_1000 loadForUser(Long userId) throws IOException {
        QAA0_1000 mod = new QAA0_1000();
        Path p = pathForUser(userId);
        if (Files.exists(p)) {
            mod.loadFromFile(p);
        }
        return mod;
    }

    public void saveForUser(Long userId, QAA0_1000 mod) throws IOException {
        Path p = pathForUser(userId);
        mod.saveToFile(p);
    }

}
