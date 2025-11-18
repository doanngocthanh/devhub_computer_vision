package com.devhub.ocr.admin.trx;

import com.devhub.ocr.app.systems.menu.MenuService;
import com.devhub.ocr.auth.mod.RoleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final MenuService menuService;
    private final RoleService roleService;

    public AdminController(MenuService menuService, RoleService roleService) {
        this.menuService = menuService;
        this.roleService = roleService;
    }

    @GetMapping("/menu")
    public String menu(Model model) {
        model.addAttribute("menus", menuService.getMenuTree());
        model.addAttribute("allRoles", roleService.getAllRoles());
        model.addAttribute("allMenus", menuService.getAllMenus());
        return "html/admin/menu";
    }

    @PostMapping("/menu/create")
    public String createMenu(@RequestParam String title,
                             @RequestParam(required = false) String icon,
                             @RequestParam(required = false) String path,
                             @RequestParam(required = false) Integer parentId,
                             @RequestParam(required = false) String roles) {
        menuService.createMenu(title, icon, path, parentId, roles);
        return "redirect:/admin/menu?created=1";
    }

    @GetMapping("/user-managers")
    public String userManagers(Model model) {
        List<Map<String, Object>> users = roleService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("allRoles", roleService.getAllRoles());
        return "html/admin/user-managers";
    }

    @PostMapping("/user-managers/set")
    public String setUserManager(@RequestParam String email,
                                 @RequestParam(required = false) String action) {
        // action = 'add' or 'remove'
        if ("add".equals(action)) {
            roleService.assignRoleToUserByEmail(email, "USER_MANAGER");
        } else if ("remove".equals(action)) {
            roleService.removeRoleFromUserByEmail(email, "USER_MANAGER");
        }
        return "redirect:/admin/user-managers";
    }

}
