package com.devhub.ocr.RPA.A0.RPAA0_0100.trx;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * PrintStream that tees output to the original PrintStream and also sends
 * completed lines to LogBroker with a jobId.
 */
public class JobLogPrintStream extends PrintStream {
    private final PrintStream original;
    private final String jobId;
    private final StringBuilder buffer = new StringBuilder();

    public JobLogPrintStream(PrintStream original, String jobId) {
        super(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                // no-op: we override write methods below
            }
        });
        this.original = original;
        this.jobId = jobId;
    }

    @Override
    public void write(byte[] b, int off, int len) {
        // forward to original
        original.write(b, off, len);
        String s = new String(b, off, len, StandardCharsets.UTF_8);
        appendAndEmit(s);
    }

    @Override
    public void write(int b) {
        original.write(b);
        appendAndEmit(new String(new byte[] { (byte) b }, StandardCharsets.UTF_8));
    }

    private void appendAndEmit(String s) {
        for (char c : s.toCharArray()) {
            buffer.append(c);
            if (c == '\n') {
                String line = buffer.toString();
                // trim trailing newline
                line = line.replaceAll("\\r?\\n$", "");
                LogBroker.get().publishLog(jobId, line);
                buffer.setLength(0);
            }
        }
    }

    @Override
    public void flush() {
        original.flush();
    }

    @Override
    public void close() {
        original.close();
    }
}
