package com.devhub.ocr.app.systems.menu;

import com.devhub.ocr.app.systems.auth.AuthContext;
import com.devhub.ocr.app.systems.auth.UserObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ControllerAdvice(annotations = Controller.class)
public class MenuModelAdvice {

    private final MenuService menuService;

    public MenuModelAdvice(MenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * Provide `sidebarMenus` model attribute for all controllers/templates.
     * This returns the flat list of menu rows filtered by the current user's roles.
     */
    @ModelAttribute("sidebarMenus")
    public List<Map<String, Object>> sidebarMenus() {
        List<Map<String, Object>> all = menuService.getAllMenus();
        if (all == null) return new ArrayList<>();

        UserObject u = AuthContext.get();
        Set<String> userRoles = new HashSet<>();
        if (u != null && u.getRoles() != null) userRoles.addAll(u.getRoles());

        // filter by roles: if menu.roles is empty => public; otherwise require any overlap
        List<Map<String, Object>> filtered = all.stream().filter(m -> {
            Object rolesObj = m.get("roles");
            if (rolesObj == null) return true;
            String rolesCsv = String.valueOf(rolesObj).trim();
            if (rolesCsv.isEmpty()) return true;
            String[] parts = rolesCsv.split(",");
            for (String p : parts) {
                String rp = p.trim();
                if (rp.isEmpty()) continue;
                if (userRoles.contains(rp)) return true;
            }
            return false;
        }).collect(Collectors.toList());

        // build a tree from filtered flat list
        Map<Integer, Map<String, Object>> byId = new LinkedHashMap<>();
        for (Map<String, Object> r : filtered) {
            Integer id = r.get("id") == null ? null : Integer.valueOf(String.valueOf(r.get("id")));
            Map<String, Object> node = new HashMap<>();
            node.put("id", id);
            node.put("title", r.get("title"));
            node.put("icon", r.get("icon"));
            node.put("path", r.get("path"));
            node.put("parent_id", r.get("parent_id"));
            node.put("children", new ArrayList<Map<String, Object>>());
            byId.put(id, node);
        }

        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> r : filtered) {
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