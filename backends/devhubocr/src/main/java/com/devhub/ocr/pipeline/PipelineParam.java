package com.devhub.ocr.pipeline;

public class PipelineParam {
    private String name;
    private String type;
    private boolean required;

    public PipelineParam() {}

    public PipelineParam(String name, String type, boolean required) {
        this.name = name;
        this.type = type;
        this.required = required;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
}
