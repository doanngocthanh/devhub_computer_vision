package com.devhub.ocr.RPA.A0.RPAA0_0100.plugins;

import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class Runner {
    private int returnCode;
    private String returnMessage;

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public String getReturnMessage() {
        return returnMessage;
    }

    public void setReturnMessage(String returnMessage) {
        this.returnMessage = returnMessage;
    }

    public abstract void run() throws Exception;

    public Runner execute(String className) {
        try {
            this.returnCode = 0;
            Class<?> clazz = Class.forName(className);
            log("JobName: [" + clazz.getSimpleName() + "] is Run");
            run();
            log("JobName: [" + clazz.getSimpleName() + "] is Done");
        } catch (Exception e) {
            this.returnCode = -1;
            this.returnMessage = e.getMessage();
            log("Job ClassName: " + className + " is ERROR");
            e.printStackTrace();
        }
        return this;
    }

    protected String timeLog() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(new Date());
    }

    protected void log(String message) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = dateFormat.format(new Date());
        System.out.println(currentTime + " - " + message);
    }
}
