package com.devhub.ocr.app.systems.notification;

import com.devhub.ocr.app.systems.auth.AuthContext;
import com.devhub.ocr.app.systems.auth.UserObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@ControllerAdvice(annotations = Controller.class)
public class NotificationModelAdvice {

    private final NotificationService notificationService;

    public NotificationModelAdvice(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @ModelAttribute("notificationCount")
    public Integer notificationCount() {
        UserObject u = AuthContext.get();
        if (u == null) return 0;
        return notificationService.getUnreadCount(u.getId());
    }

    @ModelAttribute("recentNotifications")
    public List<Map<String, Object>> recentNotifications() {
        UserObject u = AuthContext.get();
        if (u == null) return Collections.emptyList();
        return notificationService.getNotificationsForUser(u.getId(), 5, 0);
    }

}
