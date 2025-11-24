package com.devhub.ocr.RPA.A0.RPAA0_0100.plugins;

import com.google.gson.JsonObject;

/**
 * Simple holder for job parameters that can be attached to a scheduled task.
 * Contains optional JSON params, a file path, and a remaining-delay field
 * used when pausing/resuming tasks.
 */
public class JobParams {
    private String jsonParams;
    private String filePath;
    private long remainingDelayMillis = -1;

    public JobParams() {
    }

    public JobParams(String jsonParams, String filePath) {
        this.jsonParams = jsonParams;
        this.filePath = filePath;
    }

    public String getJsonParams() {
        return jsonParams;
    }

    public JsonObject getJsonParamsAsObject() {
        if (jsonParams == null) {
            return null;
        }
        return com.google.gson.JsonParser.parseString(jsonParams).getAsJsonObject();
    }

    public void setJsonParams(String jsonParams) {
        this.jsonParams = jsonParams;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getRemainingDelayMillis() {
        return remainingDelayMillis;
    }

    public void setRemainingDelayMillis(long remainingDelayMillis) {
        this.remainingDelayMillis = remainingDelayMillis;
    }
}
