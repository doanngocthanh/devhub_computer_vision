package com.devhub.ocr.WL.A0.WLA0_0100.dto;

import java.util.List;

public class PipelineDTO {
    private Long id;
    private String name;
    private List<PipelineStepDTO> steps;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<PipelineStepDTO> getSteps() { return steps; }
    public void setSteps(List<PipelineStepDTO> steps) { this.steps = steps; }
}
