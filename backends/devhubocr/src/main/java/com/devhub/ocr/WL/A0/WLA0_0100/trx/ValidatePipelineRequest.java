package com.devhub.ocr.WL.A0.WLA0_0100.trx;

import java.util.Map;

import com.devhub.ocr.WL.A0.WLA0_0100.dto.PipelineDTO;

public class ValidatePipelineRequest {
    private PipelineDTO pipeline;
    private Map<String, String> initialTypes;

    public PipelineDTO getPipeline() { return pipeline; }
    public void setPipeline(PipelineDTO pipeline) { this.pipeline = pipeline; }
    public Map<String, String> getInitialTypes() { return initialTypes; }
    public void setInitialTypes(Map<String, String> initialTypes) { this.initialTypes = initialTypes; }
}
