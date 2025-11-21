package com.devhub.ocr.CP.CPA0_0100.mod;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.awt.Rectangle;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
 
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CPA0_0100Service {

    private static final Logger log = LoggerFactory.getLogger(CPA0_0100Service.class);

    private String datapath;
    private final String defaultLang;

    public CPA0_0100Service(@Value("${tess4j.data.path:}") String datapath,
                            @Value("${tess4j.lang:eng}") String defaultLang) {
        this.datapath = datapath == null ? "" : datapath.trim();
        this.defaultLang = defaultLang == null || defaultLang.isBlank() ? "eng" : defaultLang;

        // If no datapath configured, attempt to locate traineddata under project resources
        if (this.datapath.isBlank()) {
            try {
                String local = locateLocalTessdata();
                if (local != null && !local.isBlank()) {
                    this.datapath = local;
                    log.info("Using tessdata from: {}", this.datapath);
                }
            } catch (Exception e) {
                log.warn("Unable to locate local tessdata: {}", e.getMessage());
            }
        }
    }

    private String locateLocalTessdata() throws IOException {
        // 1) Check src/main/resources/static/models (useful in dev)
        Path devPath = Path.of("src", "main", "resources", "static", "models");
        if (Files.exists(devPath) && Files.isDirectory(devPath)) {
            // ensure at least one .traineddata exists
            try (var s = Files.list(devPath)) {
                if (s.anyMatch(p -> p.getFileName().toString().endsWith(".traineddata"))) {
                    return devPath.toAbsolutePath().toString();
                }
            }
        }

        // 2) Check target/classes/static/models (when running from IDE/build)
        Path targetPath = Path.of("target", "classes", "static", "models");
        if (Files.exists(targetPath) && Files.isDirectory(targetPath)) {
            try (var s = Files.list(targetPath)) {
                if (s.anyMatch(p -> p.getFileName().toString().endsWith(".traineddata"))) {
                    return targetPath.toAbsolutePath().toString();
                }
            }
        }

        // 3) Try to extract a known resource from classpath (e.g., /static/models/vie.traineddata or defaultLang)
        String[] candidates = new String[]{this.defaultLang + ".traineddata", "vie.traineddata"};
        for (String name : candidates) {
            String resPath = "/static/models/" + name;
            try (InputStream is = getClass().getResourceAsStream(resPath)) {
                if (is != null) {
                    Path tmp = Files.createTempDirectory("tessdata-");
                    Path out = tmp.resolve(name);
                    Files.copy(is, out, StandardCopyOption.REPLACE_EXISTING);
                    // return the temp directory as tessdata path
                    return tmp.toAbsolutePath().toString();
                }
            }
        }

        return null;
    }

    private ITesseract createTesseract(String lang) {
        Tesseract t = new Tesseract();
        if (datapath != null && !datapath.isBlank()) {
            t.setDatapath(datapath);
        }
        t.setLanguage((lang == null || lang.isBlank()) ? defaultLang : lang);
        return t;
    }

    /**
     * Run OCR on an image file and return recognized text.
     */
    public String doOCR(File imageFile, String lang) throws IOException, TesseractException {
        BufferedImage img = ImageIO.read(imageFile);
        if (img == null) {
            throw new IOException("Unable to read image file: " + imageFile.getAbsolutePath());
        }
        ITesseract t = createTesseract(lang);
        try {
            return t.doOCR(img);
        } catch (UnsatisfiedLinkError e) {
            log.error("Tesseract native library load failed: {}", e.toString());
            throw new TesseractException("Native Tesseract library not found or failed to load. " +
                    "Install Tesseract (libtesseract) on the system or provide native libs. " +
                    "See HELP.md for platform instructions. Details: " + e.getMessage());
        }
    }

    /**
     * Run OCR on a cropped bounding box region of the image.
     */
    public String doOCR(File imageFile, Rectangle bbox, String lang) throws IOException, TesseractException {
        BufferedImage img = ImageIO.read(imageFile);
        if (img == null) {
            throw new IOException("Unable to read image file: " + imageFile.getAbsolutePath());
        }
        int x = Math.max(0, bbox.x);
        int y = Math.max(0, bbox.y);
        int w = Math.min(bbox.width, img.getWidth() - x);
        int h = Math.min(bbox.height, img.getHeight() - y);
        if (w <= 0 || h <= 0) {
            throw new IOException("Invalid bounding box or out of image bounds");
        }
        BufferedImage sub = img.getSubimage(x, y, w, h);
        ITesseract t = createTesseract(lang);
        try {
            return t.doOCR(sub);
        } catch (UnsatisfiedLinkError e) {
            log.error("Tesseract native library load failed: {}", e.toString());
            throw new TesseractException("Native Tesseract library not found or failed to load. " +
                    "Install Tesseract (libtesseract) on the system or provide native libs. " +
                    "See HELP.md for platform instructions. Details: " + e.getMessage());
        }
    }

    /**
     * Quick native check: attempts a tiny OCR call to detect native library availability.
     */
    public boolean isNativeAvailable() {
        try {
            ITesseract t = createTesseract(null);
            // tiny blank image
            BufferedImage b = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
            try {
                t.doOCR(b);
                return true;
            } catch (TesseractException ex) {
                // if TesseractException happens but native libs loaded, consider available
                return true;
            }
        } catch (UnsatisfiedLinkError e) {
            return false;
        } catch (Throwable e) {
            // any other error assume native is not available
            log.warn("Native availability probe failed: {}", e.toString());
            return false;
        }
    }
}
