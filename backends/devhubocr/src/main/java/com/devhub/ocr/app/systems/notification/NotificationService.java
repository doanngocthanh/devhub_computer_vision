package com.devhub.ocr.app.systems.notification;

import com.devhub.ocr.app.plugins.database.DatabasePlugin;
import com.devhub.ocr.auth.mod.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class NotificationService {

    private final DatabasePlugin db;
    private final RoleService roleService;
    private final NotificationStreamService streamService;
    private final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public NotificationService(DatabasePlugin db, RoleService roleService, NotificationStreamService streamService) {
        this.db = db;
        this.roleService = roleService;
        this.streamService = streamService;
        ensureTables();
    }

    private void ensureTables() {
        try {
            db.execute(
                    "CREATE TABLE IF NOT EXISTS notifications (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, message TEXT NOT NULL, data TEXT, created_at TEXT NOT NULL, actor_id INTEGER)",
                    null);
            db.execute(
                    "CREATE TABLE IF NOT EXISTS notification_user (id INTEGER PRIMARY KEY AUTOINCREMENT, notification_id INTEGER NOT NULL, user_id INTEGER NOT NULL, is_read INTEGER DEFAULT 0, read_at TEXT)",
                    null);
            db.execute("CREATE INDEX IF NOT EXISTS idx_notification_user_user ON notification_user(user_id)", null);
        } catch (Exception ex) {
            // ignore
        }
    }

    /**
     * Send a notification to a specific user. Returns true when saved.
     */
    public boolean sendToUser(Long userId, String title, String message, String dataJson, Long actorId) {
        if (userId == null)
            return false;

        Map<String, Object> p = new HashMap<>();
        p.put("t", title == null ? "" : title);
        p.put("m", message == null ? "" : message);
        p.put("d", dataJson == null ? "" : dataJson);
        p.put("c", Instant.now().toString());
        p.put("a", actorId);

        try (org.sql2o.Connection con = db.getSql2o().beginTransaction()) {

            // 1) Insert notification và lấy ID
            org.sql2o.Query q = con.createQuery(
                    "INSERT INTO notifications(title, message, data, created_at, actor_id) " +
                            "VALUES(:t, :m, :d, :c, :a)",
                    true);

            p.forEach(q::addParameter);

            Long nid = q.executeUpdate().getKey(Long.class);
            if (nid == null) {
                con.commit();
                return false;
            }

            // 2) Ghi vào notification_user
            con.createQuery("INSERT INTO notification_user(notification_id, user_id, is_read) VALUES(:nid, :uid, 0)")
                    .addParameter("nid", nid)
                    .addParameter("uid", userId)
                    .executeUpdate();

            // 3) Lấy lại bản ghi để emit SSE
            org.sql2o.data.Table tbl = con.createQuery(
                    "SELECT id, title, message, data, created_at FROM notifications WHERE id = :id")
                    .addParameter("id", nid)
                    .executeAndFetchTable();

            Map<String, Object> created = (tbl != null && !tbl.asList().isEmpty())
                    ? tbl.asList().get(0)
                    : null;

            con.commit();

            // 4) Emit SSE
            if (streamService != null && created != null) {
                streamService.emitToUser(userId, created);
            }

            return true;

        } catch (Exception ex) {
            logger.error("Failed to send notification to userId={}", userId, ex);
            return false;
        }
    }

    /**
     * Send notification to all users who have the given role.
     */
    public int sendToRole(String role, String title, String message, String dataJson, Long actorId) {
        if (role == null || role.trim().isEmpty())
            return 0;
        // find user ids for role
        List<Map<String, Object>> users = db.query(
                "SELECT u.id FROM users u JOIN users_roles ur ON ur.user_id = u.id WHERE ur.role = :r",
                Map.of("r", role));
        if (users == null || users.isEmpty())
            return 0;
        int total = 0;
        for (Map<String, Object> u : users) {
            Object id = u.get("id");
            if (id == null)
                continue;
            try {
                boolean ok = sendToUser(Long.parseLong(String.valueOf(id)), title, message, dataJson, actorId);
                if (ok)
                    total++;
            } catch (Exception ignored) {
            }
        }
        return total;
    }

    /**
     * List notifications for a user (their own + global if desired). Paginated.
     */
    public List<Map<String, Object>> getNotificationsForUser(Long userId, int limit, int offset) {
        if (userId == null)
            return Collections.emptyList();
        // join notifications -> notification_user
        String sql = "SELECT n.id as id, n.title as title, n.message as message, n.data as data, nu.is_read as is_read, nu.read_at as read_at, n.created_at as created_at, n.actor_id as actor_id, nu.id as delivery_id FROM notifications n JOIN notification_user nu ON nu.notification_id = n.id WHERE nu.user_id = :uid ORDER BY n.created_at DESC LIMIT :lim OFFSET :off";
        Map<String, Object> p = new HashMap<>();
        p.put("uid", userId);
        p.put("lim", limit <= 0 ? 50 : limit);
        p.put("off", Math.max(0, offset));
        List<Map<String, Object>> rows = db.query(sql, p);
        return rows == null ? Collections.emptyList() : rows;
    }

    public boolean markAsRead(Long deliveryId, Long userId) {
        if (deliveryId == null || userId == null)
            return false;
        Map<String, Object> p = Map.of("id", deliveryId, "uid", userId, "t", Instant.now().toString());
        int res = db.execute("UPDATE notification_user SET is_read = 1, read_at = :t WHERE id = :id AND user_id = :uid",
                p);
        return res > 0;
    }

    public int markAllRead(Long userId) {
        if (userId == null)
            return 0;
        Map<String, Object> p = Map.of("uid", userId, "t", Instant.now().toString());
        int res = db.execute(
                "UPDATE notification_user SET is_read = 1, read_at = :t WHERE user_id = :uid AND is_read = 0", p);
        return res;
    }

    /**
     * Count unread notifications for a user.
     */
    public int getUnreadCount(Long userId) {
        if (userId == null)
            return 0;
        Map<String, Object> p = Map.of("uid", userId);
        List<Map<String, Object>> rows = db
                .query("SELECT COUNT(*) as cnt FROM notification_user WHERE user_id = :uid AND is_read = 0", p);
        if (rows == null || rows.isEmpty())
            return 0;
        Object c = rows.get(0).get("cnt");
        try {
            return Integer.parseInt(String.valueOf(c));
        } catch (Exception ex) {
            return 0;
        }
    }

}
