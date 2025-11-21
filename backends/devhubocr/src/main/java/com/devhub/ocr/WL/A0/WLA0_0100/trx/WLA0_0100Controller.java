package com.devhub.ocr.WL.A0.WLA0_0100.trx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.devhub.ocr.WL.A0.WLA0_0100.dto.PipelineDTO;
import com.devhub.ocr.WL.A0.WLA0_0100.mod.WLA0_0100Mod;
import com.devhub.ocr.pipeline.PipelineParam;
import com.devhub.ocr.pipeline.PipelineRegistry;
import com.devhub.ocr.pipeline.PipelineStep;
import com.devhub.ocr.pipeline.PipelineResult;
import org.springframework.http.ResponseEntity;
import com.devhub.ocr.pipeline.PipelineValidator;
import java.util.List;

@Controller
@RequestMapping("/A0/WLA0_0100")
public class WLA0_0100Controller {

    @Autowired
    private PipelineRegistry registry;

    @Autowired
    WLA0_0100Mod mod;

    @Autowired
    ObjectMapper mapper;

    @GetMapping("")
    public String index(Model model) {
        model.addAttribute("pipelines", mod.listPipelines());
        return "html/WL/A0/WLA0_0100/index";
    }

    @ResponseBody
    @PostMapping("/save")
    public Map<String,Object> save(@RequestBody PipelineDTO dto) {
        return mod.savePipeline(dto);
    }

    @ResponseBody
    @PostMapping("/run")
    public Map<String,Object> run(@RequestBody PipelineDTO dto) {
        return mod.runPipeline(dto);
    }

    @ResponseBody
    @PostMapping("/run-with-file")
    public Map<String,Object> runWithFile(@RequestParam("file") MultipartFile file, @RequestParam("pipeline") String pipelineJson) throws Exception {
        PipelineDTO dto = mapper.readValue(pipelineJson, PipelineDTO.class);

        Path uploads = Paths.get("uploads/tmp");
        Files.createDirectories(uploads);
        Path dest = uploads.resolve(System.currentTimeMillis() + "-" + file.getOriginalFilename());
        Files.copy(file.getInputStream(), dest);

        Map<String,Object> ctx = new HashMap<>();
        // By convention, pipeline steps can access ${file} (File) and ${file.path} (String)
        ctx.put("file", dest.toFile());
        ctx.put("file.path", dest.toString());

        return mod.runPipelineWithContext(dto, ctx);
    }

    @ResponseBody
    @PostMapping("/run-step")
    public ResponseEntity<?> runStep(@RequestBody com.devhub.ocr.WL.A0.WLA0_0100.trx.RunStepRequest req) {
        try {
            String bean = req.getBean();
            Map<String,Object> input = req.getInput();
            PipelineStep step = registry.getStep(bean);
            if (step == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Unknown step: " + bean));
            }

            Map<String,Object> validation = PipelineValidator.validate(step, input);
            PipelineResult res = step.execute(input);

            Map<String,Object> out = new HashMap<>();
            out.put("validation", validation);
            out.put("output", res != null ? res.getOutput() : Map.of());
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @ResponseBody
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            Path uploads = Paths.get("uploads/files");
            Files.createDirectories(uploads);
            Path dest = uploads.resolve(System.currentTimeMillis() + "-" + file.getOriginalFilename());
            Files.copy(file.getInputStream(), dest);

            Map<String,Object> meta = new HashMap<>();
            meta.put("name", dest.getFileName().toString());
            meta.put("size", Files.size(dest));
            meta.put("url", "/A0/WLA0_0100/file?name=" + dest.getFileName().toString());
            return ResponseEntity.ok(meta);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @ResponseBody
    @GetMapping("/files")
    public List<Map<String,Object>> listFiles() {
        try {
            Path uploads = Paths.get("uploads/files");
            if (!Files.exists(uploads)) return List.of();
            return Files.list(uploads).map(p -> {
                Map<String,Object> m = new HashMap<>();
                try {
                    m.put("name", p.getFileName().toString());
                    m.put("size", Files.size(p));
                    m.put("url", "/A0/WLA0_0100/file?name=" + p.getFileName().toString());
                } catch (Exception e) {
                    m.put("error", e.getMessage());
                }
                return m;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    @GetMapping("/file")
    public ResponseEntity<org.springframework.core.io.Resource> serveFile(@RequestParam("name") String name) {
        try {
            Path p = Paths.get("uploads/files").resolve(name);
            if (!Files.exists(p)) return ResponseEntity.notFound().build();
            org.springframework.core.io.UrlResource resource = new org.springframework.core.io.UrlResource(p.toUri());
            String ct = Files.probeContentType(p);
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + p.getFileName().toString() + "\"")
                    .contentType(org.springframework.http.MediaType.parseMediaType(ct == null ? "application/octet-stream" : ct))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/output")
    public ResponseEntity<org.springframework.core.io.Resource> serveOutput(@RequestParam("name") String name) {
        try {
            Path p = Paths.get("uploads/pipeline_outputs").resolve(name);
            if (!Files.exists(p)) return ResponseEntity.notFound().build();
            org.springframework.core.io.UrlResource resource = new org.springframework.core.io.UrlResource(p.toUri());
            String ct = Files.probeContentType(p);
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + p.getFileName().toString() + "\"")
                    .contentType(org.springframework.http.MediaType.parseMediaType(ct == null ? "application/octet-stream" : ct))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @ResponseBody
    @GetMapping("/steps")
    public List<Map<String,Object>> listSteps() {
        return registry.list().stream().map(s -> {
            Map<String,Object> m = new HashMap<>();
            m.put("bean", s.getClass().getSimpleName());
            m.put("name", s.getName());
            try { m.put("inputs", s.getInputParams()); } catch(Exception e){ m.put("inputs", List.of()); }
            try { m.put("outputs", s.getOutputParams()); } catch(Exception e){ m.put("outputs", List.of()); }
            return m;
        }).collect(Collectors.toList());
    }

    @ResponseBody
    @PostMapping("/validate-pipeline")
    public Map<String,Object> validatePipeline(@RequestBody com.devhub.ocr.WL.A0.WLA0_0100.trx.ValidatePipelineRequest req) {
        try {
            PipelineDTO dto = req.getPipeline();
            Map<String,String> initial = req.getInitialTypes();
            return mod.validatePipeline(dto, initial);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }
}
