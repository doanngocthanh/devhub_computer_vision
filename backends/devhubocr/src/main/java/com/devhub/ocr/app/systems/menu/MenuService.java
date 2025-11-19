package com.devhub.ocr.app.systems.menu;

import com.devhub.ocr.app.plugins.database.DatabasePlugin;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MenuService {

    private final DatabasePlugin db;

    public MenuService(DatabasePlugin db) {
        this.db = db;
        initMenuTable();
    }

    private void initMenuTable() {
        String sql = "CREATE TABLE IF NOT EXISTS menus (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "icon TEXT, " +
                "path TEXT, " +
                "parent_id INTEGER, " +
                "roles TEXT" +
                ")";
        db.execute(sql, null);
    }

    public List<Map<String, Object>> getAllMenus() {
        List<Map<String, Object>> rows = db.query("SELECT id, title, icon, path, parent_id, roles FROM menus ORDER BY id", null);
        return rows == null ? Collections.emptyList() : rows;
    }

    public boolean createMenu(String title, String icon, String path, Integer parentId, String rolesCsv) {
        // avoid duplicate menu rows for the same path
        if (path != null) {
            List<Map<String, Object>> exists = db.query("SELECT id FROM menus WHERE path = :p", Map.of("p", path));
            if (exists != null && !exists.isEmpty()) {
                return false;
            }
        }
        Map<String, Object> params = new HashMap<>();
        params.put("title", title);
        params.put("icon", icon == null ? "" : icon);
        params.put("path", path == null ? "" : path);
        params.put("parent", parentId);
        params.put("roles", rolesCsv == null ? "" : rolesCsv);
        int res = db.execute("INSERT INTO menus(title, icon, path, parent_id, roles) VALUES(:title, :icon, :path, :parent, :roles)", params);
        return res > 0;
    }

    public boolean deleteMenu(int id) {
        int res = db.execute("DELETE FROM menus WHERE id = :id", Map.of("id", id));
        return res > 0;
    }

    public Map<String, Object> getMenuById(int id) {
        List<Map<String, Object>> rows = db.query("SELECT id, title, icon, path, parent_id, roles FROM menus WHERE id = :id", Map.of("id", id));
        if (rows == null || rows.isEmpty()) return null;
        return rows.get(0);
    }

    public Map<String, Object> getMenuByPath(String path) {
        if (path == null) return null;
        List<Map<String, Object>> rows = db.query("SELECT id, title, icon, path, parent_id, roles FROM menus WHERE path = :p LIMIT 1", Map.of("p", path));
        if (rows == null || rows.isEmpty()) return null;
        return rows.get(0);
    }

    public boolean updateMenu(int id, String title, String icon, String path, Integer parentId, String rolesCsv) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("title", title);
        params.put("icon", icon == null ? "" : icon);
        params.put("path", path == null ? "" : path);
        params.put("parent", parentId);
        params.put("roles", rolesCsv == null ? "" : rolesCsv);
        int res = db.execute("UPDATE menus SET title = :title, icon = :icon, path = :path, parent_id = :parent, roles = :roles WHERE id = :id", params);
        return res > 0;
    }

    /**
     * Build a tree of menus: top-level items with a `children` list.
     */
    public List<Map<String, Object>> getMenuTree() {
        List<Map<String, Object>> all = getAllMenus();
        Map<Integer, Map<String, Object>> byId = new LinkedHashMap<>();
        for (Map<String, Object> r : all) {
            Integer id = r.get("id") == null ? null : Integer.valueOf(String.valueOf(r.get("id")));
            Map<String, Object> node = new HashMap<>();
            node.put("id", id);
            node.put("title", r.get("title"));
            node.put("icon", r.get("icon"));
            node.put("path", r.get("path"));
            node.put("roles", r.get("roles"));
            node.put("children", new ArrayList<Map<String, Object>>());
            byId.put(id, node);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> r : all) {
            Integer id = r.get("id") == null ? null : Integer.valueOf(String.valueOf(r.get("id")));
            Object p = r.get("parent_id");
            Integer pid = p == null ? null : (p instanceof Number ? ((Number) p).intValue() : Integer.valueOf(String.valueOf(p)));
            Map<String, Object> node = byId.get(id);
            if (pid == null) {
                roots.add(node);
            } else {
                Map<String, Object> parent = byId.get(pid);
                if (parent != null) {
                    List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                    children.add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        return roots;
    }
}
