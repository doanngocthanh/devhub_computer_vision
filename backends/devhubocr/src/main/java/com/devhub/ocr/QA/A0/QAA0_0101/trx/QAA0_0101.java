package com.devhub.ocr.QA.A0.QAA0_0101.trx;

import com.devhub.ocr.QA.A0.QAA0_0101.mod.QAA0_0101BotService;
import com.devhub.ocr.QA.A0.QAA0_0100.mod.QAA0_1000Service;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/QA/A0/QAA0_0101")
public class QAA0_0101 {

    private final QAA0_0101BotService botService;
    private final QAA0_1000Service savedBotService;

    public QAA0_0101(QAA0_0101BotService botService, QAA0_1000Service savedBotService) {
        this.botService = botService;
        this.savedBotService = savedBotService;
    }

    @PostMapping("/bot/create")
    @ResponseBody
    public ResponseEntity<?> createBot(@RequestParam String botId,
                                       @RequestParam String token,
                                       @RequestParam(required = false) String baseUrl,
                                       @RequestParam(required = false) String callbackUrl,
                                       @RequestParam(required = false) String description) {
        try {
            int res = botService.createBot(botId, token, baseUrl, callbackUrl, description);
            return ResponseEntity.ok(Map.of("result", res));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/bots")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> listBots() {
        List<Map<String, Object>> rows = botService.listBots();
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/history")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> listHistory(@RequestParam(required = false) String botId,
                                                                 @RequestParam(required = false) Integer limit) {
        List<Map<String, Object>> rows = botService.listSendHistory(botId, limit != null ? limit : 50);
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/download-file")
    public ResponseEntity<byte[]> downloadFile(@RequestParam String botId, @RequestParam String fileId,
                                               @RequestParam(required = false) String filename) {
        try {
            byte[] data = botService.downloadFileByFileId(botId, fileId);
            // If filename not provided, attempt to find original filename from history
            String fn = filename;
            if (fn == null || fn.isEmpty()) {
                try { fn = botService.findOriginalFilenameByFileId(fileId); } catch (Exception ignored) {}
            }
            if (fn == null || fn.isEmpty()) fn = fileId + ".bin";
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fn);
            return org.springframework.http.ResponseEntity.ok().headers(headers).body(data);
        } catch (IllegalArgumentException ia) {
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            return ResponseEntity.status(500).build();
        }
    }



    @GetMapping("/ui")
    public String ui() {
        // Thymeleaf template will be located at templates/html/QA/A0/QAA0_0101/QAA0_0101.html
        return "html/QA/A0/QAA0_0101/QAA0_0101";
    }

    @GetMapping("/saved-bots")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> savedBots() {
        // return bots saved by the user in the older QAA0_0100 module
        try {
            com.devhub.ocr.app.systems.auth.UserObject u = com.devhub.ocr.app.systems.auth.AuthContext.get();
            if (u == null) return ResponseEntity.ok(java.util.Collections.emptyList());
            com.devhub.ocr.QA.A0.QAA0_0100.mod.QAA0_1000 mod = savedBotService.loadForUser(u.getId());
            java.util.List<com.devhub.ocr.QA.A0.QAA0_0100.mod.QAA0_1000.BotConfig> list = mod.listBots();
            java.util.ArrayList<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
            for (com.devhub.ocr.QA.A0.QAA0_0100.mod.QAA0_1000.BotConfig b : list) {
                java.util.Map<String,Object> m = new java.util.HashMap<>();
                m.put("id", b.id != null ? b.id.toString() : "");
                m.put("name", b.name);
                m.put("token", b.token);
                m.put("chatId", b.chatId);
                m.put("description", b.description);
                m.put("enabled", b.enabled ? 1 : 0);
                out.add(m);
            }
            return ResponseEntity.ok(out);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(java.util.Collections.emptyList());
        }
    }

    /**
     * Send a file by URL using configured bot. The token is kept internal and never returned.
     * Params: botId, chatId, fileUrl, caption (optional)
     */
    @PostMapping("/send-file")
    @ResponseBody
    public ResponseEntity<?> sendFile(@RequestParam String botId,
                                      @RequestParam String chatId,
                                      @RequestParam String fileUrl,
                                      @RequestParam(required = false) String caption) {
        try {
            String resp = botService.sendFileByUrl(botId, chatId, fileUrl, caption);
            return ResponseEntity.ok(Map.of("ok", true, "response", resp));
        } catch (IllegalArgumentException ia) {
            return ResponseEntity.badRequest().body(Map.of("error", ia.getMessage()));
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Accept a direct file upload (multipart) and send via configured bot.
     */
    @PostMapping("/send-file/upload")
    @ResponseBody
    public ResponseEntity<?> sendFileUpload(@RequestParam String botId,
                                            @RequestParam String chatId,
                                            @RequestParam(required = false) String caption,
                                            @RequestPart(required = false) MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded"));
        }
        try {
            byte[] bytes = file.getBytes();
            String resp = botService.sendFileUpload(botId, chatId, bytes, file.getOriginalFilename(), caption);
            return ResponseEntity.ok(Map.of("ok", true, "response", resp));
        } catch (IllegalArgumentException ia) {
            return ResponseEntity.badRequest().body(Map.of("error", ia.getMessage()));
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/history/delete")
    @ResponseBody
    public ResponseEntity<?> deleteHistory(@RequestParam long id) {
        try {
            int r = botService.deleteSendHistory(id);
            return ResponseEntity.ok(Map.of("deleted", r));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }

}
