package com.devhub.ocr.app.systems.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the current user (if any) to all controllers and Thymeleaf templates as 'currentUser'.
 */
@ControllerAdvice(annotations = Controller.class)
public class CurrentUserModelAdvice {

    @ModelAttribute("currentUser")
    public UserObject currentUser() {
        return AuthContext.get();
    }
}
