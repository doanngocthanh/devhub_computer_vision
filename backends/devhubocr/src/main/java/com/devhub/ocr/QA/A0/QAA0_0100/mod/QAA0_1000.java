package com.devhub.ocr.QA.A0.QAA0_0100.mod;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.Base64;

/**
 * QAA0_1000 — Telegram Bot configuration manager
 *
 * Mục đích:
 * - Cho phép lưu nhiều cấu hình bot Telegram (token, chatId, tên, mô tả, trạng thái) trong bộ nhớ.
 * - Hỗ trợ lưu / tải cấu hình sang/từ một file cục bộ (định dạng pipe-separated, trường dữ liệu được Base64-encode để an toàn).
 *
 * Lý do đơn giản: tránh thêm dependency mới (JSON libraries). Định dạng file dễ parse và an toàn cho dữ liệu chứa ký tự đặc biệt.
 *
 * Contract (giản lược):
 * - addBot(bot) -> returns UUID
 * - removeBot(id) -> boolean
 * - updateBot(bot) -> boolean
 * - listBots() -> List<BotConfig>
 * - saveToFile(path) / loadFromFile(path)
 *
 * File format (text): mỗi dòng một bot, header bắt đầu: #QAA0_1000 v1
 * Fields (pipe '|'):
 * id | b64(name) | b64(token) | b64(chatId) | enabled(true|false) | b64(description) | createdEpochMilli
 */
public class QAA0_1000 {

    private final Map<UUID, BotConfig> bots = new ConcurrentHashMap<>();

    public static final String FILE_HEADER = "#QAA0_1000 v1";

    public QAA0_1000() {
    
    }

    public UUID addBot(BotConfig bot) {
        if (bot.id == null) {
            bot.id = UUID.randomUUID();
        }
        if (bot.createdAt == null) {
            bot.createdAt = Instant.now();
        }
        bots.put(bot.id, bot);
        return bot.id;
    }

    public boolean removeBot(UUID id) {
        return bots.remove(id) != null;
    }

    public boolean updateBot(BotConfig bot) {
        if (bot == null || bot.id == null) return false;
        if (!bots.containsKey(bot.id)) return false;
        bots.put(bot.id, bot);
        return true;
    }

    public List<BotConfig> listBots() {
        return Collections.unmodifiableList(new ArrayList<>(bots.values()));
    }

    public Optional<BotConfig> findById(UUID id) {
        return Optional.ofNullable(bots.get(id));
    }

    /**
     * Save current bot list to a file atomically (write to temp then move).
     */
    public void saveToFile(Path file) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            w.write(FILE_HEADER);
            w.newLine();
            for (BotConfig b : listBots()) {
                w.write(b.toLine());
                w.newLine();
            }
            w.flush();
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Load bots from file (replaces in-memory store).
     */
    public void loadFromFile(Path file) throws IOException {
        Map<UUID, BotConfig> loaded = new ConcurrentHashMap<>();
        if (!Files.exists(file)) {
            bots.clear();
            return;
        }
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String first = r.readLine();
            if (first == null || !first.trim().equals(FILE_HEADER)) {
                throw new IOException("Invalid QAA0_1000 file header");
            }
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                BotConfig b = BotConfig.fromLine(line);
                if (b != null && b.id != null) {
                    loaded.put(b.id, b);
                }
            }
        }
        bots.clear();
        bots.putAll(loaded);
    }

    // --- BotConfig ---
    public static class BotConfig {
        private static final Base64.Encoder B64_ENC = Base64.getUrlEncoder().withoutPadding();
        private static final Base64.Decoder B64_DEC = Base64.getUrlDecoder();

        public UUID id;
        public String name;
        public String token;
        public String chatId;
        public boolean enabled = true;
        public String description;
        public Instant createdAt;

        public BotConfig() {
        }

        public BotConfig(String name, String token, String chatId, boolean enabled, String description) {
            this.name = name;
            this.token = token;
            this.chatId = chatId;
            this.enabled = enabled;
            this.description = description;
            this.createdAt = Instant.now();
        }

        public String toLine() {
            StringBuilder sb = new StringBuilder();
            sb.append(id == null ? "" : id.toString());
            sb.append('|');
            sb.append(encode(name));
            sb.append('|');
            sb.append(encode(token));
            sb.append('|');
            sb.append(encode(chatId));
            sb.append('|');
            sb.append(Boolean.toString(enabled));
            sb.append('|');
            sb.append(encode(description));
            sb.append('|');
            sb.append(createdAt == null ? "" : Long.toString(createdAt.toEpochMilli()));
            return sb.toString();
        }

        public static BotConfig fromLine(String line) {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 7) return null;
            BotConfig b = new BotConfig();
            try {
                if (!parts[0].isEmpty()) b.id = UUID.fromString(parts[0]);
            } catch (Exception ex) {
                // ignore invalid id
            }
            b.name = decode(parts[1]);
            b.token = decode(parts[2]);
            b.chatId = decode(parts[3]);
            b.enabled = Boolean.parseBoolean(parts[4]);
            b.description = decode(parts[5]);
            try {
                if (!parts[6].isEmpty()) b.createdAt = Instant.ofEpochMilli(Long.parseLong(parts[6]));
            } catch (Exception ex) {
                b.createdAt = Instant.now();
            }
            return b;
        }

        private static String encode(String s) {
            if (s == null) return "";
            return B64_ENC.encodeToString(s.getBytes(StandardCharsets.UTF_8));
        }

        private static String decode(String s) {
            if (s == null || s.isEmpty()) return null;
            try {
                byte[] bs = B64_DEC.decode(s);
                return new String(bs, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        @Override
        public String toString() {
            return "BotConfig{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", chatId='" + chatId + '\'' +
                    ", enabled=" + enabled +
                    ", createdAt=" + createdAt +
                    '}';
        }
    }

    // --- convenience helpers ---
    public List<BotConfig> findEnabledBots() {
        return bots.values().stream().filter(b -> b.enabled).collect(Collectors.toList());
    }

}
