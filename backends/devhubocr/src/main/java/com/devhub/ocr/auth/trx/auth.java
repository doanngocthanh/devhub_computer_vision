package com.devhub.ocr.auth.trx;

import com.devhub.ocr.auth.mod.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.devhub.ocr.AA.A0.AAA0_0104.mod.AAA0_0104Mod;
import com.devhub.ocr.app.systems.auth.UserObject;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/auth")
public class auth {
    private final AuthService authService;
    private final AAA0_0104Mod notificationMod;
    private final Logger logger = LoggerFactory.getLogger(auth.class);

    public auth(AuthService authService, AAA0_0104Mod notificationMod) {
        this.authService = authService;
        this.notificationMod = notificationMod;
    }

    @GetMapping("/sign-in")
    public String signIn() {
        return "html/auth/sign-in";
    }

    @GetMapping("/sign-up")
    public String signUp() {
        return "html/auth/sign-up";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
            @RequestParam String password,
            HttpServletRequest request,
            HttpServletResponse response) {
        String token = authService.authenticate(email, password);
        if (token == null) {
            return "redirect:/auth/sign-in?error=invalid";
        }

        Cookie c = new Cookie("DEVHUB_AUTH", token);
        c.setHttpOnly(true);
        c.setPath("/");
        // let browser manage cookie expiry; set max age to a reasonable default (7
        // days)
        c.setMaxAge(7 * 24 * 3600);
        response.addCookie(c);

        // build a minimal UserObject for notification purposes and capture IP
        Long uid = authService.getIdByEmail(email);
        if (uid != null) {
            UserObject userObject = new UserObject(uid, email);
            // prefer X-Forwarded-For / X-Real-IP when behind proxies
            String ip = null;
            try {
                ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isBlank()) ip = request.getHeader("X-Real-IP");
                if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
                // if multiple IPs present, take the first
                if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
            } catch (Exception e) {
                ip = request.getRemoteAddr();
            }
            userObject.setLastLoginIp(ip);
            try {
                // pass primitive long to avoid accidental unboxing issues
                boolean ok = notificationMod.sendToUser(uid.longValue(), "[Login] Thông báo đăng nhập",
                        "Đăng nhập thành công từ ip: " + userObject.getLastLoginIp());
                if (ok) {
                    logger.info("Login notification sent to user ID {}", uid.longValue());
                } else {
                    logger.warn("Login notification failed to persist for user ID {}", uid.longValue());
                }
            } catch (Exception e) {
                logger.error("Exception while sending login notification to user ID {}", uid.longValue(), e);
            }
        }
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie c = new Cookie("DEVHUB_AUTH", "");
        c.setHttpOnly(true);
        c.setPath("/");
        c.setMaxAge(0); // delete cookie
        response.addCookie(c);
        return "redirect:/auth/sign-in";
    }

    @PostMapping("/register")
    public String register(@RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName) {
        boolean ok = authService.register(email, password, firstName, lastName);
        if (!ok) {
            return "redirect:/auth/sign-up?error=exists";
        }
        return "redirect:/auth/sign-in?registered=1";
    }

}