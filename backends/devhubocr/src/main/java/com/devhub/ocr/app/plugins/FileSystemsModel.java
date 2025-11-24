package com.devhub.ocr.app.plugins;

import java.io.File;
import java.util.Date;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class FileSystemsModel {
    private String id;
    private String name;
    private String path_user_upload;
    private String path;
    private String url;
    private long size;
    private String type;
    private String owner;
    private String createdAt;
    private String modifiedAt;

    public FileSystemsModel(File file, String user_id) {
        this.id = UUID.randomUUID().toString();
        this.name = file.getName();
        this.path = file.getPath();
        this.url = "";
        this.size = file.length();
        this.type = file.isDirectory() ? "directory" : "file";
        this.owner = user_id;
        this.createdAt = new Date().toString(); // now as String
        this.modifiedAt = new Date(file.lastModified()).toString();
    }

    public String getHash() {
        try {
            if (this.path != null) {
                java.nio.file.Path p = java.nio.file.Paths.get(this.path);
                if (java.nio.file.Files.exists(p) && java.nio.file.Files.isRegularFile(p)) {
                    java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                    try (java.io.InputStream is = java.nio.file.Files.newInputStream(p)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            md.update(buffer, 0, read);
                        }
                    }
                    return bytesToHex(md.digest());
                }
            }
        } catch (Exception ignored) {
            // fall through to fallback below
        }

        // Fallback: compute SHA-256 of metadata (name + size + file lastModified if
        // available)
        String metaModified;
        try {
            java.io.File f = (this.path != null) ? new java.io.File(this.path) : null;
            metaModified = (f != null && f.exists()) ? String.valueOf(f.lastModified())
                    : (this.modifiedAt != null ? this.modifiedAt : "");
        } catch (Exception e) {
            metaModified = (this.modifiedAt != null) ? this.modifiedAt : "";
        }
        String data = (this.name == null ? "" : this.name) + this.size + metaModified;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            md.update(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(md.digest());
        } catch (Exception e) {
            // Last resort: return simple hashCode hex
            return Integer.toHexString(data.hashCode());
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(String modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public String getPath_user_upload() {
        return path_user_upload;
    }

    public void setPath_user_upload(String path_user_upload) {
        this.path_user_upload = path_user_upload;
    }

    public static void main(String[] args) {
        File file = new File("C:/WorkSpace3/devhub_computer_vision/README.md");
        FileSystemsModel fsm = new FileSystemsModel(file, "user123");
        System.out.println("File ID: " + fsm.getId());
        System.out.println("File Name: " + fsm.getName());
        System.out.println("File Path: " + fsm.getPath());
        System.out.println("File Size: " + fsm.getSize());
        System.out.println("File Type: " + fsm.getType());
        System.out.println("File Owner: " + fsm.getOwner());
        System.out.println("File Hash: " + fsm.getHash());
        System.out.println("Created At: " + fsm.getCreatedAt());
        System.out.println("Modified At: " + fsm.getModifiedAt());
        System.out.println("Decoded Hash: " + fsm.getHash());
        System.out.println("Encrypted Data: " + fsm.getName());
    }

    public static byte[] encrypt(byte[] data, String key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] data, String key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

}
