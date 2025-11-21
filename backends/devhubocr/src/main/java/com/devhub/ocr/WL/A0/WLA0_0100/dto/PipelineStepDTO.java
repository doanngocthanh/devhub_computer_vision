package com.devhub.ocr.WL.A0.WLA0_0100.dto;

import java.util.Map;

public class PipelineStepDTO {
    private String id;
    private String bean;
    private Map<String, Object> input;
    private String outputKey;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBean() { return bean; }
    public void setBean(String bean) { this.bean = bean; }
    public Map<String, Object> getInput() { return input; }
    public void setInput(Map<String, Object> input) { this.input = input; }
    public String getOutputKey() { return outputKey; }
    public void setOutputKey(String outputKey) { this.outputKey = outputKey; }
}
