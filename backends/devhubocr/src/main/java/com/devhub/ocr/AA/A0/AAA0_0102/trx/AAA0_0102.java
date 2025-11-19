package com.devhub.ocr.AA.A0.AAA0_0102.trx;

import com.devhub.ocr.app.systems.menu.AutoMenu;
import com.devhub.ocr.app.systems.menu.MenuService;
import com.devhub.ocr.auth.mod.RoleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("/AA/A0/AAA0_0102")
public class AAA0_0102 {

    private final RoleService roleService;
    private final MenuService menuService;

    public AAA0_0102(RoleService roleService, MenuService menuService) {
        this.roleService = roleService;
        this.menuService = menuService;
    }

    @GetMapping("/")
    public String index(Model model, @RequestParam(required = false) Integer editId) {
        model.addAttribute("pageTitle", "AAA0_0102 - Menu Management");
        model.addAttribute("menus", menuService.getMenuTree());
        model.addAttribute("allMenus", menuService.getAllMenus());
        model.addAttribute("allRoles", roleService.getAllRoles());
        if (editId != null) {
            Map<String, Object> edit = menuService.getMenuById(editId);
            model.addAttribute("editMenu", edit);
            if (edit != null && edit.get("roles") != null) {
                String rolesCsv = String.valueOf(edit.get("roles"));
                String[] editRoles = rolesCsv.isBlank() ? new String[]{} : rolesCsv.split("\\s*,\\s*");
                model.addAttribute("editRoles", editRoles);
            } else {
                model.addAttribute("editRoles", new String[]{});
            }
        }
        return "html/AA/A0/AAA0_0102/AAA0_0102";
    }

    @PostMapping("/menu/create")
    public String createMenu(@RequestParam String title,
                             @RequestParam(required = false) String icon,
                             @RequestParam(required = false) String path,
                             @RequestParam(required = false) Integer parentId,
                             @RequestParam(required = false) String[] roles) {
        String rolesCsv = "";
        if (roles != null && roles.length > 0) rolesCsv = String.join(",", roles);
        menuService.createMenu(title, icon, path, parentId, rolesCsv, null, true);
        return "redirect:/AA/A0/AAA0_0102/?created=1";
    }

    @PostMapping("/menu/update")
    public String updateMenu(@RequestParam int id,
                             @RequestParam String title,
                             @RequestParam(required = false) String icon,
                             @RequestParam(required = false) String path,
                             @RequestParam(required = false) Integer parentId,
                             @RequestParam(required = false) String[] roles) {
        String rolesCsv = "";
        if (roles != null && roles.length > 0) rolesCsv = String.join(",", roles);
        menuService.updateMenu(id, title, icon, path, parentId, rolesCsv, null, true);
        return "redirect:/AA/A0/AAA0_0102/?updated=1";
    }

    @PostMapping("/menu/delete")
    public String deleteMenu(@RequestParam int id) {
        menuService.deleteMenu(id);
        return "redirect:/AA/A0/AAA0_0102/?deleted=1";
    }

}
