package com.devhub.ocr.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validator for pipeline step inputs.
 * Supports types: file, image, json, string, int, float, buffer
 */
public class PipelineValidator {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, Object> validate(PipelineStep step, Map<String, Object> input) {
        List<String> errors = new ArrayList<>();

        for (PipelineParam p : step.getInputParams()) {
            Object val = input != null ? input.get(p.getName()) : null;
            if (p.isRequired()) {
                if (val == null || (val instanceof String && ((String) val).isBlank())) {
                    errors.add("Missing required input: " + p.getName());
                    continue;
                }
            }

            if (val == null) continue;

            String t = p.getType() != null ? p.getType().toLowerCase() : "string";
            try {
                switch (t) {
                    case "file":
                    case "image":
                        if (val instanceof String) {
                            File f = new File((String) val);
                            if (!f.exists()) errors.add("File not found for param " + p.getName() + ": " + val);
                        } else if (val instanceof File) {
                            File f = (File) val;
                            if (!f.exists()) errors.add("File not found for param " + p.getName() + ": " + f.getPath());
                        } else {
                            errors.add("Invalid file value for " + p.getName());
                        }
                        break;
                    case "int":
                        if (val instanceof Number) break;
                        if (val instanceof String) Integer.parseInt((String) val);
                        else errors.add("Invalid int value for " + p.getName());
                        break;
                    case "float":
                        if (val instanceof Number) break;
                        if (val instanceof String) Double.parseDouble((String) val);
                        else errors.add("Invalid float value for " + p.getName());
                        break;
                    case "json":
                        if (val instanceof String) mapper.readTree((String) val);
                        // if it's already a Map/List, assume OK
                        break;
                    case "buffer":
                        if (val instanceof String) {
                            try { Base64.getDecoder().decode((String) val); } catch (IllegalArgumentException ex) { errors.add("Invalid base64 buffer for " + p.getName()); }
                        }
                        break;
                    default:
                        // string and other types: nothing to validate here
                        break;
                }
            } catch (Exception ex) {
                errors.add("Invalid value for " + p.getName() + ": " + ex.getMessage());
            }
        }

        Map<String, Object> out = new HashMap<>();
        out.put("ok", errors.isEmpty());
        out.put("errors", errors);
        return out;
    }
}
