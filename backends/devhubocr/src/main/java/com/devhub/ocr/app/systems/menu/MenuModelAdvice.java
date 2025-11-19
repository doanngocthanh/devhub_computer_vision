package com.devhub.ocr.app.systems.menu;

import com.devhub.ocr.app.systems.auth.AuthContext;
import com.devhub.ocr.app.systems.auth.UserObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.HashSet;
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
        List<Map<String, Object>> out = all.stream().filter(m -> {
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

        return out;
    }
}