package com.devhub.ocr.auth.trx;

import com.devhub.ocr.auth.mod.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/auth")
public class auth {
    private final AuthService authService;

    public auth(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/sign-in")
    public String signIn() {
        return "html/auth/sign-in";
    }

    @GetMapping("/sign-up")
    public String signUp() {
        return "html/auth/sign-up";
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

    @PostMapping("/login")
    public String login(@RequestParam String email,
            @RequestParam String password,
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
}