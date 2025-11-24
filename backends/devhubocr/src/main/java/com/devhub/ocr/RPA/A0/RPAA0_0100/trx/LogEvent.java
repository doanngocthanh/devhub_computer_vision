package com.devhub.ocr.RPA.A0.RPAA0_0100.trx;

public class LogEvent {
    private final String jobId;
    private final String message;
    private final long ts;

    public LogEvent(String jobId, String message, long ts) {
        this.jobId = jobId;
        this.message = message;
        this.ts = ts;
    }

    public String getJobId() {
        return jobId;
    }

    public String getMessage() {
        return message;
    }

    public long getTs() {
        return ts;
    }
}
