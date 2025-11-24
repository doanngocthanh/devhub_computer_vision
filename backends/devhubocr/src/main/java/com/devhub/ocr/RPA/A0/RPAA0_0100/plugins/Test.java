package com.devhub.ocr.RPA.A0.RPAA0_0100.plugins;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Test extends Runner {
    private JobParams jobParams;
    private String filePath;

    public void setJobParams(JobParams params) {
        this.jobParams = params;
    }

    public void setFile(String file) {
        this.filePath = file;
    }

    @Override
    public void run() throws Exception {
        System.out.println("Hello " + (jobParams != null ? jobParams.getJsonParamsAsObject().get("name").getAsString() : "null"));
        System.out.println("file=" + filePath);
    }

    public static void main(String[] args) {
        Scheduled e1 = new Scheduled();
        try {
            // set default params for class (optional)
            e1.setParams("com.devhub.ocr.RPA.A0.RPAA0_0100.plugins.Test", "{\"name\":\"Thành Nè\"}");

            // schedule job -> returns jobId
            String jobId = e1.execute("com.devhub.ocr.RPA.A0.RPAA0_0100.plugins.Test",
                    "2024-05-22 10:30:00");

            // inspect future
            ScheduledFuture<?> future = e1.getFuture(jobId);
            System.out.println("isDone=" + (future != null ? future.isDone() : "null"));

            // pause/resume/stop by jobId
            e1.pauseJob(jobId);
            Thread.sleep(1000);
            e1.resumeJob(jobId, null); // resume using stored remaining delay
            Thread.sleep(1000);
            e1.stopJob(jobId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
