package com.devhub.ocr.app.systems.notification;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationStreamService {
    // Map userId -> emitter
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(NotificationStreamService.class);

    // Default timeout 30 minutes
    private static final long DEFAULT_TIMEOUT = 30 * 60 * 1000L;

    public SseEmitter register(long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        // remove existing emitter for user (replace)
        SseEmitter existing = emitters.put(userId, emitter);
        if (existing != null) {
            try { existing.complete(); } catch (Exception ignored) {}
        }

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((ex) -> emitters.remove(userId));

        // send a welcome/heartbeat event so the client knows it's connected
        try {
            emitter.send(SseEmitter.event().name("connected").data("connected:" + Instant.now().toString()));
        } catch (IOException ignored) {
        }
        try { logger.info("SSE emitter registered for userId={}", userId); } catch (Exception ignored) {}

        return emitter;
    }

    public void emitToUser(long userId, Object payload) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event().name("notification").data(payload));
            try { logger.info("Emitted notification to userId={}", userId); } catch (Exception ignored) {}
        } catch (IOException e) {
            // connection dead — remove
            emitters.remove(userId);
            try { emitter.completeWithError(e); } catch (Exception ignored) {}
        }
    }

    public void closeAll() {
        for (Map.Entry<Long, SseEmitter> e : emitters.entrySet()) {
            try { e.getValue().complete(); } catch (Exception ignored) {}
        }
        emitters.clear();
    }
}
