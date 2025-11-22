package com.devhub.ocr.CP.CPA0_0101.trx;

import com.devhub.ocr.CP.CPA0_0101.mod.CPA0_0101Service;
import com.devhub.ocr.CP.CPA0_0101.mod.CPA0_0101Service.PreprocessResult;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/CP/A0/CPA0_0101")
public class CPA0_0101Controller {

    private final Path uploadRoot = Path.of("uploads", "cpa0", "CPA0_0101").toAbsolutePath();

    @GetMapping("/")
    public String index() {
        // Return the Thymeleaf template name (rendered by MVC) if used. For raw deployments this may not be used.
        return "html/CP/A0/CPA0_0101/index";
    }

    @PostMapping(value = "/upload-preprocess", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public Map<String, Object> uploadAndPreprocess(@RequestParam("file") MultipartFile file,
                                                    @RequestParam(value = "lang", required = false, defaultValue = "vie") String lang) {
        Map<String, Object> resp = new HashMap<>();
        try {
            Files.createDirectories(uploadRoot);
            String original = file.getOriginalFilename();
            if (original == null) original = "upload-" + System.currentTimeMillis() + ".png";
            Path out = uploadRoot.resolve(System.currentTimeMillis() + "-" + original);
            Files.write(out, file.getBytes(), StandardOpenOption.CREATE_NEW);

            CPA0_0101Service service = new CPA0_0101Service();
            PreprocessResult result = service.process(out.toFile(), lang);

            resp.put("ok", true);
            resp.put("deskewAngleDegrees", result.deskewAngleDegrees);
            resp.put("regions", result.regions);
            resp.put("file", "/uploads/cpa0/CPA0_0101/" + out.getFileName().toString());
            return resp;
        } catch (IOException e) {
            resp.put("ok", false);
            resp.put("error", e.getMessage());
            return resp;
        } catch (Throwable t) {
            resp.put("ok", false);
            resp.put("error", t.getMessage());
            return resp;
        }
    }

}
