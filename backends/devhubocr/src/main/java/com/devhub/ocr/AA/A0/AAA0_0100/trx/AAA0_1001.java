package com.devhub.ocr.AA.A0.AAA0_0100.trx;

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
@RequestMapping("/AA/A0/AAA0_0101")
public class AAA0_1001 {

    private final MenuService menuService;
    private final RoleService roleService;

    public AAA0_1001(MenuService menuService, RoleService roleService) {
        this.menuService = menuService;
        this.roleService = roleService;
    }

    @GetMapping("/")
    public String menu(Model model, @RequestParam(required = false) Integer editId) {
        model.addAttribute("menus", menuService.getMenuTree());
        model.addAttribute("allRoles", roleService.getAllRoles());
        model.addAttribute("allMenus", menuService.getAllMenus());
        if (editId != null) {
            Map<String, Object> edit = menuService.getMenuById(editId);
            model.addAttribute("editMenu", edit);
            // prepare roles array for pre-selection in the template
            if (edit != null && edit.get("roles") != null) {
                String rolesCsv = String.valueOf(edit.get("roles"));
                String[] editRoles = rolesCsv.isBlank() ? new String[]{} : rolesCsv.split("\\s*,\\s*");
                model.addAttribute("editRoles", editRoles);
            } else {
                model.addAttribute("editRoles", new String[]{});
            }
        }
        // Return the specific template file under templates/html/AA/A0/AAA0_0101/AAA0_0101.html
        return "html/AA/A0/AAA0_0101/AAA0_0101";
    }

    @PostMapping("/menu/create")
    public String createMenu(@RequestParam String title,
                             @RequestParam(required = false) String icon,
                             @RequestParam(required = false) String path,
                             @RequestParam(required = false) Integer parentId,
                             @RequestParam(required = false) String[] roles) {
        String rolesCsv = "";
        if (roles != null && roles.length > 0) {
            rolesCsv = String.join(",", roles);
        }
        menuService.createMenu(title, icon, path, parentId, rolesCsv);
        return "redirect:/AA/A0/AAA0_0101/?created=1";
    }

    @PostMapping("/menu/update")
    public String updateMenu(@RequestParam int id,
                             @RequestParam String title,
                             @RequestParam(required = false) String icon,
                             @RequestParam(required = false) String path,
                             @RequestParam(required = false) Integer parentId,
                             @RequestParam(required = false) String[] roles) {
        String rolesCsv = "";
        if (roles != null && roles.length > 0) {
            rolesCsv = String.join(",", roles);
        }
        menuService.updateMenu(id, title, icon, path, parentId, rolesCsv);
        return "redirect:/AA/A0/AAA0_0101/?updated=1";
    }

    @PostMapping("/menu/delete")
    public String deleteMenu(@RequestParam int id) {
        menuService.deleteMenu(id);
        return "redirect:/AA/A0/AAA0_0101/?deleted=1";
    }

    @GetMapping("/user-managers")
    public String userManagers(Model model) {
        List<Map<String, Object>> users = roleService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("allRoles", roleService.getAllRoles());
        return "html/AA/A0/AAA0_0101/user-managers";
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
        return "redirect:/AA/A0/AAA0_0101/user-managers";
    }

}
