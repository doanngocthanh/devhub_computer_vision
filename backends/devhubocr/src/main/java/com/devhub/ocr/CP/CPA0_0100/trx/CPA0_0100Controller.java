package com.devhub.ocr.CP.CPA0_0100.trx;

import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.devhub.ocr.CP.CPA0_0100.mod.CPA0_0100Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.awt.Rectangle;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/CP/A0/CPA0_0100")
public class CPA0_0100Controller {

    private final CPA0_0100Service service;

    @Autowired
    public CPA0_0100Controller(CPA0_0100Service service) {
        this.service = service;
    }

    @GetMapping("")
    public String index(Model model) {
        return "html/CP/A0/CPA0_0100/index";
    }

    @PostMapping("/upload-and-ocr")
    @ResponseBody
    public Map<String, Object> uploadAndOcr(@RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "lang", required = false) String lang) throws IOException {
        Map<String, Object> resp = new HashMap<>();
        if (file == null || file.isEmpty()) {
            resp.put("ok", false);
            resp.put("error", "No file uploaded");
            return resp;
        }

        Path uploadsDir = Path.of("uploads", "cpa0");
        Files.createDirectories(uploadsDir);
        Path dst = uploadsDir.resolve(System.currentTimeMillis() + "-" + file.getOriginalFilename());
        try {
            FileCopyUtils.copy(file.getInputStream(), Files.newOutputStream(dst, StandardOpenOption.CREATE_NEW));
            String text = service.doOCR(dst.toFile(), lang);
            resp.put("ok", true);
            resp.put("text", text);
            // return both a file-serving URL and the raw path (for bbox OCR requests)
            String encoded = URLEncoder.encode(dst.toString(), StandardCharsets.UTF_8);
            resp.put("fileUrl", "/CP/A0/CPA0_0100/file?path=" + encoded);
            resp.put("rawPath", dst.toString());
        } catch (UnsatisfiedLinkError e) {
            resp.put("ok", false);
            resp.put("error", "Native Tesseract library not found: " + e.getMessage());
        } catch (TesseractException e) {
            resp.put("ok", false);
            resp.put("error", "OCR failed: " + e.getMessage());
        } catch (IOException e) {
            resp.put("ok", false);
            resp.put("error", "IO error: " + e.getMessage());
        }
        return resp;
    }

    @PostMapping("/ocr-bbox")
    @ResponseBody
    public Map<String, Object> ocrBbox(@RequestBody OcrBboxRequest req) {
        Map<String, Object> resp = new HashMap<>();
        try {
            File f = new File(req.getPath());
            if (!f.exists()) {
                resp.put("ok", false);
                resp.put("error", "File not found: " + req.getPath());
                return resp;
            }
            Rectangle bbox = new Rectangle(req.getX(), req.getY(), req.getW(), req.getH());
            String text = service.doOCR(f, bbox, req.getLang());
            resp.put("ok", true);
            resp.put("text", text);
        } catch (UnsatisfiedLinkError e) {
            resp.put("ok", false);
            resp.put("error", "Native Tesseract library not found: " + e.getMessage());
        } catch (TesseractException e) {
            resp.put("ok", false);
            resp.put("error", "OCR failed: " + e.getMessage());
        } catch (IOException e) {
            resp.put("ok", false);
            resp.put("error", "IO error: " + e.getMessage());
        } catch (Exception e) {
            resp.put("ok", false);
            resp.put("error", e.getMessage());
        }
        return resp;
    }

    @GetMapping("/status")
    @ResponseBody
    public Map<String, Object> status() {
        Map<String, Object> resp = new HashMap<>();
        try {
            boolean ok = service.isNativeAvailable();
            resp.put("ok", ok);
            resp.put("nativeAvailable", ok);
            if (!ok) resp.put("message", "Native Tesseract library (libtesseract) not available");
        } catch (Exception e) {
            resp.put("ok", false);
            resp.put("nativeAvailable", false);
            resp.put("message", e.getMessage());
        }
        return resp;
    }

    @GetMapping("/file")
    @ResponseBody
    public byte[] serveFile(@RequestParam("path") String path) throws IOException {
        File f = new File(path);
        if (!f.exists()) {
            throw new IOException("File not found: " + path);
        }
        return Files.readAllBytes(f.toPath());
    }
}
