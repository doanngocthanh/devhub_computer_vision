package com.devhub.ocr.WL.A0.WLA0_0100.dto;

public class PipelineConnectionDTO {
    private String fromStep;
    private String fromOutput;
    private String toStep;
    private String toInput;

    public String getFromStep() { return fromStep; }
    public void setFromStep(String fromStep) { this.fromStep = fromStep; }
    public String getFromOutput() { return fromOutput; }
    public void setFromOutput(String fromOutput) { this.fromOutput = fromOutput; }
    public String getToStep() { return toStep; }
    public void setToStep(String toStep) { this.toStep = toStep; }
    public String getToInput() { return toInput; }
    public void setToInput(String toInput) { this.toInput = toInput; }
}
