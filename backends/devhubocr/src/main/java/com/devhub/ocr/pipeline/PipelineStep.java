package com.devhub.ocr.pipeline;

import java.util.List;
import java.util.Map;

public interface PipelineStep {
    String getName();
    List<PipelineParam> getInputParams();
    List<PipelineParam> getOutputParams();

    PipelineResult execute(Map<String, Object> input) throws Exception;
}
