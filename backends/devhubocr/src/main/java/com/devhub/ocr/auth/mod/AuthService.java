package com.devhub.ocr.auth.mod;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.devhub.ocr.app.plugins.database.DatabasePlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final DatabasePlugin db;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Algorithm jwtAlg;
    private final long jwtExpirySeconds;

    public AuthService(DatabasePlugin db,
            @Value("${devhub.jwt.secret:devhub-secret-do-not-use-in-prod}") String jwtSecret,
            @Value("${devhub.jwt.expiry.seconds:604800}") long jwtExpirySeconds) {
        this.db = db;
        this.jwtAlg = Algorithm.HMAC256(jwtSecret);
        this.jwtExpirySeconds = jwtExpirySeconds;

        // ensure users table exists
        initUsersTable();
    }

    private void initUsersTable() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "email TEXT NOT NULL UNIQUE, " +
                "password_hash TEXT NOT NULL, " +
                "first_name TEXT, " +
                "last_name TEXT, " +
                "created_at TEXT" +
                ")";
        db.execute(sql, null);
    }

    public boolean register(String email, String rawPassword, String firstName, String lastName) {
        // check exists
        List<Map<String, Object>> rows = db.query("SELECT id FROM users WHERE email = :e", Map.of("e", email));
        if (rows != null && !rows.isEmpty())
            return false;

        String hash = passwordEncoder.encode(rawPassword);
        int res = db.execute(
                "INSERT INTO users(email, password_hash, first_name, last_name, created_at) VALUES(:e, :p, :f, :l, :c)",
                Map.of("e", email, "p", hash, "f", firstName, "l", lastName, "c", Instant.now().toString()));
        return res > 0;
    }

    public String authenticate(String email, String rawPassword) {
        List<Map<String, Object>> rows = db.query("SELECT id, password_hash FROM users WHERE email = :e",
                Map.of("e", email));
        if (rows == null || rows.isEmpty())
            return null;
        Map<String, Object> row = rows.get(0);
        String hash = String.valueOf(row.get("password_hash"));
        if (!passwordEncoder.matches(rawPassword, hash))
            return null;

        // generate JWT
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date expiresAt = Date.from(now.plus(jwtExpirySeconds, ChronoUnit.SECONDS));

        String token = JWT.create()
                .withSubject(email)
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiresAt)
                .sign(jwtAlg);
        return token;
    }

    public Long getIdByEmail(String email) {
        List<Map<String, Object>> rows = db.query("SELECT id FROM users WHERE email = :e", Map.of("e", email));
        if (rows == null || rows.isEmpty()) return null;
        Map<String, Object> row = rows.get(0);
        try {
            return Long.parseLong(String.valueOf(row.get("id")));
        } catch (Exception ex) { return null; }
    }

    public int getIdFromTokenString(String token) {
        List<Map<String, Object>> rows = db.query("SELECT id FROM users WHERE email = :e", Map.of("e", token));
        if (rows == null || rows.isEmpty())
            return -1;
        Map<String, Object> row = rows.get(0);
        return Integer.parseInt(String.valueOf(row.get("id")));
    }
}
