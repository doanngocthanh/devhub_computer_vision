package com.devhub.ocr.auth.mod;

import com.devhub.ocr.app.plugins.database.DatabasePlugin;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RoleService {

    private final DatabasePlugin db;

    public RoleService(DatabasePlugin db) {
        this.db = db;
        initRoleTables();
    }

    private void initRoleTables() {
        // ensure tables exist; migrations are preferred, but code fallback is helpful
        // in dev
    // ensure users table exists (AuthService usually creates it, but RoleService
    // may run earlier during bean construction; create a fallback here to avoid
    // "no such table: users" errors)
    String usersSql = "CREATE TABLE IF NOT EXISTS users (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "email TEXT NOT NULL UNIQUE, " +
        "password_hash TEXT NOT NULL, " +
        "first_name TEXT, " +
        "last_name TEXT, " +
        "created_at TEXT" +
        ")";
    db.execute(usersSql, null);
        String a = "CREATE TABLE IF NOT EXISTS path_roles (id INTEGER PRIMARY KEY AUTOINCREMENT, path TEXT NOT NULL, role TEXT NOT NULL)";
        String b = "CREATE TABLE IF NOT EXISTS users_roles (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, role TEXT NOT NULL)";
        db.execute(a, null);
        db.execute(b, null);
        // roles table to keep canonical roles
        String c = "CREATE TABLE IF NOT EXISTS roles (id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT NOT NULL UNIQUE, description TEXT)";
        db.execute(c, null);

        // ensure default IT role exists
        List<Map<String, Object>> existing = db.query("SELECT role FROM roles WHERE role = :r", Map.of("r", "IT"));
        if (existing == null || existing.isEmpty()) {
            db.execute("INSERT INTO roles(role, description) VALUES(:r, :d)",
                    Map.of("r", "IT", "d", "Highest-privilege administrator"));
        }

        // if no user-role mappings exist yet, try to assign IT to the first user
        // (bootstrap). Wrap queries in try/catch to avoid startup failure if something
        // else is wrong with the DB.
        try {
            List<Map<String, Object>> ur = db.query("SELECT id FROM users_roles LIMIT 1", null);
            if (ur == null || ur.isEmpty()) {
                List<Map<String, Object>> users = db.query("SELECT id FROM users ORDER BY id LIMIT 1", null);
                if (users != null && !users.isEmpty()) {
                    Object uid = users.get(0).get("id");
                    if (uid != null) {
                        db.execute("INSERT INTO users_roles(user_id, role) VALUES(:uid, :r)",
                                Map.of("uid", uid, "r", "IT"));
                    }
                }
            }
        } catch (Exception ex) {
            // Log and continue; failing to bootstrap IT role is non-fatal in many dev setups
            // (migrations or later admin actions can fix roles).
            System.err.println("Warning: could not bootstrap user-role mapping: " + ex.getMessage());
        }
    }

    public List<Map<String, Object>> getAllRoles() {
        List<Map<String, Object>> rows = db.query("SELECT id, role, description FROM roles ORDER BY id", null);
        return rows == null ? Collections.emptyList() : rows;
    }

    public boolean createRole(String role, String description) {
        if (role == null || role.trim().isEmpty())
            return false;
        int res = db.execute("INSERT OR IGNORE INTO roles(role, description) VALUES(:r, :d)",
                Map.of("r", role.trim(), "d", description == null ? "" : description));
        return res > 0;
    }

    public boolean deleteRole(String role) {
        if (role == null || role.trim().isEmpty())
            return false;
        // remove from canonical roles and cleanup references
        db.execute("DELETE FROM users_roles WHERE role = :r", Map.of("r", role));
        db.execute("DELETE FROM path_roles WHERE role = :r", Map.of("r", role));
        int res = db.execute("DELETE FROM roles WHERE role = :r", Map.of("r", role));
        return res > 0;
    }

    public boolean assignRolesToUserByEmail(String email, List<String> roles) {
        if (email == null || email.trim().isEmpty() || roles == null || roles.isEmpty())
            return false;
        List<Map<String, Object>> rows = db.query("SELECT id FROM users WHERE email = :e", Map.of("e", email));
        if (rows == null || rows.isEmpty())
            return false;
        Object id = rows.get(0).get("id");
        int total = 0;
        for (String r : roles) {
            // avoid duplicates
            List<Map<String, Object>> check = db.query("SELECT 1 FROM users_roles WHERE user_id = :uid AND role = :r",
                    Map.of("uid", id, "r", r));
            if (check == null || check.isEmpty()) {
                total += db.execute("INSERT INTO users_roles(user_id, role) VALUES(:uid, :r)",
                        Map.of("uid", id, "r", r));
            }
        }
        return total > 0;
    }

    /**
     * Roles assigned to an exact path key (e.g. AA.A0.AAA0_0100)
     */
    public Set<String> getRolesForPath(String pathKey) {
        List<Map<String, Object>> rows = db.query("SELECT role FROM path_roles WHERE path = :p", Map.of("p", pathKey));
        if (rows == null || rows.isEmpty())
            return Collections.emptySet();
        Set<String> roles = new HashSet<>();
        for (Map<String, Object> r : rows)
            roles.add(String.valueOf(r.get("role")));
        return roles;
    }

    /**
     * Roles assigned to a user identified by email
     */
    public Set<String> getUserRolesByEmail(String email) {
        List<Map<String, Object>> rows = db.query(
                "SELECT ur.role FROM users_roles ur JOIN users u ON ur.user_id = u.id WHERE u.email = :e",
                Map.of("e", email));
        if (rows == null || rows.isEmpty())
            return Collections.emptySet();
        Set<String> roles = new HashSet<>();
        for (Map<String, Object> r : rows)
            roles.add(String.valueOf(r.get("role")));
        return roles;
    }

    public List<Map<String, Object>> getAllUsers() {
        List<Map<String, Object>> rows = db.query("SELECT id, email, first_name, last_name FROM users ORDER BY id DESC",
                null);
        return rows == null ? Collections.emptyList() : rows;
    }

    public boolean addRoleToPath(String path, String role) {
        int res = db.execute("INSERT INTO path_roles(path, role) VALUES(:p, :r)", Map.of("p", path, "r", role));
        return res > 0;
    }

    public boolean removeRoleFromPath(String path, String role) {
        int res = db.execute("DELETE FROM path_roles WHERE path = :p AND role = :r", Map.of("p", path, "r", role));
        return res > 0;
    }

    public boolean assignRoleToUserByEmail(String email, String role) {
        // find user id
        List<Map<String, Object>> rows = db.query("SELECT id FROM users WHERE email = :e", Map.of("e", email));
        if (rows == null || rows.isEmpty())
            return false;
        Object id = rows.get(0).get("id");
        int res = db.execute("INSERT INTO users_roles(user_id, role) VALUES(:uid, :r)", Map.of("uid", id, "r", role));
        return res > 0;
    }

    public boolean removeRoleFromUserByEmail(String email, String role) {
        List<Map<String, Object>> rows = db.query("SELECT id FROM users WHERE email = :e", Map.of("e", email));
        if (rows == null || rows.isEmpty())
            return false;
        Object id = rows.get(0).get("id");
        int res = db.execute("DELETE FROM users_roles WHERE user_id = :uid AND role = :r",
                Map.of("uid", id, "r", role));
        return res > 0;
    }

    /**
     * Replace roles assigned to a path with the provided set (transactional-ish).
     * Returns true when applied.
     */
    public boolean setRolesForPath(String path, Set<String> roles) {
        if (path == null || path.trim().isEmpty())
            return false;
        db.execute("DELETE FROM path_roles WHERE path = :p", Map.of("p", path));
        if (roles == null || roles.isEmpty())
            return true;
        int total = 0;
        for (String r : roles) {
            total += db.execute("INSERT INTO path_roles(path, role) VALUES(:p, :r)", Map.of("p", path, "r", r));
        }
        return total >= 0;
    }

    /** Replace roles assigned to a user (by email) with the provided set. */
    public boolean setRolesForUserByEmail(String email, Set<String> roles) {
        List<Map<String, Object>> rows = db.query("SELECT id FROM users WHERE email = :e", Map.of("e", email));
        if (rows == null || rows.isEmpty())
            return false;
        Object id = rows.get(0).get("id");
        db.execute("DELETE FROM users_roles WHERE user_id = :uid", Map.of("uid", id));
        if (roles == null || roles.isEmpty())
            return true;
        for (String r : roles) {
            db.execute("INSERT INTO users_roles(user_id, role) VALUES(:uid, :r)", Map.of("uid", id, "r", r));
        }
        return true;
    }

}
