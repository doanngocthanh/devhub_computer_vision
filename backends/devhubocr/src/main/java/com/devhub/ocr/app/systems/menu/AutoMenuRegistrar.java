package com.devhub.ocr.app.systems.menu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Scans the application context for beans annotated with @AutoMenu and ensures
 * a corresponding row exists in the menus table.
 */
@Component
public class AutoMenuRegistrar implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(AutoMenuRegistrar.class);

    private ApplicationContext ctx;
    private final MenuService menuService;
    private final java.util.Set<String> processed = new java.util.HashSet<>();
    private boolean ran = false;

    public AutoMenuRegistrar(MenuService menuService) {
        this.menuService = menuService;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        if (ran) return; // guard against multiple context refreshed events
        String[] names = ctx.getBeanDefinitionNames();
        for (String n : names) {
            Object bean = ctx.getBean(n);
            if (bean == null) continue;
            AutoMenu a = AnnotationUtils.findAnnotation(bean.getClass(), AutoMenu.class);
            if (a != null) {
                try {
                    String path = a.path();
                    if (path == null || path.isBlank()) continue;
                    if (processed.contains(path)) continue;
                    // determine parent id: prefer explicit parentId, then parentPath, then infer by path prefix
                    Integer parent = null;
                    if (a.parentId() >= 0) {
                        parent = a.parentId();
                    } else if (a.parentPath() != null && !a.parentPath().isBlank()) {
                        String ppath = a.parentPath();
                        Map<String, Object> parentRow = menuService.getMenuByPath(ppath);
                        if (parentRow == null) {
                            // create parent stub (title fallback to parent path)
                            String rolesCsvParent = "";
                            menuService.createMenu(ppath, "", ppath, null, rolesCsvParent, null, true);
                            // mark parent as processed
                            processed.add(ppath);
                            parentRow = menuService.getMenuByPath(ppath);
                        }
                        if (parentRow != null && parentRow.get("id") != null) parent = Integer.valueOf(String.valueOf(parentRow.get("id")));
                    } else {
                        // try to infer parent by removing the last segment of path
                        String cleaned = path;
                        if (cleaned.endsWith("/")) cleaned = cleaned.substring(0, cleaned.length()-1);
                        int lastSlash = cleaned.lastIndexOf('/');
                        if (lastSlash > 0) {
                            String inferred = cleaned.substring(0, lastSlash+1); // keep trailing /
                            Map<String, Object> parentRow = menuService.getMenuByPath(inferred);
                            if (parentRow != null && parentRow.get("id") != null) parent = Integer.valueOf(String.valueOf(parentRow.get("id")));
                        }
                    }

                    Map<String, Object> existing = menuService.getMenuByPath(path);
                    String rolesCsv = String.join(",", a.roles());
                    if (existing == null) {
                        boolean ok = menuService.createMenu(a.title(), a.icon(), path, parent, rolesCsv, null, true);
                        if (ok) {
                            logger.info("Auto-registered menu for bean {} path={}", bean.getClass().getName(), path);
                            processed.add(path);
                        }
                    } else {
                        // update parent/title/icon/roles to keep in sync
                        try {
                            Integer existingId = Integer.valueOf(String.valueOf(existing.get("id")));
                            menuService.updateMenu(existingId, a.title(), a.icon(), path, parent, rolesCsv, null, true);
                            processed.add(path);
                        } catch (Exception ex) {
                            logger.warn("Failed to update existing menu {}: {}", path, ex.getMessage());
                        }
                        // mark as processed even if already exists
                        processed.add(path);
                    }
                } catch (Exception ex) {
                    logger.warn("Failed to auto-register menu for bean {}: {}", bean.getClass().getName(), ex.getMessage());
                }
            }
        }
        ran = true;
    }
}
