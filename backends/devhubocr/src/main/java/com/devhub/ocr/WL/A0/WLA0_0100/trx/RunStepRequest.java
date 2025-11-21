package com.devhub.ocr.WL.A0.WLA0_0100.trx;

import java.util.Map;

/**
 * Typed request for running a single pipeline step from the UI.
 */
public class RunStepRequest {
    private String bean;
    private Map<String, Object> input;

    public RunStepRequest() {}

    public String getBean() {
        return bean;
    }

    public void setBean(String bean) {
        this.bean = bean;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input;
    }
}
