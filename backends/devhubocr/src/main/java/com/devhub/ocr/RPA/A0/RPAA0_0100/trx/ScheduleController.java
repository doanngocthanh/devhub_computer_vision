package com.devhub.ocr.RPA.A0.RPAA0_0100.trx;

import com.devhub.ocr.RPA.A0.RPAA0_0100.plugins.JobParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/RPA/A0/RPAA0_0100")
public class ScheduleController {

    private final ScheduleManager manager;

    @Autowired
    public ScheduleController(ScheduleManager manager) {
        this.manager = manager;
    }

    // Serve the UI page (Thymeleaf template)
    @GetMapping("/")
    public String ui() {
        return "html/RPA/A0/RPAA0_0100/index";
    }

    @PostMapping("/schedule")
    @ResponseBody
    public Map<String, String> schedule(@RequestBody JobRequest req) throws Exception {
        JobParams p = new JobParams(req.getJsonParams(), req.getFilePath());
        String jobId = manager.scheduleJob(req.getClassName(), req.getSubmitTime(), p);
        return Map.of("jobId", jobId);
    }

    @GetMapping("/jobs/{className}")
    @ResponseBody
    public List<String> listJobs(@PathVariable String className) {
        return manager.listJobsByClass(className);
    }

    @GetMapping("/job/{jobId}")
    @ResponseBody
    public Map<String, Object> jobInfo(@PathVariable String jobId) {
        return manager.getJobInfo(jobId);
    }

    @PostMapping("/job/{jobId}/pause")
    @ResponseBody
    public void pause(@PathVariable String jobId) {
        manager.pauseJob(jobId);
    }

    @PostMapping("/job/{jobId}/resume")
    @ResponseBody
    public void resume(@PathVariable String jobId, @RequestParam(required = false) String submitTime) throws Exception {
        manager.resumeJob(jobId, submitTime);
    }

    @PostMapping("/job/{jobId}/stop")
    @ResponseBody
    public void stop(@PathVariable String jobId) {
        manager.stopJob(jobId);
    }

    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter events() {
        return manager.createEmitter();
    }
}
