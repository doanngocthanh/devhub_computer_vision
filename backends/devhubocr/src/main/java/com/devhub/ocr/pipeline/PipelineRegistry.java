package com.devhub.ocr.pipeline;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PipelineRegistry {

    @Autowired(required = false)
    private List<PipelineStep> steps;

    private Map<String, PipelineStep> map = new HashMap<>();

    @PostConstruct
    public void init() {
        if (steps == null) return;
        for (PipelineStep s : steps) {
            map.put(s.getClass().getSimpleName(), s);
        }
    }

    public PipelineStep getStep(String name) {
        return map.get(name);
    }

    public Collection<PipelineStep> list() {
        return map.values();
    }
}
