package com.devhub.ocr.RPA.A0.RPAA0_0100.trx;

import com.devhub.ocr.RPA.A0.RPAA0_0100.plugins.JobParams;
import com.devhub.ocr.RPA.A0.RPAA0_0100.plugins.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ScheduleManager {
    private final Scheduled scheduled;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public ScheduleManager() {
        this.scheduled = new Scheduled();
    }

    public String scheduleJob(String className, String submitTime, JobParams params) throws Exception {
        String jobId = scheduled.execute(className, submitTime, params);
        pushEvent(Map.of("type", "scheduled", "jobId", jobId, "className", className));
        return jobId;
    }

    public void pauseJob(String jobId) {
        scheduled.pauseJob(jobId);
        pushEvent(Map.of("type", "paused", "jobId", jobId));
    }

    public void resumeJob(String jobId, String submitTime) throws Exception {
        scheduled.resumeJob(jobId, submitTime);
        pushEvent(Map.of("type", "resumed", "jobId", jobId));
    }

    public void stopJob(String jobId) {
        scheduled.stopJob(jobId);
        pushEvent(Map.of("type", "stopped", "jobId", jobId));
    }

    public Map<String, Object> getJobInfo(String jobId) {
        JobParams p = scheduled.getJobParams(jobId);
        boolean exists = scheduled.getFuture(jobId) != null;
        return Map.of("jobId", jobId, "exists", exists, "params", p != null ? p : Map.of());
    }

    public List<String> listJobsByClass(String className) {
        return scheduled.listJobIdsByClass(className);
    }

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        emitters.add(emitter);
        
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            System.out.println("[ScheduleManager] Emitter completed. Active: " + emitters.size());
        });
        
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            System.out.println("[ScheduleManager] Emitter timeout. Active: " + emitters.size());
        });
        
        emitter.onError((e) -> {
            emitters.remove(emitter);
            System.out.println("[ScheduleManager] Emitter error. Active: " + emitters.size());
        });
        
        // Register with LogBroker for receiving log events
        return LogBroker.get().register(emitter);
    }

    private void pushEvent(Object event) {
        // Publish events asynchronously in a try-catch to prevent errors from propagating
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                LogBroker.get().publishEvent(event);
            } catch (Exception e) {
                // Log but don't propagate - SSE errors are handled in LogBroker
                System.err.println("[ScheduleManager] Error publishing event: " + e.getMessage());
            }
        }).exceptionally(throwable -> {
            System.err.println("[ScheduleManager] Async error publishing event: " + throwable.getMessage());
            return null;
        });
    }
}