package com.devhub.ocr.app.systems.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Interceptor that watches for a `theme` query parameter (e.g. ?theme=dark or ?theme=light).
 * If present it sets a cookie `DEVHUB_THEME` and redirects to the same URL without the parameter.
 */
public class ThemeInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String theme = request.getParameter("theme");
        if (theme == null || theme.isBlank()) {
            return true;
        }

        // Only accept known values to avoid arbitrary cookie values
        theme = theme.trim().toLowerCase();
        if (!"dark".equals(theme) && !"light".equals(theme)) {
            return true;
        }

        // Set cookie (client-visible, so JS can read it if needed)
        Cookie cookie = new Cookie("DEVHUB_THEME", theme);
        cookie.setPath("/");
        // persist for 90 days
        cookie.setMaxAge(60 * 60 * 24 * 90);
        // not HttpOnly so frontend can inspect if necessary
        response.addCookie(cookie);

        // Build redirect URL without 'theme' query param
        String requestURI = request.getRequestURI();
        String qs = request.getQueryString();
        String redirectTo = requestURI;
        if (qs != null) {
            String[] parts = qs.split("&");
            List<String> keep = new ArrayList<>();
            for (String p : parts) {
                if (p == null || p.isEmpty()) continue;
                String lower = p.toLowerCase();
                if (lower.startsWith("theme=")) continue;
                keep.add(p);
            }
            if (!keep.isEmpty()) {
                redirectTo = requestURI + "?" + String.join("&", keep);
            }
        }

        // Use a relative redirect. Preserve fragment is not possible here.
        response.sendRedirect(redirectTo);
        return false;
    }
}
