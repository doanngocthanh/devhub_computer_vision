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
public class AutoLabelStep implements PipelineStep {

    @Override
    public String getName() { return "AutoLabelStep"; }

    @Override
    public List<PipelineParam> getInputParams() {
        return Arrays.asList(new PipelineParam("image","buffer",true), new PipelineParam("bbox","json",true));
    }

    @Override
    public List<PipelineParam> getOutputParams() {
        return Arrays.asList(new PipelineParam("labels","json",true));
    }

    @Override
    public PipelineResult execute(Map<String, Object> input) throws Exception {
        PipelineResult r = new PipelineResult();
        Map<String,Object> out = new HashMap<>();
        out.put("labels", "[{\"label\":\"text\",\"score\":0.98}]");
        r.setOutput(out);
        return r;
    }
}
