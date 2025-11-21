package com.devhub.ocr.pipeline.steps;

import com.devhub.ocr.pipeline.PipelineParam;
import com.devhub.ocr.pipeline.PipelineResult;
import com.devhub.ocr.pipeline.PipelineStep;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LoadImageStep implements PipelineStep {

    @Override
    public String getName() { return "LoadImageStep"; }

    @Override
    public List<PipelineParam> getInputParams() {
        return Arrays.asList(new PipelineParam("path","file",true));
    }

    @Override
    public List<PipelineParam> getOutputParams() {
        return Arrays.asList(new PipelineParam("image","buffer",true));
    }

    @Override
    public PipelineResult execute(Map<String, Object> input) throws Exception {
        PipelineResult r = new PipelineResult();
        Object p = input.get("path");
        if (p == null) throw new IllegalArgumentException("path is required");
        // For demo: return File object as 'image'
        File f = new File(String.valueOf(p));
        Map<String,Object> out = new HashMap<>();
        out.put("file", f);
        r.setOutput(out);
        return r;
    }
}
