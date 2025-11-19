package com.devhub.ocr.api;

import org.springframework.web.bind.annotation.*;
import com.devhub.ocr.app.systems.menu.MenuService;

import java.util.*;

@RestController
@RequestMapping("/api")
public class MenuApiController {

    private final MenuService menuService;

    public MenuApiController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/menu/sidebar")
    public List<Map<String, Object>> getSidebarMenus() {
        return menuService.getMenuTree();
    }
    
    /**
     * API DEBUG - Xem tất cả menu flat (không có tree)
     */
    @GetMapping("/menu/debug")
    public Map<String, Object> debugMenus() {
        List<Map<String, Object>> allMenus = menuService.getAllMenus();
        List<Map<String, Object>> menuTree = menuService.getMenuTree();
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", allMenus.size());
        result.put("flat_menus", allMenus);
        result.put("tree_menus", menuTree);
        
        return result;
    }
    
    /**
     * API để fix dữ liệu menu nhanh
     */
    

    @GetMapping("/icons/fontawesome")
    public List<String> getFontAwesomeIcons() {
        return Arrays.asList(
                "fa-home", "fa-user", "fa-cog", "fa-chart-line", "fa-database",
                "fa-file", "fa-folder", "fa-image", "fa-layer-group", "fa-list",
                "fa-table", "fa-tags", "fa-tasks", "fa-bell", "fa-calendar",
                "fa-clock", "fa-envelope", "fa-phone", "fa-map-marker-alt", "fa-building",
                "fa-box", "fa-briefcase", "fa-clipboard", "fa-edit", "fa-eraser",
                "fa-key", "fa-lock", "fa-unlock", "fa-shield-alt", "fa-users",
                "fa-user-circle", "fa-user-cog", "fa-user-plus", "fa-download", "fa-upload",
                "fa-cloud", "fa-print", "fa-qrcode", "fa-search", "fa-filter",
                "fa-bars", "fa-th", "fa-th-large", "fa-th-list", "fa-sliders-h",
                "fa-store", "fa-shopping-cart", "fa-credit-card", "fa-money-bill",
                "fa-chart-bar", "fa-chart-pie", "fa-analytics", "fa-desktop",
                "fa-mobile-alt", "fa-tablet-alt", "fa-server", "fa-network-wired");
    }

    @GetMapping("/icons/tabler")
    public List<String> getTablerIcons() {
        return Arrays.asList(
                "home", "user", "settings", "chart-line", "database",
                "file", "folder", "photo", "layers", "list",
                "table", "tags", "checkbox", "bell", "calendar",
                "clock", "mail", "phone", "map-pin", "building",
                "package", "briefcase", "clipboard", "edit", "eraser",
                "key", "lock", "lock-open", "shield", "users",
                "user-circle", "user-cog", "user-plus", "download", "upload",
                "cloud", "printer", "qrcode", "search", "filter",
                "menu-2", "grid", "layout-grid", "layout-list", "adjustments",
                "shopping-cart", "credit-card", "currency-dollar", "report-analytics",
                "device-desktop", "device-mobile", "device-tablet", "server", "network");
    }
}