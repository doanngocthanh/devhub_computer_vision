package com.devhub.ocr.pipeline;

import java.util.HashMap;
import java.util.Map;

public class PipelineResult {
    private Map<String, Object> output = new HashMap<>();

    public PipelineResult() {}

    public PipelineResult(Map<String, Object> output) { this.output = output; }

    public Map<String, Object> getOutput() { return output; }
    public void setOutput(Map<String, Object> output) { this.output = output; }

    public void put(String key, Object val) { this.output.put(key, val); }
}
