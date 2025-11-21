package com.devhub.ocr.QA.A0.WLA0_0100.mod;

import com.devhub.ocr.QA.A0.WLA0_0100.dto.PipelineDTO;
import com.devhub.ocr.QA.A0.WLA0_0100.dto.PipelineStepDTO;
import com.devhub.ocr.pipeline.PipelineRegistry;
import com.devhub.ocr.app.plugins.database.DatabasePlugin;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.devhub.ocr.pipeline.PipelineResult;
import com.devhub.ocr.pipeline.PipelineStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

@Service
public class WLA0_0100Mod {

    @Autowired
    private PipelineRegistry registry;

    @Autowired
    private DatabasePlugin db;

    private final ObjectMapper mapper = new ObjectMapper();

    // In-memory store for demo purposes
    private Map<Long, PipelineDTO> store = new LinkedHashMap<>();
    private long seq = 1;

    public List<Map<String,Object>> listPipelines() {
        try {
            List<Map<String,Object>> rows = db.query("SELECT id, name FROM pipelines ORDER BY id DESC", null);
            List<Map<String,Object>> out = new ArrayList<>();
            for (Map<String,Object> r : rows) {
                Map<String,Object> m = new HashMap<>();
                m.put("id", r.get("id"));
                m.put("name", r.get("name"));
                out.add(m);
            }
            return out;
        } catch (Exception ex) {
            // fallback to in-memory
            List<Map<String,Object>> out = new ArrayList<>();
            for (PipelineDTO p : store.values()) {
                Map<String,Object> m = new HashMap<>();
                m.put("id", p.getId());
                m.put("name", p.getName());
                out.add(m);
            }
            return out;
        }
    }

    public Map<String,Object> savePipeline(PipelineDTO dto) {
        try {
            String json = mapper.writeValueAsString(dto);
            String now = new Date().toString();
            if (dto.getId() == null) {
                db.execute("INSERT INTO pipelines(name, workflow_json, created_at, updated_at) VALUES(:name, :json, :t, :t)", Map.of("name", dto.getName(), "json", json, "t", now));
                List<Map<String,Object>> rows = db.query("SELECT id FROM pipelines WHERE name = :name ORDER BY id DESC LIMIT 1", Map.of("name", dto.getName()));
                if (!rows.isEmpty()) {
                    Object idv = rows.get(0).get("id");
                    dto.setId(Long.valueOf(String.valueOf(idv)));
                }
            } else {
                db.execute("UPDATE pipelines SET name = :name, workflow_json = :json, updated_at = :t WHERE id = :id", Map.of("name", dto.getName(), "json", json, "t", now, "id", dto.getId()));
            }
            store.put(dto.getId(), dto);
            Map<String,Object> r = new HashMap<>();
            r.put("ok", true);
            r.put("id", dto.getId());
            return r;
        } catch (Exception ex) {
            // fallback to in-memory
            if (dto.getId() == null) dto.setId(seq++);
            store.put(dto.getId(), dto);
            Map<String,Object> r = new HashMap<>();
            r.put("ok", true);
            r.put("id", dto.getId());
            r.put("warning", "db unavailable: " + ex.getMessage());
            return r;
        }
    }

    public Map<String, Object> runPipeline(PipelineDTO dto) {
        return runPipelineWithContext(dto, new HashMap<>());
    }

    public Map<String,Object> runPipelineWithContext(PipelineDTO dto, Map<String,Object> initialContext) {
        Map<String,Object> context = new HashMap<>();
        if (initialContext != null) context.putAll(initialContext);
        try {
            if (dto.getSteps() != null) {
                for (PipelineStepDTO s : dto.getSteps()) {
                    PipelineStep step = registry.getStep(s.getBean());
                    if (step == null) throw new IllegalStateException("Step not found: " + s.getBean());
                    Map<String,Object> input = resolveInput(s.getInput(), context);
                    PipelineResult res = step.execute(input);
                    if (s.getOutputKey() != null && res != null) {
                        context.put(s.getOutputKey(), res.getOutput());
                    }
                }
            }
            // publish any File objects found in context to uploads/pipeline_outputs and replace with public URLs
            publishFiles(context);

            Map<String,Object> r = new HashMap<>();
            r.put("ok", true);
            r.put("result", context);
            return r;
        } catch (Exception ex) {
            Map<String,Object> r = new HashMap<>();
            r.put("ok", false);
            r.put("error", ex.getMessage());
            return r;
        }
    }

    private void publishFiles(Map<String,Object> context) {
        if (context == null) return;
        Path outDir = Paths.get("uploads/pipeline_outputs");
        try {
            Files.createDirectories(outDir);
        } catch (Exception e) {
            return; // cannot publish, skip
        }

        // recursive walker
        walkAndPublish(context, outDir);
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    private void walkAndPublish(Object node, Path outDir) {
        if (node == null) return;
        try {
            if (node instanceof Map) {
                Map map = (Map) node;
                for (Object k : new ArrayList<>(map.keySet())) {
                    Object v = map.get(k);
                    if (v instanceof java.io.File) {
                        java.io.File f = (java.io.File) v;
                        if (f.exists()) {
                            String fname = System.currentTimeMillis() + "-" + java.util.UUID.randomUUID() + "-" + f.getName();
                            Path dest = outDir.resolve(fname);
                            try { Files.copy(f.toPath(), dest); } catch (Exception ex) { continue; }
                            String url = "/A0/WLA0_0100/output?name=" + java.net.URLEncoder.encode(dest.getFileName().toString(), java.nio.charset.StandardCharsets.UTF_8);
                            map.put(k, url);
                        }
                    } else if (v instanceof Map || v instanceof List) {
                        walkAndPublish(v, outDir);
                    }
                }
            } else if (node instanceof List) {
                List list = (List) node;
                for (int i = 0; i < list.size(); i++) {
                    Object v = list.get(i);
                    if (v instanceof java.io.File) {
                        java.io.File f = (java.io.File) v;
                        if (f.exists()) {
                            String fname = System.currentTimeMillis() + "-" + java.util.UUID.randomUUID() + "-" + f.getName();
                            Path dest = outDir.resolve(fname);
                            try { Files.copy(f.toPath(), dest); } catch (Exception ex) { continue; }
                            String url = "/A0/WLA0_0100/output?name=" + java.net.URLEncoder.encode(dest.getFileName().toString(), java.nio.charset.StandardCharsets.UTF_8);
                            list.set(i, url);
                        }
                    } else if (v instanceof Map || v instanceof List) {
                        walkAndPublish(v, outDir);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private Map<String,Object> resolveInput(Map<String,Object> inputTemplate, Map<String,Object> context) {
        if (inputTemplate == null) return Collections.emptyMap();
        Map<String,Object> resolved = new HashMap<>();
        for (Map.Entry<String,Object> e : inputTemplate.entrySet()) {
            Object v = e.getValue();
            if (v instanceof String) {
                String s = (String) v;
                // simple ${var} interpolation
                if (s.startsWith("${") && s.endsWith("}")) {
                    String key = s.substring(2, s.length()-1);
                    Object val = resolveDottedKey(key, context);
                    resolved.put(e.getKey(), val);
                } else {
                    resolved.put(e.getKey(), s);
                }
            } else {
                resolved.put(e.getKey(), v);
            }
        }
        return resolved;
    }
    
    /**
     * Resolve dotted keys like "img.file.path" from nested Maps stored in context.
     */
    private Object resolveDottedKey(String key, Map<String,Object> context) {
        if (key == null || key.length() == 0) return null;
        String[] parts = key.split("\\.");
        Object cur = context.get(parts[0]);
        for (int i = 1; i < parts.length && cur != null; i++) {
            String p = parts[i];
            if (cur instanceof Map) {
                cur = ((Map<?,?>)cur).get(p);
            } else {
                // try to read property via getter? not implemented; return null
                cur = null;
            }
        }
        return cur;
    }
}
