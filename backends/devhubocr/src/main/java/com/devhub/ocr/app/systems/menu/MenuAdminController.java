package com.devhub.ocr.app.systems.menu;

import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * Admin endpoints to inspect menus registered in DB and AutoMenu-declared menus in code.
 */
@RestController
@RequestMapping("/AA/A0/AAA0_0101")
public class MenuAdminController {

    private final MenuService menuService;
    private final ApplicationContext ctx;

    public MenuAdminController(MenuService menuService, ApplicationContext ctx) {
        this.menuService = menuService;
        this.ctx = ctx;
    }

    @GetMapping("/menus.json")
    public ResponseEntity<Map<String,Object>> listMenus() {
        Map<String,Object> out = new HashMap<>();
        List<Map<String,Object>> menus = menuService.getAllMenus();
        out.put("menus", menus);

        List<Map<String,Object>> auto = new ArrayList<>();
        String[] names = ctx.getBeanDefinitionNames();
        for (String n : names) {
            try {
                Object b = ctx.getBean(n);
                if (b == null) continue;
                AutoMenu a = b.getClass().getAnnotation(AutoMenu.class);
                if (a != null) {
                    Map<String,Object> m = new HashMap<>();
                    m.put("bean", b.getClass().getName());
                    m.put("title", a.title());
                    m.put("icon", a.icon());
                    m.put("path", a.path());
                    m.put("parentId", a.parentId());
                    m.put("roles", Arrays.asList(a.roles()));
                    auto.add(m);
                }
            } catch (Throwable ex) {
                // ignore
            }
        }
        out.put("autoMenus", auto);
        return ResponseEntity.ok(out);
    }

    @GetMapping("/menus/dedupe")
    public ResponseEntity<Map<String,Object>> dedupeMenus() {
        List<Map<String,Object>> menus = menuService.getAllMenus();
        Map<String, List<Map<String,Object>>> byPath = new LinkedHashMap<>();
        for (Map<String,Object> m : menus) {
            String p = m.get("path") == null ? "" : String.valueOf(m.get("path"));
            byPath.computeIfAbsent(p, k -> new ArrayList<>()).add(m);
        }
        List<Integer> removed = new ArrayList<>();
        for (Map.Entry<String, List<Map<String,Object>>> e : byPath.entrySet()) {
            List<Map<String,Object>> list = e.getValue();
            if (list.size() <= 1) continue;
            // keep the first (lowest id) and remove others
            list.sort(Comparator.comparingInt(o -> Integer.parseInt(String.valueOf(o.get("id")))));
            for (int i = 1; i < list.size(); i++) {
                int id = Integer.parseInt(String.valueOf(list.get(i).get("id")));
                menuService.deleteMenu(id);
                removed.add(id);
            }
        }
        Map<String,Object> out = new HashMap<>();
        out.put("removed", removed);
        out.put("remaining", menuService.getAllMenus());
        return ResponseEntity.ok(out);
    }
}
