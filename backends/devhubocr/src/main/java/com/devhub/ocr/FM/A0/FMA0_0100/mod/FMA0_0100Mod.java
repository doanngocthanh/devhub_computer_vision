package com.devhub.ocr.FM.A0.FMA0_0100.mod;

import com.devhub.ocr.app.plugins.database.DatabasePlugin;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FMA0_0100Mod {

    private final DatabasePlugin db;

    public FMA0_0100Mod(DatabasePlugin db) {
        this.db = db;
        ensureProfileColumns();
    }

    // ensure optional profile columns exist on users table
    private void ensureProfileColumns() {
        try {
            List<Map<String, Object>> cols = db.query("PRAGMA table_info(users)", null);
            Set<String> names = new HashSet<>();
            for (Map<String, Object> c : cols) names.add(String.valueOf(c.get("name")).toLowerCase());
            List<String> toAdd = new ArrayList<>();
            if (!names.contains("avatar")) toAdd.add("avatar TEXT");
            if (!names.contains("business_name")) toAdd.add("business_name TEXT");
            if (!names.contains("business_id")) toAdd.add("business_id TEXT");
            if (!names.contains("location")) toAdd.add("location TEXT");
            if (!names.contains("public_profile")) toAdd.add("public_profile INTEGER DEFAULT 0");
            for (String ddl : toAdd) {
                try {
                    db.execute("ALTER TABLE users ADD COLUMN " + ddl, null);
                } catch (Exception ex) {
                    // ignore individual failures
                }
            }
        } catch (Exception ex) {
            // ignore
        }
    }

    // compatibility helper used by controller
    public void ensureAvatarColumn() {
        ensureProfileColumns();
    }

    // return raw DB row map for controller compatibility
    public Map<String, Object> getUserById(long userId) {
        List<Map<String, Object>> rows = db.query("SELECT * FROM users WHERE id = :id", Map.of("id", userId));
        if (rows == null || rows.isEmpty()) return Collections.emptyMap();
        return rows.get(0);
    }

    // slim update used by controller
    public boolean updateUserProfile(long userId, String firstName, String lastName, String avatarFilename) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", userId);
        params.put("f", firstName == null ? "" : firstName);
        params.put("l", lastName == null ? "" : lastName);
        if (avatarFilename != null) params.put("a", avatarFilename);
        String sql;
        if (avatarFilename != null) {
            sql = "UPDATE users SET first_name = :f, last_name = :l, avatar = :a WHERE id = :id";
        } else {
            sql = "UPDATE users SET first_name = :f, last_name = :l WHERE id = :id";
        }
        int res = db.execute(sql, params);
        return res > 0;
    }

    public Map<String, Object> getProfile(long userId) {
        // If a separate 'profile' table exists, prefer merged values from profile over users
        boolean hasProfileTable = false;
        try {
            List<Map<String, Object>> t = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='profile'", null);
            hasProfileTable = t != null && !t.isEmpty();
        } catch (Exception ignored) {}

        Map<String, Object> r;
        if (hasProfileTable) {
            String sql = "SELECT u.id AS id, u.email AS email, COALESCE(p.first_name, u.first_name) AS first_name, COALESCE(p.last_name, u.last_name) AS last_name, COALESCE(p.avatar, u.avatar) AS avatar, COALESCE(p.business_name, u.business_name) AS business_name, COALESCE(p.business_id, u.business_id) AS business_id, COALESCE(p.location, u.location) AS location, COALESCE(p.public_profile, u.public_profile, 0) AS public_profile FROM users u LEFT JOIN profile p ON p.user_id = u.id WHERE u.id = :id";
            List<Map<String, Object>> rows = db.query(sql, Map.of("id", userId));
            if (rows == null || rows.isEmpty()) return Collections.emptyMap();
            r = rows.get(0);
        } else {
            List<Map<String, Object>> rows = db.query("SELECT id, email, first_name, last_name, avatar, business_name, business_id, location, public_profile FROM users WHERE id = :id", Map.of("id", userId));
            if (rows == null || rows.isEmpty()) return Collections.emptyMap();
            r = rows.get(0);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("id", r.get("id"));
        out.put("email", r.get("email"));
        out.put("firstName", r.get("first_name"));
        out.put("lastName", r.get("last_name"));
        out.put("avatar", r.get("avatar"));
        out.put("businessName", r.get("business_name"));
        out.put("businessId", r.get("business_id"));
        out.put("location", r.get("location"));
        Object pp = r.get("public_profile");
        boolean publicProfile = false;
        if (pp != null) {
            try { publicProfile = Integer.parseInt(String.valueOf(pp)) != 0; } catch (Exception ignored) {}
        }
        out.put("publicProfile", publicProfile);
        return out;
    }

    public boolean updateProfile(long userId, String firstName, String lastName, String businessName, String businessId, String location, Boolean publicProfile, String avatarFilename) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", userId);
        params.put("f", firstName == null ? "" : firstName);
        params.put("l", lastName == null ? "" : lastName);
        params.put("bn", businessName == null ? "" : businessName);
        params.put("bid", businessId == null ? "" : businessId);
        params.put("loc", location == null ? "" : location);
        params.put("pp", publicProfile == null ? 0 : (publicProfile ? 1 : 0));
        if (avatarFilename != null) params.put("a", avatarFilename);

        // If a profile table exists, upsert into it; otherwise update users table
        boolean hasProfileTable = false;
        try {
            List<Map<String, Object>> t = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='profile'", null);
            hasProfileTable = t != null && !t.isEmpty();
        } catch (Exception ignored) {}

        if (hasProfileTable) {
            // check if profile row exists
            List<Map<String, Object>> found = db.query("SELECT id FROM profile WHERE user_id = :id", Map.of("id", userId));
            if (found != null && !found.isEmpty()) {
                // update
                String sql = "UPDATE profile SET first_name = :f, last_name = :l, business_name = :bn, business_id = :bid, location = :loc, public_profile = :pp" + (avatarFilename != null ? ", avatar = :a" : "") + " WHERE user_id = :id";
                int res = db.execute(sql, params);
                return res > 0;
            } else {
                // insert
                String sql = "INSERT INTO profile(user_id, first_name, last_name, business_name, business_id, location, public_profile" + (avatarFilename != null ? ", avatar" : "") + ") VALUES(:id, :f, :l, :bn, :bid, :loc, :pp" + (avatarFilename != null ? ", :a" : "") + ")";
                int res = db.execute(sql, params);
                return res > 0;
            }
        } else {
            String sql;
            if (avatarFilename != null) {
                sql = "UPDATE users SET first_name = :f, last_name = :l, business_name = :bn, business_id = :bid, location = :loc, public_profile = :pp, avatar = :a WHERE id = :id";
            } else {
                sql = "UPDATE users SET first_name = :f, last_name = :l, business_name = :bn, business_id = :bid, location = :loc, public_profile = :pp WHERE id = :id";
            }
            int res = db.execute(sql, params);
            return res > 0;
        }
    }
}
