package com.devhub.ocr.QA.A0.QAA0_0100.mod;

import com.devhub.ocr.app.systems.mod.FileService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QAA0_1000ContactsService {

    private final FileService fileService;

    public QAA0_1000ContactsService(FileService fileService) {
        this.fileService = fileService;
    }

    private Path dirForUser(Long userId) throws IOException {
        if (userId == null) throw new IOException("userId required");
        Path dir = Paths.get(fileService.getUploadDir(), "com", "devhub", "ocr", "QA", "A0", "QAA0_0100", "contacts");
        if (!Files.exists(dir)) Files.createDirectories(dir);
        return dir.resolve("user-" + userId + ".dat");
    }

    public static class Contact {
        private static final Base64.Encoder B64_ENC = Base64.getUrlEncoder().withoutPadding();
        private static final Base64.Decoder B64_DEC = Base64.getUrlDecoder();

        public String chatId;
        public String firstName;
        public String lastName;
        public String username;
        public String avatarPath;
        public long createdAt;

        public Contact() {}

        public Contact(String chatId, String firstName, String lastName, String username, String avatarPath) {
            this.chatId = chatId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.username = username;
            this.avatarPath = avatarPath;
            this.createdAt = Instant.now().toEpochMilli();
        }

        public String toLine() {
            StringBuilder sb = new StringBuilder();
            sb.append(encode(chatId)).append('|');
            sb.append(encode(firstName)).append('|');
            sb.append(encode(lastName)).append('|');
            sb.append(encode(username)).append('|');
            sb.append(encode(avatarPath)).append('|');
            sb.append(Long.toString(createdAt));
            return sb.toString();
        }

        public static Contact fromLine(String line) {
            String[] p = line.split("\\|", -1);
            if (p.length < 6) return null;
            Contact c = new Contact();
            c.chatId = decode(p[0]);
            c.firstName = decode(p[1]);
            c.lastName = decode(p[2]);
            c.username = decode(p[3]);
            c.avatarPath = decode(p[4]);
            try { c.createdAt = Long.parseLong(p[5]); } catch (Exception ex) { c.createdAt = Instant.now().toEpochMilli(); }
            return c;
        }

        private static String encode(String s) {
            if (s == null) return "";
            return B64_ENC.encodeToString(s.getBytes(StandardCharsets.UTF_8));
        }

        private static String decode(String s) {
            if (s == null || s.isEmpty()) return null;
            try { return new String(B64_DEC.decode(s), StandardCharsets.UTF_8); } catch (IllegalArgumentException ex) { return null; }
        }
    }

    public List<Contact> loadForUser(Long userId) throws IOException {
        Path p = dirForUser(userId);
        Map<String, Contact> map = new ConcurrentHashMap<>();
        if (!Files.exists(p)) return Collections.emptyList();
        try (BufferedReader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim(); if (line.isEmpty()) continue;
                Contact c = Contact.fromLine(line);
                if (c != null && c.chatId != null) map.put(c.chatId, c);
            }
        }
        return new ArrayList<>(map.values());
    }

    public void saveForUser(Long userId, List<Contact> list) throws IOException {
        Path p = dirForUser(userId);
        Path tmp = p.resolveSibling(p.getFileName().toString() + ".tmp");
        try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            for (Contact c : list) {
                w.write(c.toLine()); w.newLine();
            }
            w.flush();
        }
        Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public void addOrUpdateContact(Long userId, Contact contact) throws IOException {
        List<Contact> current = new ArrayList<>(loadForUser(userId));
        boolean found = false;
        for (Contact c : current) {
            if (c.chatId != null && c.chatId.equals(contact.chatId)) {
                // update fields
                c.firstName = contact.firstName != null ? contact.firstName : c.firstName;
                c.lastName = contact.lastName != null ? contact.lastName : c.lastName;
                c.username = contact.username != null ? contact.username : c.username;
                c.avatarPath = contact.avatarPath != null ? contact.avatarPath : c.avatarPath;
                found = true;
                break;
            }
        }
        if (!found) current.add(contact);
        saveForUser(userId, current);
    }

}
