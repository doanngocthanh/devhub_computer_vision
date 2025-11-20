package com.devhub.ocr.QA.A0.QAA0_0100.trx;

import com.devhub.ocr.app.systems.auth.AuthContext;
import com.devhub.ocr.app.systems.auth.UserObject;
import com.devhub.ocr.app.systems.menu.AutoMenu;
import com.devhub.ocr.QA.A0.QAA0_0100.mod.QAA0_1000;
import com.devhub.ocr.QA.A0.QAA0_0100.mod.QAA0_1000Service;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.UUID;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;

import com.devhub.ocr.QA.A0.QAA0_0100.mod.QAA0_1000ContactsService;

@Controller
@RequestMapping("/QA/A0/QAA0_0100")
// <i class="fa-brands fa-telegram"></i>
// @AutoMenu(title = "Telegram Bots", icon = "fa-brands fa-telegram", path = "/QA/A0/QAA0_0100/", roles = {}, parentId = 1)
public class QAA0_0100 {

    private final QAA0_1000Service service;
    private final QAA0_1000ContactsService contactsService;

    public QAA0_0100(QAA0_1000Service service, QAA0_1000ContactsService contactsService) {
        this.service = service;
        this.contactsService = contactsService;
    }

    @GetMapping({ "", "/" })
    public String index(Model model) {
        UserObject u = AuthContext.get();
        if (u == null)
            return "redirect:/auth/sign-in";

        try {
            QAA0_1000 mod = service.loadForUser(u.getId());
            model.addAttribute("bots", mod.listBots());
            model.addAttribute("user", u);
            model.addAttribute("pageTitle", "Telegram Bots");
            return "html/QA/A0/QAA0_0100/QAA0_0100";
        } catch (IOException ex) {
            model.addAttribute("error", "Không thể load cấu hình bots: " + ex.getMessage());
            model.addAttribute("bots", java.util.Collections.emptyList());
            model.addAttribute("user", u);
            model.addAttribute("pageTitle", "Telegram Bots");
            return "html/QA/A0/QAA0_0100/QAA0_0100";
        }
    }

    @PostMapping("/add")
    public String addBot(@RequestParam String name,
            @RequestParam String token,
            @RequestParam String chatId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String enabled) {
        UserObject u = AuthContext.get();
        if (u == null)
            return "redirect:/auth/sign-in";

        try {
            QAA0_1000 mod = service.loadForUser(u.getId());
            QAA0_1000.BotConfig b = new QAA0_1000.BotConfig(name, token, chatId, enabled != null, description);
            mod.addBot(b);
            service.saveForUser(u.getId(), mod);
        } catch (IOException ex) {
            // swallow - redirect with error query param might be added later
        }
        return "redirect:/QA/A0/QAA0_0100/";
    }

    @PostMapping("/update")
    public String updateBot(@RequestParam String id,
                            @RequestParam String name,
                            @RequestParam String token,
                            @RequestParam String chatId,
                            @RequestParam(required = false) String description,
                            @RequestParam(required = false) String enabled) {
        UserObject u = AuthContext.get();
        if (u == null)
            return "redirect:/auth/sign-in";

        try {
            QAA0_1000 mod = service.loadForUser(u.getId());
            try {
                UUID uuid = UUID.fromString(id);
                java.util.Optional<QAA0_1000.BotConfig> maybe = mod.findById(uuid);
                if (maybe.isPresent()) {
                    QAA0_1000.BotConfig b = maybe.get();
                    b.name = name;
                    b.token = token;
                    b.chatId = chatId;
                    b.description = description;
                    b.enabled = (enabled != null);
                    mod.updateBot(b);
                    service.saveForUser(u.getId(), mod);
                }
            } catch (IllegalArgumentException ignored) {
            }
        } catch (IOException ex) {
            // ignore
        }
        return "redirect:/QA/A0/QAA0_0100/";
    }

    @GetMapping("/contacts")
    @ResponseBody
    public List<QAA0_1000ContactsService.Contact> listContacts() {
        UserObject u = AuthContext.get();
        if (u == null) return java.util.Collections.emptyList();
        try {
            return contactsService.loadForUser(u.getId());
        } catch (IOException ex) {
            return java.util.Collections.emptyList();
        }
    }

    @PostMapping("/delete")
    public String deleteBot(@RequestParam String id) {
        UserObject u = AuthContext.get();
        if (u == null)
            return "redirect:/auth/sign-in";

        try {
            QAA0_1000 mod = service.loadForUser(u.getId());
            try {
                UUID uuid = UUID.fromString(id);
                mod.removeBot(uuid);
                service.saveForUser(u.getId(), mod);
            } catch (IllegalArgumentException ignored) {
            }
        } catch (IOException ex) {
            // ignore
        }
        return "redirect:/QA/A0/QAA0_0100/";
    }

    @PostMapping("/test/send")
    public String sendTest(@RequestParam String token,
            @RequestParam String chatId,
            @RequestParam(required = false) String message,
            Model model) {
        UserObject u = AuthContext.get();
        if (u == null)
            return "redirect:/auth/sign-in";
        if (message == null || message.isBlank())
            message = "Test message from DevHub OCR";

        String result;
        try {
            String form = "chat_id=" + URLEncoder.encode(chatId, StandardCharsets.UTF_8)
                    + "&text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
            String url = "https://api.telegram.org/bot" + token + "/sendMessage";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            result = "HTTP:" + resp.statusCode() + " - " + resp.body();
        } catch (Exception ex) {
            result = "Error: " + ex.getMessage();
        }

        // reload bots and return page with result
        try {
            QAA0_1000 mod = service.loadForUser(u.getId());
            model.addAttribute("bots", mod.listBots());
        } catch (IOException ex) {
            model.addAttribute("bots", java.util.Collections.emptyList());
        }
        model.addAttribute("user", u);
        model.addAttribute("pageTitle", "Telegram Bots");
        model.addAttribute("testResult", result);
        return "html/QA/A0/QAA0_0100/QAA0_0100";
    }

    @PostMapping("/test/wait")
    public String waitForMessage(@RequestParam String token,
            @RequestParam String chatId,
            @RequestParam(required = false, defaultValue = "20") int timeoutSeconds,
            Model model) {
        UserObject u = AuthContext.get();
        if (u == null)
            return "redirect:/auth/sign-in";

        String waitResult = "No message received within timeout";
        try {
            HttpClient client = HttpClient.newHttpClient();
            int offset = 0;
            long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
            Pattern chatIdPattern = Pattern.compile("\"chat\"\\s*:\\s*\\{[^}]*\\\"id\\\"\\s*:\\s*([\\-0-9]+)");
            Pattern updateIdPattern = Pattern.compile("\"update_id\"\\s*:\\s*(\\d+)");
            while (System.currentTimeMillis() < deadline) {
                String url = "https://api.telegram.org/bot" + token + "/getUpdates?timeout=2"
                        + (offset > 0 ? "&offset=" + offset : "");
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                String body = resp.body();

                // update offset to avoid receiving same updates again
                Matcher mu = updateIdPattern.matcher(body);
                int maxUpdate = -1;
                while (mu.find()) {
                    try {
                        int v = Integer.parseInt(mu.group(1));
                        if (v > maxUpdate)
                            maxUpdate = v;
                    } catch (Exception ignored) {
                    }
                }
                if (maxUpdate >= 0)
                    offset = maxUpdate + 1;

                // check for chat id in payload
                Matcher m = chatIdPattern.matcher(body);
                while (m.find()) {
                    String found = m.group(1);
                    if (found.equals(chatId)) {
                        waitResult = "Received message from chatId=" + chatId;
                        // optionally extract text snippet
                        int idx = body.indexOf(found);
                        int start = Math.max(0, idx - 80);
                        int end = Math.min(body.length(), idx + 200);
                        String snippet = body.substring(start, end).replaceAll("\\s+", " ");
                        waitResult += ": ..." + snippet + "...";
                        break;
                    }
                }
                if (!waitResult.startsWith("No message") && waitResult.contains("Received"))
                    break;
                // sleep briefly to avoid tight loop
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        } catch (Exception ex) {
            waitResult = "Error while waiting: " + ex.getMessage();
        }

        // reload bots and return page with waitResult
        try {
            QAA0_1000 mod = service.loadForUser(u.getId());
            model.addAttribute("bots", mod.listBots());
        } catch (IOException ex) {
            model.addAttribute("bots", java.util.Collections.emptyList());
        }
        model.addAttribute("user", u);
        model.addAttribute("pageTitle", "Telegram Bots");
        model.addAttribute("waitResult", waitResult);
        return "html/QA/A0/QAA0_0100/QAA0_0100";
    }

    // SSE streaming endpoint: client opens EventSource to receive real-time polling updates
    @GetMapping("/test/stream")
    public SseEmitter streamWait(@RequestParam String token,
                                 @RequestParam String chatId,
                                 @RequestParam(required = false, defaultValue = "60") int timeoutSeconds) {
        UserObject u = AuthContext.get();
        Long currentUserId = u != null ? u.getId() : null;
        long timeoutMs = Math.max(10000, timeoutSeconds * 1000L + 5000);
        SseEmitter emitter = new SseEmitter(timeoutMs);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.submit(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                int offset = 0;
                long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
                Pattern chatIdPattern = Pattern.compile("\"chat\"\\s*:\\s*\\{[^}]*\\\"id\\\"\\s*:\\s*([\\-0-9]+)");
                Pattern updateIdPattern = Pattern.compile("\"update_id\"\\s*:\\s*(\\d+)");

                emitter.send("START: waiting for messages for chatId=" + chatId + " (timeout=" + timeoutSeconds + "s)");

                while (System.currentTimeMillis() < deadline) {
                    String url = "https://api.telegram.org/bot" + token + "/getUpdates?timeout=2" + (offset > 0 ? "&offset=" + offset : "");
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    String body = resp.body();

                    // send raw response status for debugging
                    emitter.send("HTTP: " + resp.statusCode());

                    // update offset
                    Matcher mu = updateIdPattern.matcher(body);
                    int maxUpdate = -1;
                    while (mu.find()) {
                        try { int v = Integer.parseInt(mu.group(1)); if (v > maxUpdate) maxUpdate = v; } catch (Exception ignored) {}
                    }
                    if (maxUpdate >= 0) {
                        offset = maxUpdate + 1;
                        emitter.send("offset -> " + offset);
                    }

                    // check for chat id in payload
                    Matcher m = chatIdPattern.matcher(body);
                    boolean found = false;
                    while (m.find()) {
                        String foundId = m.group(1);
                        emitter.send("found chat id in updates: " + foundId);
                        // try to extract sender info and persist contact (if available)
                        try {
                            if (currentUserId != null) {
                                java.util.regex.Pattern fromBlock = java.util.regex.Pattern.compile("\\\"from\\\"\\s*:\\s*\\{([^}]*)\\}");
                                java.util.regex.Matcher mf = fromBlock.matcher(body);
                                if (mf.find()) {
                                    String inner = mf.group(1);
                                    java.util.regex.Pattern idP = java.util.regex.Pattern.compile("\\\"id\\\"\\s*:\\s*([0-9]+)");
                                    java.util.regex.Pattern fnP = java.util.regex.Pattern.compile("\\\"first_name\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
                                    java.util.regex.Pattern lnP = java.util.regex.Pattern.compile("\\\"last_name\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
                                    java.util.regex.Pattern unP = java.util.regex.Pattern.compile("\\\"username\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
                                    java.util.regex.Matcher idm = idP.matcher(inner);
                                    java.util.regex.Matcher fnm = fnP.matcher(inner);
                                    java.util.regex.Matcher lnm = lnP.matcher(inner);
                                    java.util.regex.Matcher unm = unP.matcher(inner);
                                    String senderId = null, fn = null, ln = null, un = null;
                                    if (idm.find()) senderId = idm.group(1);
                                    if (fnm.find()) fn = fnm.group(1);
                                    if (lnm.find()) ln = lnm.group(1);
                                    if (unm.find()) un = unm.group(1);
                                    if (senderId != null) {
                                        com.devhub.ocr.QA.A0.QAA0_0100.mod.QAA0_1000ContactsService.Contact c = new com.devhub.ocr.QA.A0.QAA0_0100.mod.QAA0_1000ContactsService.Contact(senderId, fn, ln, un, null);
                                        try { contactsService.addOrUpdateContact(currentUserId, c); } catch (Exception ex) { /* ignore persist errors */ }
                                    }
                                }
                            }
                        } catch (Exception ex) { /* ignore */ }
                        if (foundId.equals(chatId)) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        emitter.send("RECEIVED: message from chatId=" + chatId);
                        emitter.complete();
                        return;
                    }

                    // sleep briefly between polls
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }

                emitter.send("TIMEOUT: no message within " + timeoutSeconds + "s");
                emitter.complete();
            } catch (Exception ex) {
                try { emitter.send("ERROR: " + ex.getMessage()); } catch (Exception ignored) {}
                emitter.completeWithError(ex);
            } finally {
                exec.shutdownNow();
            }
        });
        return emitter;
    }

}
