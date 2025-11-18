package com.devhub.ocr.app.systems.menu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

@ControllerAdvice
public class MenuModelAdvice {

    @Autowired
    private MenuService menuService;

    @ModelAttribute
    public void addMenusToModel(Model model) {
        List<Map<String, Object>> menus = menuService.getMenuTree();
        model.addAttribute("menus", menus);
    }
}
