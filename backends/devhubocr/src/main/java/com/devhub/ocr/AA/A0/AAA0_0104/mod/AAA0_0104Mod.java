package com.devhub.ocr.AA.A0.AAA0_0104.mod;

import com.devhub.ocr.app.plugins.database.DatabasePlugin;
import com.devhub.ocr.app.systems.notification.NotificationService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AAA0_0104Mod {

    private final NotificationService notificationService;
    private final DatabasePlugin db;

    public AAA0_0104Mod(NotificationService notificationService, DatabasePlugin db) {
        this.notificationService = notificationService;
        this.db = db;
    }

    public List<Map<String, Object>> listForUser(long userId, int limit, int offset) {
        return notificationService.getNotificationsForUser(userId, limit, offset);
    }

    public boolean markAsRead(long deliveryId, long userId) {
        return notificationService.markAsRead(deliveryId, userId);
    }

    public int markAllRead(long userId) {
        return notificationService.markAllRead(userId);
    }

    public boolean sendToUser(long userId, String title, String message) {
        return notificationService.sendToUser(userId, title, message, null, null);
    }

}
