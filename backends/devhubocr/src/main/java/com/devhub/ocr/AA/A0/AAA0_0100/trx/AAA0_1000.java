package com.devhub.ocr.AA.A0.AAA0_0100.trx;

import com.devhub.ocr.auth.mod.RoleService;
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

@Controller
@RequestMapping("/AA/A0/AAA0_0100")
public class AAA0_1000 {

    private final RoleService roleService;

    @Autowired
    public AAA0_1000(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping({ "", "/" })
    public String root(Model model) {
        // pathKey uses dot notation
        String pathKey = "AA.A0.AAA0_0100";

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

        model.addAttribute("pageTitle", "Quản lý Role - " + pathKey);
        model.addAttribute("pathKey", pathKey);
        model.addAttribute("rolesForPath", rolesForPath);
        model.addAttribute("users", users);
        model.addAttribute("userRolesMap", userRolesMap);
        model.addAttribute("allRoles", allRoles);

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
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        String pathKey = "AA.A0.AAA0_0100";
        Set<String> set = new HashSet<>();
        if (roles != null)
            roles.forEach(r -> set.add(r.trim()));
        boolean ok = roleService.setRolesForPath(pathKey, set);
        if (ok)
            ra.addFlashAttribute("success", "Cập nhật roles cho path thành công.");
        else
            ra.addFlashAttribute("error", "Không thể cập nhật roles cho path.");
        return "redirect:/AA/A0/AAA0_0100";
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