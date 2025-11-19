package com.devhub.ocr.app.systems.mod;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class FileService {
    /// uploads directory relative to project root
    private final String uploadDir = "uploads";

    public String getUploadDir() {
        return uploadDir;
    }

    public List<String> getAllFiles() {
        // list all files in uploadDir
        File dir = new File(uploadDir);
        String[] files = dir.list();
        if (files == null) {
            return new ArrayList<>();
        }
        return Arrays.asList(files);

    }

    public String hashFile(String filename, long fileSize, long lastModified) {
        String data = filename + fileSize + lastModified;
        return Integer.toHexString(data.hashCode());
    }

    public boolean verifyHash(String filename, long fileSize, long lastModified, String hash) {
        String expectedHash = hashFile(filename, fileSize, lastModified);
        System.err.println("Expected: " + expectedHash + ", Given: " + hash);
        return expectedHash.equals(hash);
    }

    /**
     * Save a multipart file into the uploads directory with the given target filename.
     * Creates the uploads directory if missing. Returns stored filename on success.
     */
    public String saveUploadedFile(MultipartFile file, String targetFilename) throws IOException {
        if (file == null || file.isEmpty()) return null;
        Path dir = Paths.get(uploadDir);
        if (!Files.exists(dir)) Files.createDirectories(dir);
        String filename = targetFilename == null || targetFilename.isBlank() ? file.getOriginalFilename() : targetFilename;
        Path dest = dir.resolve(filename).normalize();
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        return dest.getFileName().toString();
    }

    /**
     * Save raw bytes to uploads directory with a filename.
     */
    public String saveBytes(byte[] bytes, String filename) throws IOException {
        if (bytes == null || filename == null) return null;
        Path dir = Paths.get(uploadDir);
        if (!Files.exists(dir)) Files.createDirectories(dir);
        Path dest = dir.resolve(filename).normalize();
        Files.write(dest, bytes);
        return dest.getFileName().toString();
    }

    public Path getFilePath(String filename) {
        if (filename == null) return null;
        Path p = Paths.get(uploadDir).resolve(filename).normalize();
        return p;
    }

    public String getPublicUrl(String filename) {
        if (filename == null || filename.isBlank()) return "";
        return "/uploads/" + filename;
    }

    public boolean deleteFile(String filename) {
        if (filename == null) return false;
        Path p = getFilePath(filename);
        try {
            return Files.deleteIfExists(p);
        } catch (IOException ex) {
            return false;
        }
    }

    public static void main(String[] args) {
        FileService fs = new FileService();
        String hash = fs.hashFile("example.txt", 12345, 67890);
        System.out.println("Hash: " + hash);
        boolean isValid = fs.verifyHash("example.txt", 12345, 67890, hash);
        System.out.println("Is valid: " + isValid);
    }
}
