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
        String sql = """
        CREATE TABLE IF NOT EXISTS menus (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          title TEXT NOT NULL,
          icon TEXT,
          path TEXT,
          parent_id INTEGER DEFAULT NULL,
          roles TEXT,
          order_num INTEGER DEFAULT 0,
          is_active INTEGER DEFAULT 1,
          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
        )
        """;
        db.execute(sql, null);
    }

    public List<Map<String, Object>> getAllMenus() {
        List<Map<String, Object>> rows = db.query(
            "SELECT id, title, icon, path, parent_id, roles, order_num, is_active FROM menus WHERE is_active = 1 ORDER BY order_num, id", 
            null
        );
        return rows == null ? Collections.emptyList() : rows;
    }

    public boolean createMenu(String title, String icon, String path, Integer parentId, String rolesCsv, Integer orderNum, Boolean isActive) {
        if (path != null && !path.isEmpty()) {
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
        params.put("order_num", orderNum == null ? 0 : orderNum);
        params.put("is_active", isActive == null ? 1 : (isActive ? 1 : 0));
        int res = db.execute(
            "INSERT INTO menus(title, icon, path, parent_id, roles, order_num, is_active) VALUES(:title, :icon, :path, :parent, :roles, :order_num, :is_active)", 
            params
        );
        return res > 0;
    }

    public boolean deleteMenu(int id) {
        int res = db.execute("DELETE FROM menus WHERE id = :id", Map.of("id", id));
        return res > 0;
    }

    public Map<String, Object> getMenuById(int id) {
        List<Map<String, Object>> rows = db.query(
            "SELECT id, title, icon, path, parent_id, roles FROM menus WHERE id = :id", 
            Map.of("id", id)
        );
        if (rows == null || rows.isEmpty()) return null;
        return rows.get(0);
    }

    public Map<String, Object> getMenuByPath(String path) {
        if (path == null) return null;
        List<Map<String, Object>> rows = db.query(
            "SELECT id, title, icon, path, parent_id, roles FROM menus WHERE path = :p LIMIT 1", 
            Map.of("p", path)
        );
        if (rows == null || rows.isEmpty()) return null;
        return rows.get(0);
    }

    public boolean updateMenu(int id, String title, String icon, String path, Integer parentId, String rolesCsv, Integer orderNum, Boolean isActive) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("title", title);
        params.put("icon", icon == null ? "" : icon);
        params.put("path", path == null ? "" : path);
        params.put("parent", parentId);
        params.put("roles", rolesCsv == null ? "" : rolesCsv);
        params.put("order_num", orderNum == null ? 0 : orderNum);
        params.put("is_active", isActive == null ? 1 : (isActive ? 1 : 0));
        int res = db.execute(
            "UPDATE menus SET title = :title, icon = :icon, path = :path, parent_id = :parent, roles = :roles, order_num = :order_num, is_active = :is_active WHERE id = :id", 
            params
        );
        return res > 0;
    }

    /**
     * Build a tree of menus: top-level items with a `children` list.
     * FIX: Đảm bảo parent_id được xử lý đúng kiểu dữ liệu
     */
    public List<Map<String, Object>> getMenuTree() {
        List<Map<String, Object>> all = getAllMenus();
        Map<Integer, Map<String, Object>> byId = new LinkedHashMap<>();
        
        // Bước 1: Tạo map của tất cả menu nodes
        for (Map<String, Object> r : all) {
            Integer id = parseInteger(r.get("id"));
            if (id == null) continue;
            
            Map<String, Object> node = new HashMap<>();
            node.put("id", id);
            node.put("title", r.get("title"));
            node.put("icon", r.get("icon"));
            node.put("path", r.get("path"));
            node.put("roles", r.get("roles"));
            node.put("children", new ArrayList<Map<String, Object>>());
            byId.put(id, node);
        }
        
        // Bước 2: Xây dựng cây menu
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> r : all) {
            Integer id = parseInteger(r.get("id"));
            Integer pid = parseInteger(r.get("parent_id"));
            
            Map<String, Object> node = byId.get(id);
            if (node == null) continue;
            
            if (pid == null || pid == 0) {
                // Menu gốc (không có parent)
                roots.add(node);
            } else {
                // Menu con - thêm vào parent's children
                Map<String, Object> parent = byId.get(pid);
                if (parent != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                    children.add(node);
                } else {
                    // Parent không tồn tại, coi như menu gốc
                    roots.add(node);
                }
            }
        }
        
        return roots;
    }
    
    /**
     * Helper method để parse Integer an toàn từ Object
     */
    private Integer parseInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            String str = (String) value;
            if (str.isEmpty() || str.equalsIgnoreCase("null")) return null;
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}