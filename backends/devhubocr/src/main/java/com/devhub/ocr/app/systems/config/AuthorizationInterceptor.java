package com.devhub.ocr.app.systems.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.devhub.ocr.auth.mod.RoleService;
import com.devhub.ocr.app.plugins.database.DatabasePlugin;
import com.devhub.ocr.app.systems.auth.UserObject;
import com.devhub.ocr.app.systems.auth.AuthContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    private final RoleService roleService;
    private final JWTVerifier verifier;
    private final DatabasePlugin db;

    public AuthorizationInterceptor(RoleService roleService,
                                    DatabasePlugin db,
                                    @Value("${devhub.jwt.secret:devhub-secret-do-not-use-in-prod}") String jwtSecret) {
        this.roleService = roleService;
        this.db = db;
        this.verifier = JWT.require(Algorithm.HMAC256(jwtSecret)).build();
    }

    private Optional<String> extractTokenFromRequest(HttpServletRequest req) {
        // 1) cookie DEVHUB_AUTH
        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if ("DEVHUB_AUTH".equals(c.getName())) return Optional.ofNullable(c.getValue());
            }
        }
        // 2) Authorization: Bearer <token>
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) return Optional.of(h.substring(7));
        return Optional.empty();
    }

    private void deny(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String accept = Optional.ofNullable(request.getHeader("Accept")).orElse("");
        if (accept.contains("application/json") || "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"unauthorized\"}");
        } else {
            String to = "/auth/sign-in";
            String q = "?next=" + URLEncoder.encode(request.getRequestURI(), StandardCharsets.UTF_8);
            response.sendRedirect(to + q);
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String ctx = Optional.ofNullable(request.getContextPath()).orElse("");
        if (!ctx.isEmpty() && uri.startsWith(ctx)) uri = uri.substring(ctx.length());

        // exclude public resources and auth endpoints
        if (uri.startsWith("/css") || uri.startsWith("/js") || uri.startsWith("/images") || uri.startsWith("/auth") || uri.startsWith("/uploads") || uri.startsWith("/error") || uri.equals("/")) {
            return true;
        }

        // build tokens
        String cleaned = uri;
        if (cleaned.endsWith("/")) cleaned = cleaned.substring(0, cleaned.length()-1);
        if (cleaned.startsWith("/")) cleaned = cleaned.substring(1);
        String[] parts = cleaned.isEmpty() ? new String[0] : cleaned.split("/");
        // try to decode token early to allow IT users to bypass checks
        Optional<String> tokEarly = extractTokenFromRequest(request);
        Set<String> userRoles = Collections.emptySet();
        String email = null;
        if (tokEarly.isPresent()) {
            try {
                DecodedJWT jwt = verifier.verify(tokEarly.get());
                email = jwt.getSubject();
                if (email != null && !email.isEmpty()) {
                    userRoles = roleService.getUserRolesByEmail(email);
                    // highest privilege: allow IT to bypass
                    if (userRoles.contains("IT")) {
                        // also populate current user context for convenience
                        tryPopulateUser(email, request);
                        return true;
                    }
                }
            } catch (JWTVerificationException ex) {
                // ignore here; will be handled later if needed
            }
        }

        // generate candidate path keys from most specific to least
        Set<String> rolesForPath = Collections.emptySet();
        boolean found = false;
        for (int i = parts.length; i >= 1; i--) {
            String key = String.join(".", Arrays.copyOfRange(parts, 0, i));
            Set<String> r = roleService.getRolesForPath(key);
            if (r != null && !r.isEmpty()) {
                rolesForPath = r;
                found = true;
                break;
            }
        }

        if (!found) {
            // If no roles assigned for this path, allow access to AA/A0/* by default
            // (developer-area pages). Otherwise deny.
            if (parts.length >= 2 && "AA".equals(parts[0]) && "A0".equals(parts[1])) {
                return true;
            }
            // no roles assigned for this path -> deny
            deny(request, response);
            return false;
        }

        // if RLZZANY is assigned to this path, allow everyone
        if (rolesForPath.contains("RLZZANY")) return true;

        // otherwise require authentication and matching role
        Optional<String> tokOpt = extractTokenFromRequest(request);
        if (tokOpt.isEmpty()) {
            deny(request, response);
            return false;
        }

        String token = tokOpt.get();
        try {
            DecodedJWT jwt = verifier.verify(token);
            email = jwt.getSubject();
            if (email == null || email.isEmpty()) throw new JWTVerificationException("no subject");
            // populate current user after a successful full verification
            tryPopulateUser(email, request);
        } catch (JWTVerificationException ex) {
            deny(request, response);
            return false;
        }

        if (userRoles == null || userRoles.isEmpty()) {
            userRoles = roleService.getUserRolesByEmail(email);
        }
        for (String r : userRoles) {
            if (rolesForPath.contains(r)) return true;
        }

        // not allowed
        deny(request, response);
        return false;
    }

    private void tryPopulateUser(String email, HttpServletRequest request) {
        try {
            if (email == null || email.isEmpty()) return;
            // load user row
            List<Map<String, Object>> rows = db.query("SELECT id, email, first_name, last_name, created_at FROM users WHERE email = :e", Map.of("e", email));
            if (rows != null && !rows.isEmpty()) {
                Map<String, Object> row = rows.get(0);
                UserObject u = UserObject.fromMap(row);
                // attach roles from RoleService
                Set<String> roles = roleService.getUserRolesByEmail(email);
                u.setRoles(roles);
                // set into thread-local and request attribute
                AuthContext.set(u);
                request.setAttribute("currentUser", u);
            }
        } catch (Exception ex) {
            // don't fail the request due to user-loading problems
            System.err.println("Warning: could not populate current user: " + ex.getMessage());
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // clear thread-local to avoid leaks
        AuthContext.clear();
    }
}
