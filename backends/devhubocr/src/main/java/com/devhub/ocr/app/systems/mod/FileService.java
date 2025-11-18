package com.devhub.ocr.app.systems.mod;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileService {
    /// workspaces/devhub_computer_vision/backends/devhubocr/uploads
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

    public static void main(String[] args) {
        FileService fs = new FileService();
        String hash = fs.hashFile("example.txt", 12345, 67890);
        System.out.println("Hash: " + hash);
        boolean isValid = fs.verifyHash("example.txt", 12345, 67890, hash);
        System.out.println("Is valid: " + isValid);
    }
}
