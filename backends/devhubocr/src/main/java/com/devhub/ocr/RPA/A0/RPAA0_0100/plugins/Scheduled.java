package com.devhub.ocr.RPA.A0.RPAA0_0100.plugins;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.UUID;
import java.io.PrintStream;

import com.devhub.ocr.RPA.A0.RPAA0_0100.trx.JobLogPrintStream;
import com.devhub.ocr.RPA.A0.RPAA0_0100.trx.LogBroker;

public class Scheduled {
    private ScheduledExecutorService scheduler;
    // primary maps keyed by jobId (UUID strings)
    private Map<String, ScheduledFuture<?>> jobFutures;
    private Map<String, JobParams> jobParams;
    // className -> list of jobIds (index)
    private Map<String, List<String>> classIndex;
    // default params set per class (when caller uses setParams/setFile before
    // execute)
    private Map<String, JobParams> classDefaultParams;

    public Scheduled() {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.jobFutures = new ConcurrentHashMap<>();
        this.jobParams = new ConcurrentHashMap<>();
        this.classIndex = new ConcurrentHashMap<>();
        this.classDefaultParams = new ConcurrentHashMap<>();
    }

    /**
     * Schedule a job and return a jobId (UUID string). If no per-execution params
     * are provided,
     * the class default params (if any) will be copied for this job.
     */
    public String execute(String className, String submitTime) throws ParseException {
        JobParams defaults = classDefaultParams.get(className);
        JobParams params = defaults != null ? copyParams(defaults) : new JobParams();
        return execute(className, submitTime, params);
    }

    /**
     * Schedule a job with explicit JobParams. Returns a jobId string.
     */
    public String execute(String className, String submitTime, JobParams params) throws ParseException {
        long delay = calculateDelay(submitTime);
        // copy params so caller can't mutate after scheduling
        JobParams jobCopy = params != null ? copyParams(params) : new JobParams();
        String jobId = UUID.randomUUID().toString();
        ScheduledFuture<?> scl = scheduler.schedule(new Task(jobId, className, jobCopy), delay,
                TimeUnit.MILLISECONDS);
        jobFutures.put(jobId, scl);
        jobParams.put(jobId, jobCopy);
        classIndex.computeIfAbsent(className, k -> Collections.synchronizedList(new ArrayList<>())).add(jobId);
        return jobId;
    }

    private JobParams copyParams(JobParams src) {
        JobParams p = new JobParams();
        p.setJsonParams(src.getJsonParams());
        p.setFilePath(src.getFilePath());
        p.setRemainingDelayMillis(src.getRemainingDelayMillis());
        return p;
    }

    /**
     * Attach arbitrary JSON params to a task before executing it. Caller may call
     * this prior to execute(...).
     */
    /**
     * Set default JSON params for a class; used when execute(...) is called without
     * explicit JobParams.
     */
    public void setParams(String className, String json) {
        JobParams params = classDefaultParams.computeIfAbsent(className, k -> new JobParams());
        params.setJsonParams(json);
    }

    /**
     * Attach a file path to a task before executing it. Caller may call this prior
     * to execute(...).
     */
    /**
     * Set default file path for a class; used when execute(...) is called without
     * explicit JobParams.
     */
    public void setFile(String className, String filePath) {
        JobParams params = classDefaultParams.computeIfAbsent(className, k -> new JobParams());
        params.setFilePath(filePath);
    }

    /**
     * Set/override params for a specific jobId (after scheduling or before if you
     * have the id).
     */
    public void setParamsForJob(String jobId, String json) {
        JobParams p = jobParams.computeIfAbsent(jobId, k -> new JobParams());
        p.setJsonParams(json);
    }

    /**
     * Set/override file path for a specific jobId.
     */
    public void setFileForJob(String jobId, String filePath) {
        JobParams p = jobParams.computeIfAbsent(jobId, k -> new JobParams());
        p.setFilePath(filePath);
    }

    public void cancelAllTasks() {
        for (ScheduledFuture<?> sf : jobFutures.values()) {
            sf.cancel(false);
        }
        jobFutures.clear();
        jobParams.clear();
        classIndex.clear();
        classDefaultParams.clear();
    }

    /**
     * Pause a specific job by jobId. Stores remaining delay into that job's
     * JobParams.
     */
    public void pauseJob(String jobId) {
        ScheduledFuture<?> sf = jobFutures.get(jobId);
        if (sf != null) {
            long remaining = sf.getDelay(TimeUnit.MILLISECONDS);
            JobParams p = jobParams.computeIfAbsent(jobId, k -> new JobParams());
            p.setRemainingDelayMillis(remaining);
            sf.cancel(false);
            System.out.println("Job paused: " + jobId + " (remaining ms=" + remaining + ")");
        }
    }

    /**
     * Resume a specific job by jobId. If submitTime is null, uses stored
     * remainingDelay; otherwise uses submitTime.
     */
    public void resumeJob(String jobId, String submitTime) throws ParseException {
        JobParams p = jobParams.get(jobId);
        long delay;
        if (submitTime != null && !submitTime.isBlank()) {
            delay = calculateDelay(submitTime);
        } else if (p != null && p.getRemainingDelayMillis() >= 0) {
            delay = p.getRemainingDelayMillis();
        } else {
            System.out.println("No paused job or submitTime provided for jobId: " + jobId);
            return;
        }

        // need className to reschedule task; find from classIndex reverse lookup
        String className = findClassNameForJob(jobId);
        if (className == null) {
            System.out.println("Cannot find className for jobId: " + jobId);
            return;
        }
        // schedule new task
        ScheduledFuture<?> newSf = scheduler.schedule(new Task(jobId, className, p), delay,
                TimeUnit.MILLISECONDS);
        jobFutures.put(jobId, newSf);
        if (p != null)
            p.setRemainingDelayMillis(-1);
        System.out.println("Job resumed: " + jobId + " (delay ms=" + delay + ")");
    }

    private String findClassNameForJob(String jobId) {
        for (Map.Entry<String, List<String>> e : classIndex.entrySet()) {
            if (e.getValue().contains(jobId))
                return e.getKey();
        }
        return null;
    }

    /**
     * Stop a specific job by jobId (cancels with interrupt) and clean up.
     */
    public void stopJob(String jobId) {
        ScheduledFuture<?> sf = jobFutures.remove(jobId);
        if (sf != null)
            sf.cancel(true);
        jobParams.remove(jobId);
        // remove from classIndex
        classIndex.values().forEach(list -> list.remove(jobId));
        System.out.println("Job stopped: " + jobId);
    }

    /**
     * Stop all jobs for a given className.
     */
    public void stopJobsByClass(String className) {
        List<String> ids = classIndex.getOrDefault(className, Collections.emptyList()).stream()
                .collect(Collectors.toList());
        for (String id : ids)
            stopJob(id);
    }

    // Backwards-compatible helpers that operate on all jobs of a class
    public void pauseTask(String className) {
        List<String> ids = listJobIdsByClass(className);
        for (String id : ids)
            pauseJob(id);
    }

    public void resumeTask(String className, String submitTime) throws ParseException {
        List<String> ids = listJobIdsByClass(className);
        for (String id : ids)
            resumeJob(id, submitTime);
    }

    public void stopTask(String className) {
        stopJobsByClass(className);
    }

    /**
     * Return jobIds for a class.
     */
    public List<String> listJobIdsByClass(String className) {
        return classIndex.getOrDefault(className, Collections.emptyList()).stream().collect(Collectors.toList());
    }

    /**
     * Return ScheduledFuture for a jobId, or null.
     */
    public ScheduledFuture<?> getFuture(String jobId) {
        return jobFutures.get(jobId);
    }

    /**
     * Return JobParams for a jobId, or null.
     */
    public JobParams getJobParams(String jobId) {
        return jobParams.get(jobId);
    }

    private long calculateDelay(String submitTime) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = dateFormat.parse(submitTime);
        return Math.max(0, date.getTime() - System.currentTimeMillis());
    }

    private class Task implements Runnable {
        private final String jobId;
        private final String className;
        private final JobParams jobParams;

        public Task(String jobId, String className, JobParams jobParams) {
            this.jobId = jobId;
            this.className = className;
            this.jobParams = jobParams;
        }

        @Override
        public void run() {
            try {
                Class<?> clazz = Class.forName(className);
                Object instance = clazz.getDeclaredConstructor().newInstance();

                // Redirect System.out to capture logs for this job while it runs.
                PrintStream originalOut = System.out;
                PrintStream originalErr = System.err;
                JobLogPrintStream jobOut = new JobLogPrintStream(originalOut, jobId);
                System.setOut(jobOut);
                System.setErr(jobOut);
                // publish start log
                LogBroker.get().publishLog(jobId, "Job started: " + className);

                // If the job supports receiving JobParams, inject them reflectively
                if (jobParams != null) {
                    try {
                        Method setter = clazz.getMethod("setJobParams", JobParams.class);
                        setter.invoke(instance, jobParams);
                    } catch (NoSuchMethodException ignored) {
                        // try other common setters
                        try {
                            Method fileSetter = clazz.getMethod("setFile", String.class);
                            if (jobParams.getFilePath() != null) {
                                fileSetter.invoke(instance, jobParams.getFilePath());
                            }
                        } catch (NoSuchMethodException ignored2) {
                            try {
                                Method paramsSetter = clazz.getMethod("setParams", String.class);
                                if (jobParams.getJsonParams() != null) {
                                    paramsSetter.invoke(instance, jobParams.getJsonParams());
                                }
                            } catch (NoSuchMethodException ignored3) {
                                // no-op: job doesn't accept params via setters
                            }
                        }
                    }
                }

                // Call standard execute(String) method if present
                try {
                    Method executeMethod = clazz.getMethod("execute", String.class);
                    Object result = executeMethod.invoke(instance, clazz.getName());
                    System.out.println("[jobId=" + jobId + "] " + result);
                } catch (NoSuchMethodException nsme) {
                    // fallback: try a run() method (for plain Runnable/Runner implementations)
                    try {
                        Method runMethod = clazz.getMethod("run");
                        runMethod.invoke(instance);
                    } catch (NoSuchMethodException ex) {
                        System.out.println("No executable method found on class: " + className + " for jobId=" + jobId);
                    }
                }
                // flush output and restore System.out
                try {
                    jobOut.flush();
                } catch (Exception ignore) {
                }
                System.setOut(originalOut);
                System.setErr(originalErr);
                LogBroker.get().publishLog(jobId, "Job finished: " + className);
            } catch (Exception e) {
                // ensure we restore System.out even on error
                try {
                    System.out.flush();
                } catch (Exception ignore) {}
                e.printStackTrace();
                LogBroker.get().publishLog(jobId, "Job failed: " + e.getMessage());
            }
        }
    }
}