package com.devhub.ocr.RPA.A0.RPAA0_0100.trx;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class LogBroker {
    private static final LogBroker INSTANCE = new LogBroker();
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    private LogBroker() {
    }

    public static LogBroker get() {
        return INSTANCE;
    }

    public SseEmitter register(SseEmitter emitter) {
        emitters.add(emitter);
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            System.out.println("[LogBroker] Emitter completed, removed. Active emitters: " + emitters.size());
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            System.out.println("[LogBroker] Emitter timeout, removed. Active emitters: " + emitters.size());
        });
        emitter.onError((e) -> {
            emitters.remove(emitter);
            System.out.println("[LogBroker] Emitter error, removed. Active emitters: " + emitters.size());
        });
        System.out.println("[LogBroker] New emitter registered. Active emitters: " + emitters.size());
        return emitter;
    }

    public void publishLog(String jobId, String message) {
        publishEvent(Map.of(
            "jobId", jobId,
            "message", message,
            "ts", System.currentTimeMillis()
        ));
    }

    public void publishEvent(Object event) {
        // Create a copy to avoid ConcurrentModificationException
        List<SseEmitter> emittersCopy = List.copyOf(emitters);
        
        for (SseEmitter emitter : emittersCopy) {
            try {
                emitter.send(event);
            } catch (IOException e) {
                // Connection was closed, remove this emitter
                emitters.remove(emitter);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {
                    // Already completed or closed
                }
                System.out.println("[LogBroker] Failed to send to emitter, removed. Active: " + emitters.size());
            } catch (IllegalStateException e) {
                // Emitter already completed
                emitters.remove(emitter);
                System.out.println("[LogBroker] Emitter already completed, removed. Active: " + emitters.size());
            } catch (Exception e) {
                // Other errors, log and remove
                emitters.remove(emitter);
                System.err.println("[LogBroker] Unexpected error sending event: " + e.getMessage());
            }
        }
    }
}