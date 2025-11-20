package com.devhub.ocr.QA.A0.QAA0_0101.mod;

import com.devhub.ocr.app.plugins.database.DatabasePlugin;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Service
public class QAA0_0101BotService {

    private final DatabasePlugin db;
    private final HttpClient http;

    public QAA0_0101BotService(DatabasePlugin db) {
        this.db = db;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public int createBot(String botId, String token, String baseUrl, String callbackUrl, String description) {
        String sql = "INSERT INTO qaa0_bot_configs(bot_id, token, base_url, callback_url, description, enabled) VALUES(:bot_id, :token, :base_url, :callback_url, :description, :enabled)";
        Map<String, Object> params = new HashMap<>();
        params.put("bot_id", botId);
        params.put("token", token);
        params.put("base_url", baseUrl != null ? baseUrl : "https://api.telegram.org");
        params.put("callback_url", callbackUrl);
        params.put("description", description);
        params.put("enabled", 1);
        return db.execute(sql, params);
    }

    public List<Map<String, Object>> listBots() {
        // do NOT include token in returned rows
        String sql = "SELECT id, bot_id, base_url, callback_url, description, enabled, created_at, updated_at FROM qaa0_bot_configs ORDER BY id DESC";
        return db.query(sql, null);
    }

    public String getTokenForBot(String botId) {
        String sql = "SELECT token FROM qaa0_bot_configs WHERE bot_id = :bot_id LIMIT 1";
        Map<String, Object> params = Map.of("bot_id", botId);
        List<Map<String, Object>> rows = db.query(sql, params);
        if (rows == null || rows.isEmpty()) return null;
        Object t = rows.get(0).get("token");
        return t != null ? String.valueOf(t) : null;
    }

    public Map<String, Object> getBotById(String botId) {
        String sql = "SELECT id, bot_id, base_url, callback_url, description, enabled, created_at, updated_at FROM qaa0_bot_configs WHERE bot_id = :bot_id LIMIT 1";
        Map<String, Object> params = Map.of("bot_id", botId);
        List<Map<String, Object>> rows = db.query(sql, params);
        if (rows == null || rows.isEmpty()) return null;
        return rows.get(0);
    }

    /**
     * Send a file by URL using the configured bot. This method keeps token internal and
     * performs a POST to the provider (Telegram-like) sendDocument API.
     * Returns the raw response body or null on failure.
     */
    public String sendFileByUrl(String botId, String chatId, String fileUrl, String caption) throws IOException, InterruptedException {
        String token = getTokenForBot(botId);
        if (token == null) throw new IllegalArgumentException("Bot not found");

        // fetch base_url for this bot
        String baseUrl = "https://api.telegram.org";
        Map<String, Object> bot = getBotById(botId);
        if (bot != null && bot.get("base_url") != null) baseUrl = String.valueOf(bot.get("base_url"));

        // Build sendDocument URL: {baseUrl}/bot{TOKEN}/sendDocument
        String apiUrl = String.format("%s/bot%s/sendDocument", baseUrl, token);

        // For simplicity we use application/x-www-form-urlencoded with document as URL parameter
        String body = "chat_id=" + java.net.URLEncoder.encode(chatId, java.nio.charset.StandardCharsets.UTF_8)
                + "&document=" + java.net.URLEncoder.encode(fileUrl, java.nio.charset.StandardCharsets.UTF_8);
        if (caption != null) body += "&caption=" + java.net.URLEncoder.encode(caption, java.nio.charset.StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        // persist send attempt to history table (best-effort)
        try {
            persistSendHistory(botId, chatId, fileUrl, caption, resp.body(), resp.statusCode());
        } catch (Exception ex) {
            // don't fail the send because history logging failed; log to stderr
            System.err.println("Failed to persist send history: " + ex.getMessage());
        }
        return resp.body();
    }

    public int persistSendHistory(String botId, String chatId, String fileUrl, String caption, String response, int statusCode) {
        String sql = "INSERT INTO qaa0_bot_send_history(bot_id, chat_id, file_url, caption, response, status_code, file_id) VALUES(:bot_id, :chat_id, :file_url, :caption, :response, :status_code, :file_id)";
        Map<String, Object> params = new HashMap<>();
        params.put("bot_id", botId);
        params.put("chat_id", chatId);
        params.put("file_url", fileUrl);
        params.put("caption", caption);
        params.put("response", response);
        params.put("status_code", statusCode);
        // try to extract file_id from provider response JSON (best-effort string search)
        String fileId = null;
        try {
            if (response != null) {
                String needle = "\"file_id\":\"";
                int p = response.indexOf(needle);
                if (p >= 0) {
                    int s = p + needle.length();
                    int e = response.indexOf('"', s);
                    if (e > s) fileId = response.substring(s, e);
                }
            }
        } catch (Throwable ignored) {}
        params.put("file_id", fileId);
        return db.execute(sql, params);
    }

    public List<Map<String, Object>> listSendHistory(String botId, Integer limit) {
    String sql = "SELECT id, bot_id, chat_id, file_url, caption, response, status_code, file_id, created_at FROM qaa0_bot_send_history";
        Map<String, Object> params = new HashMap<>();
        if (botId != null && !botId.isEmpty()) {
            sql += " WHERE bot_id = :bot_id";
            params.put("bot_id", botId);
        }
        sql += " ORDER BY id DESC";
        if (limit != null && limit > 0) {
            sql += " LIMIT :limit";
            params.put("limit", limit);
        }
        return db.query(sql, params);
    }

    /**
     * Download a file from the provider (Telegram) using a stored bot token and a file_id.
     * Returns the raw bytes of the file.
     */
    public byte[] downloadFileByFileId(String botId, String fileId) throws IOException, InterruptedException {
        String token = getTokenForBot(botId);
        if (token == null) throw new IllegalArgumentException("Bot not found");

        String baseUrl = "https://api.telegram.org";
        Map<String, Object> bot = getBotById(botId);
        if (bot != null && bot.get("base_url") != null) baseUrl = String.valueOf(bot.get("base_url"));

        // getFile: {baseUrl}/bot{TOKEN}/getFile?file_id={fileId}
        String apiGet = String.format("%s/bot%s/getFile?file_id=%s", baseUrl, token, java.net.URLEncoder.encode(fileId, StandardCharsets.UTF_8));
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(apiGet)).timeout(Duration.ofSeconds(20)).GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) throw new IOException("getFile failed: " + resp.statusCode());
        String body = resp.body();
        // parse file_path from JSON result (best-effort)
        String filePath = null;
        try {
            String needle = "\"file_path\":\"";
            int p = body.indexOf(needle);
            if (p >= 0) {
                int s = p + needle.length();
                int e = body.indexOf('"', s);
                if (e > s) filePath = body.substring(s, e);
            }
        } catch (Throwable ex) {
            // ignore
        }
        if (filePath == null) throw new IOException("file_path not found in getFile response");

        String fileUrl = String.format("%s/file/bot%s/%s", baseUrl, token, filePath);
        HttpRequest rf = HttpRequest.newBuilder().uri(URI.create(fileUrl)).timeout(Duration.ofSeconds(60)).GET().build();
        HttpResponse<byte[]> r2 = http.send(rf, HttpResponse.BodyHandlers.ofByteArray());
        if (r2.statusCode() != 200) throw new IOException("download failed: " + r2.statusCode());
        return r2.body();
    }

    /**
     * Try to find the original filename for a given file_id from send history.
     * This returns the stored file_url field (for uploads we store the original filename there).
     */
    public String findOriginalFilenameByFileId(String fileId) {
        if (fileId == null || fileId.isEmpty()) return null;
        String sql = "SELECT file_url FROM qaa0_bot_send_history WHERE file_id = :file_id ORDER BY id DESC LIMIT 1";
        Map<String,Object> params = new HashMap<>();
        params.put("file_id", fileId);
        List<Map<String,Object>> rows = db.query(sql, params);
        if (rows == null || rows.isEmpty()) return null;
        Object v = rows.get(0).get("file_url");
        return v != null ? String.valueOf(v) : null;
    }

    /**
     * Send a file by uploading bytes (multipart/form-data) to provider sendDocument endpoint.
     * Accepts the file bytes and filename. Persists send history.
     */
    public String sendFileUpload(String botId, String chatId, byte[] fileBytes, String filename, String caption) throws IOException, InterruptedException {
        String token = getTokenForBot(botId);
        if (token == null) throw new IllegalArgumentException("Bot not found");

        // fetch base_url for this bot
        String baseUrl = "https://api.telegram.org";
        Map<String, Object> bot = getBotById(botId);
        if (bot != null && bot.get("base_url") != null) baseUrl = String.valueOf(bot.get("base_url"));

        String apiUrl = String.format("%s/bot%s/sendDocument", baseUrl, token);

        String boundary = "----DevHubBoundary" + System.currentTimeMillis();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String nl = "\r\n";

        // chat_id field
        String partChat = "--" + boundary + nl +
                "Content-Disposition: form-data; name=\"chat_id\"" + nl + nl +
                java.net.URLEncoder.encode(chatId == null ? "" : chatId, StandardCharsets.UTF_8) + nl;
        baos.write(partChat.getBytes(StandardCharsets.UTF_8));

        // caption field
        if (caption != null && !caption.isEmpty()) {
            String partCap = "--" + boundary + nl +
                    "Content-Disposition: form-data; name=\"caption\"" + nl + nl +
                    caption + nl;
            baos.write(partCap.getBytes(StandardCharsets.UTF_8));
        }

        // file part
        String partFileHeader = "--" + boundary + nl +
                "Content-Disposition: form-data; name=\"document\"; filename=\"" + (filename == null ? "file.bin" : filename) + "\"" + nl +
                "Content-Type: application/octet-stream" + nl + nl;
        baos.write(partFileHeader.getBytes(StandardCharsets.UTF_8));
        baos.write(fileBytes);
        baos.write(nl.getBytes(StandardCharsets.UTF_8));

        // closing boundary
        String closing = "--" + boundary + "--" + nl;
        baos.write(closing.getBytes(StandardCharsets.UTF_8));

        byte[] bodyBytes = baos.toByteArray();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(BodyPublishers.ofByteArray(bodyBytes))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        try {
            persistSendHistory(botId, chatId, filename, caption, resp.body(), resp.statusCode());
        } catch (Exception ex) {
            System.err.println("Failed to persist send history: " + ex.getMessage());
        }
        return resp.body();
    }

    public int deleteSendHistory(long id) {
        String sql = "DELETE FROM qaa0_bot_send_history WHERE id = :id";
        Map<String,Object> params = new HashMap<>();
        params.put("id", id);
        return db.execute(sql, params);
    }
}
