package com.devhub.ocr.AA.A0.AAA0_0104.trx;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.devhub.ocr.AA.A0.AAA0_0104.mod.AAA0_0104Mod;
import com.devhub.ocr.app.plugins.database.DatabasePlugin;
import com.devhub.ocr.app.systems.auth.AuthContext;
import com.devhub.ocr.app.systems.auth.UserObject;
import com.devhub.ocr.app.systems.notification.NotificationStreamService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
 
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/AA/A0/AAA0_0104")
public class AAA0_0104 {

    private final AAA0_0104Mod mod;
    private final DatabasePlugin db;
    private final String jwtSecret;
    private final NotificationStreamService streamService;
    private final Logger logger = LoggerFactory.getLogger(AAA0_0104.class);
    private JWTVerifier verifier;
    public AAA0_0104(AAA0_0104Mod mod, DatabasePlugin db, NotificationStreamService streamService, @Value("${devhub.jwt.secret:devhub-secret-do-not-use-in-prod}") String jwtSecret) {
        this.mod = mod;
        this.db = db;
        this.streamService = streamService;
        this.jwtSecret = jwtSecret;
    }

    @PostConstruct
    public void init() {
        try {
            this.verifier = JWT.require(Algorithm.HMAC256(jwtSecret)).build();
        } catch (Exception ex) {
            this.verifier = null;
        }
    }

    @GetMapping("/")
    public String index(Model model, @ModelAttribute("currentUser") UserObject currentUser) {
        model.addAttribute("pageTitle", "Thông báo hệ thống");
        if (currentUser != null) {
            List<Map<String, Object>> notifications = mod.listForUser(currentUser.getId(), 50, 0);
            model.addAttribute("notifications", notifications);
        } else {
            model.addAttribute("notifications", java.util.Collections.emptyList());
        }
        return "html/AA/A0/AAA0_0104/AAA0_0104";
    }
   @GetMapping("/notify/json")
    @ResponseBody
    public List<Map<String, Object>> notifyJson(@RequestParam("userId") Long userId) {
        if (userId != null) {
            return mod.listForUser(userId, 50, 0);
        }
        return Collections.emptyList();
    }

    @GetMapping("/notify")
    @ResponseBody
    public List<Map<String, Object>> notify(@ModelAttribute("currentUser") UserObject currentUser,
                                            @RequestParam(value = "userId", required = false) Long userId,
                                            @RequestHeader(value = "Authorization", required = false) String authorization,
                                            HttpServletRequest request) {
        // 1) current user from AuthContext / ModelAttribute
        if (currentUser != null) {
            return mod.listForUser(currentUser.getId(), 50, 0);
        }

        // 2) explicit userId param
        if (userId != null) {
            return mod.listForUser(userId, 50, 0);
        }

        // 3) try cookie DEVHUB_AUTH or Authorization header Bearer token
        String token = null;
        if (request != null && request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie c : request.getCookies()) {
                if ("DEVHUB_AUTH".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }
        if ((token == null || token.isEmpty()) && authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }
        
        // 4) verify token if present
        try {
            if (token != null && !token.isEmpty() && this.verifier != null) {
                DecodedJWT jwt = verifier.verify(token);
                String email = jwt.getSubject();
                if (email != null && !email.isBlank()) {
                    List<Map<String, Object>> rows = db.query("SELECT id FROM users WHERE email = :e", Map.of("e", email));
                    if (rows != null && !rows.isEmpty()) {
                        Object id = rows.get(0).get("id");
                        if (id != null) {
                            long uid = Long.parseLong(String.valueOf(id));
                            return mod.listForUser(uid, 50, 0);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // ignore and fallthrough to empty
        }

        return Collections.emptyList();
    }

    /**
     * SSE stream for realtime notifications. Prefer passing userId as query param.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter stream(@ModelAttribute("currentUser") UserObject currentUser,
                             @RequestParam(value = "userId", required = false) Long userId) {
        Long uid = null;
        if (currentUser != null) uid = currentUser.getId();
        if (uid == null && userId != null) uid = userId;
        if (uid == null) {
            // return a completed emitter (no stream)
            SseEmitter e = new SseEmitter(0L);
            try { e.send(SseEmitter.event().name("error").data("no-user")); } catch (Exception ignored) {}
            e.complete();
            return e;
        }
        try {
            logger.info("Registering SSE stream for userId={}", uid);
        } catch (Exception ignored) {}
        return streamService.register(uid);
    }

    // @GetMapping("/notify/json")
    // @ResponseBody
    // public List<Map<String, Object>> notify(@ModelAttribute("currentUser") UserObject currentUser) {
    //     if (currentUser != null) {
    //         return mod.listForUser(currentUser.getId(), 50, 0);
    //     }
    //     return java.util.Collections.emptyList();
    // }

    // Mark a single delivery as read (convenience GET redirect)
    @GetMapping("/mark/read")
    public String markRead(@ModelAttribute("currentUser") UserObject currentUser,
                           @RequestParam("deliveryId") Long deliveryId,
                           HttpServletRequest req) {
        if (currentUser != null && deliveryId != null) {
            mod.markAsRead(deliveryId, currentUser.getId());
        }
        String referer = req.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/AA/A0/AAA0_0104/");
    }

    // Mark all as read
    @GetMapping("/mark/all")
    public String markAll(@ModelAttribute("currentUser") UserObject currentUser, HttpServletRequest req) {
        if (currentUser != null) {
            mod.markAllRead(currentUser.getId());
        }
        String referer = req.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/AA/A0/AAA0_0104/");
    }

    // Send a quick test notification to current user
    @GetMapping("/send/test")
    public String sendTest(@ModelAttribute("currentUser") UserObject currentUser, HttpServletRequest req) {
        if (currentUser != null) {
            mod.sendToUser(currentUser.getId(), "Thông báo thử", "Đây là thông báo test.");
        }
        String referer = req.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/AA/A0/AAA0_0104/");
    }

}
