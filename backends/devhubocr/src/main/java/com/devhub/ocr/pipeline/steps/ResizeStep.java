package com.devhub.ocr.pipeline.steps;

import com.devhub.ocr.pipeline.PipelineParam;
import com.devhub.ocr.pipeline.PipelineResult;
import com.devhub.ocr.pipeline.PipelineStep;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ResizeStep implements PipelineStep {

    @Override
    public String getName() { return "ResizeStep"; }

    @Override
    public List<PipelineParam> getInputParams() {
        return Arrays.asList(new PipelineParam("file","file",true), new PipelineParam("width","int",false), new PipelineParam("height","int",false));
    }

    @Override
    public List<PipelineParam> getOutputParams() {
        return Arrays.asList(new PipelineParam("file","file",true));
    }

    @Override
    public PipelineResult execute(Map<String, Object> input) throws Exception {
        Object fobj = input.get("file");
        if (fobj == null) throw new IllegalArgumentException("file input required");
        File srcFile;
        if (fobj instanceof File) srcFile = (File) fobj;
        else srcFile = new File(String.valueOf(fobj));

        int width = input.getOrDefault("width", 320) instanceof Number ? ((Number)input.get("width")).intValue() : 320;
        int height = input.getOrDefault("height", 240) instanceof Number ? ((Number)input.get("height")).intValue() : 240;

        File dest = new File(srcFile.getParentFile(), "resized_" + System.currentTimeMillis() + "_" + srcFile.getName());
        try {
            BufferedImage img = ImageIO.read(srcFile);
            if (img == null) throw new IOException("Cannot read image: " + srcFile);
            Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resized.createGraphics();
            g2d.drawImage(tmp, 0, 0, null);
            g2d.dispose();
            ImageIO.write(resized, "jpg", dest);
        } catch (IOException ex) {
            // fallback: copy file
            try { java.nio.file.Files.copy(srcFile.toPath(), dest.toPath()); } catch (IOException e) { /* ignore */ }
        }

        PipelineResult r = new PipelineResult();
        Map<String,Object> out = new HashMap<>();
        out.put("file", dest);
        r.setOutput(out);
        return r;
    }
}
