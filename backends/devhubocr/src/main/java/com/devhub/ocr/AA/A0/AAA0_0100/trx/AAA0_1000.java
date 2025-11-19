package com.devhub.ocr.AA.A0.AAA0_0100.trx;

import com.devhub.ocr.auth.mod.RoleService;
import com.devhub.ocr.app.systems.menu.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

@Controller
@RequestMapping("/AA/A0/AAA0_0100")
public class AAA0_1000 {

    private final RoleService roleService;
    private final MenuService menuService;

    @Autowired
    public AAA0_1000(RoleService roleService, MenuService menuService) {
        this.roleService = roleService;
        this.menuService = menuService;
    }

    @GetMapping({ "", "/" })
    public String root(Model model,
            @RequestParam(name = "loadMenuId", required = false) Integer loadMenuId,
            @RequestParam(name = "loadPath", required = false) String loadPath) {
        // pathKey uses dot notation — default page key
        String pathKey = "";

        // If an explicit dotted path is provided (after redirect), prefer it
        if (loadPath != null && !loadPath.isBlank()) {
            pathKey = loadPath;
        }

        // if a menu row was requested to load, derive dotted path from menu.path
        if (loadMenuId != null) {
            Map<String, Object> m = menuService.getMenuById(loadMenuId);
            if (m != null) {
                Object p = m.get("path");
                String path = p == null ? "" : String.valueOf(p);
                String stripped = path.startsWith("/") ? path.substring(1) : path;
                String dotted = stripped.replace('/', '.');
                if (!dotted.isBlank()) {
                    pathKey = dotted;
                }
            }
        }

        Set<String> rolesForPath = roleService.getRolesForPath(pathKey);
        List<Map<String, Object>> users = roleService.getAllUsers();

        Map<String, Set<String>> userRolesMap = new HashMap<>();
        for (Map<String, Object> u : users) {
            String email = String.valueOf(u.get("email"));
            Set<String> r = roleService.getUserRolesByEmail(email);
            userRolesMap.put(email, r);
        }

        // all possible roles for selection
        List<Map<String, Object>> allRoles = roleService.getAllRoles();

        // load menus and compute dotted path keys (strip leading slash then replace '/'
        // -> '.')
        List<Map<String, Object>> allMenus = menuService.getAllMenus();
        for (Map<String, Object> m : allMenus) {
            Object p = m.get("path");
            String path = p == null ? "" : String.valueOf(p);
            String stripped = path.startsWith("/") ? path.substring(1) : path;
            String dotted = stripped.replace('/', '.');
            m.put("dotted", dotted);
        }

        model.addAttribute("pageTitle", "Quản lý Role - " + pathKey);
        model.addAttribute("pathKey", pathKey);
        model.addAttribute("rolesForPath", rolesForPath);
        model.addAttribute("users", users);
        model.addAttribute("userRolesMap", userRolesMap);
        model.addAttribute("allRoles", allRoles);
        model.addAttribute("allMenus", allMenus);

        return "html/AA/A0/AAA0_0100/AAA0_0100";
    }

    @PostMapping("/role/create")
    public String createRole(@RequestParam String role, @RequestParam(required = false) String description,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        if (role != null && !role.trim().isEmpty()) {
            boolean ok = roleService.createRole(role.trim(), description);
            if (ok)
                ra.addFlashAttribute("success", "Role '" + role.trim() + "' đã được tạo.");
            else
                ra.addFlashAttribute("error", "Không thể tạo role '" + role.trim() + "'.");
        } else {
            ra.addFlashAttribute("error", "Tên role không được để trống.");
        }
        return "redirect:/AA/A0/AAA0_0100";
    }

    @PostMapping("/role/delete")
    public String deleteRole(@RequestParam String role,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        if (role != null && !role.trim().isEmpty()) {
            boolean ok = roleService.deleteRole(role.trim());
            if (ok)
                ra.addFlashAttribute("success", "Role '" + role.trim() + "' đã bị xóa.");
            else
                ra.addFlashAttribute("error", "Không thể xóa role '" + role.trim() + "'.");
        } else {
            ra.addFlashAttribute("error", "Tên role không được để trống.");
        }
        return "redirect:/AA/A0/AAA0_0100";
    }

    @PostMapping("/path/update-roles")
    public String updateRolesForPath(@RequestParam(required = false, name = "roles") List<String> roles,
            @RequestParam(required = false, name = "pathKey") String pathKey,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        // prefer explicit form param pathKey; fall back to empty string
        if (pathKey == null) pathKey = "";
        Set<String> set = new HashSet<>();
        if (roles != null)
            roles.forEach(r -> set.add(r.trim()));

        boolean ok = roleService.setRolesForPath(pathKey, set);
        if (ok)
            ra.addFlashAttribute("success", "Cập nhật roles cho path thành công.");
        else
            ra.addFlashAttribute("error", "Không thể cập nhật roles cho path " + pathKey + ".");

        // Redirect back to the page and preserve the pathKey so the form remains filled
        String redirect = "/AA/A0/AAA0_0100";
        if (pathKey != null && !pathKey.isBlank()) {
            try {
                String enc = URLEncoder.encode(pathKey, java.nio.charset.StandardCharsets.UTF_8.name());
                redirect += "?loadPath=" + enc;
            } catch (UnsupportedEncodingException e) {
                // fallback: don't append
            }
        }
        return "redirect:" + redirect;
    }

    @PostMapping("/user/update-roles")
    public String updateRolesForUser(@RequestParam String email,
            @RequestParam(required = false, name = "roles") List<String> roles,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        Set<String> set = new HashSet<>();
        if (roles != null)
            roles.forEach(r -> set.add(r.trim()));
        boolean ok = roleService.setRolesForUserByEmail(email, set);
        if (ok)
            ra.addFlashAttribute("success", "Cập nhật roles cho người dùng thành công.");
        else
            ra.addFlashAttribute("error", "Không thể cập nhật roles cho người dùng.");
        return "redirect:/AA/A0/AAA0_0100";
    }

}