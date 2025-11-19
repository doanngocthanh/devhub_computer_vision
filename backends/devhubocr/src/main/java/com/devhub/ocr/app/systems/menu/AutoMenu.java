package com.devhub.ocr.app.systems.menu;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to mark a controller (or bean) that should register a menu entry
 * at application startup. Example usage:
 * @AutoMenu(title="My Page", icon="home", path="/AA/A0/AAA0_0102", roles={"IT"})
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoMenu {
    String title();
    String icon() default "";
    String path();
    int parentId() default -1; // -1 means top-level
    String parentPath() default ""; // optional parent path to attach this menu under
    String[] roles() default {};
}
