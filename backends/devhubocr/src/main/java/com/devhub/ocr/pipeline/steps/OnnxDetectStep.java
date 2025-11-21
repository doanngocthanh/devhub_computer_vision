package com.devhub.ocr.pipeline.steps;

import com.devhub.ocr.pipeline.PipelineParam;
import com.devhub.ocr.pipeline.PipelineResult;
import com.devhub.ocr.pipeline.PipelineStep;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OnnxDetectStep implements PipelineStep {

    @Override
    public String getName() { return "OnnxDetectStep"; }

    @Override
    public List<PipelineParam> getInputParams() {
        return Arrays.asList(new PipelineParam("image","buffer",true));
    }

    @Override
    public List<PipelineParam> getOutputParams() {
        return Arrays.asList(new PipelineParam("bbox","json",true));
    }

    @Override
    public PipelineResult execute(Map<String, Object> input) throws Exception {
        PipelineResult r = new PipelineResult();
        // Demo: return a fake bounding box
        Map<String,Object> out = new HashMap<>();
        out.put("bbox", "[{\"x\":10,\"y\":10,\"w\":100,\"h\":50}]");
        r.setOutput(out);
        return r;
    }
}
