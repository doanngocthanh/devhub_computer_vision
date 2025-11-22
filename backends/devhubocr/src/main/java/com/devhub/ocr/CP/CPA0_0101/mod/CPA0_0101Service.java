package com.devhub.ocr.CP.CPA0_0101.mod;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;

import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
// OpenCV imports (used if native OpenCV is available)
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Service that performs image preprocessing (deskew, rotate), detects text regions and runs Tess4J OCR.
 */
public class CPA0_0101Service {

    // Try to load OpenCV native library once
    static {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        } catch (UnsatisfiedLinkError e) {
            // ignore - OpenCV may not be available in the runtime
        }
    }

    private static class DeskewResult {
        double angleDegrees;
        BufferedImage image;
        DeskewResult(double angleDegrees, BufferedImage image) { this.angleDegrees = angleDegrees; this.image = image; }
    }

    public static class Region {
        public int x, y, w, h;
        public String text;

        public Region() {}
        public Region(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
        }
    }

    public static class PreprocessResult {
        public double deskewAngleDegrees;
        public List<Region> regions = new ArrayList<>();
    }

    /**
     * Run the full pipeline: read image, deskew, detect text regions, crop each region and OCR with Tess4J.
     * @param file image file on disk
     * @param lang tesseract language code, e.g. "vie"
     * @return PreprocessResult with detected regions and OCR text
     * @throws IOException on file read errors
     */
    public PreprocessResult process(File file, String lang) throws IOException {
        BufferedImage input = ImageIO.read(file);
        if (input == null) throw new IOException("Cannot read image file: " + file.getAbsolutePath());
        // Attempt OpenCV-based deskew (follows approach: threshold -> findNonZero -> minAreaRect)
        DeskewResult dr = null;
        try {
            dr = deskewWithOpenCV(input);
        } catch (Throwable t) {
            // OpenCV may not be present/failed to load; fall back to PCA-based method below
            dr = null;
        }

        double angleDeg;
        BufferedImage deskewed;
        if (dr != null) {
            angleDeg = dr.angleDegrees;
            deskewed = dr.image;
        } else {
            // Fallback: Convert to GrayU8 using BoofCV helper
            GrayU8 gray = ConvertBufferedImage.convertFrom(input, (GrayU8) null);

            // Simple threshold to boolean foreground mask
            int width = gray.width;
            int height = gray.height;
            boolean[][] fg = new boolean[height][width];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int v = gray.get(x, y) & 0xFF;
                    fg[y][x] = v < 128; // dark pixels as foreground
                }
            }

            // Compute PCA on foreground points to estimate skew angle
            double angleRad = estimateSkewAngleRad(fg);
            angleDeg = Math.toDegrees(angleRad);

            // Rotate image to deskew (use Graphics2D/AffineTransform)
            deskewed = rotateImage(input, -angleRad);
        }

        // Recompute binary mask on deskewed image for region detection
        GrayU8 gray2 = ConvertBufferedImage.convertFrom(deskewed, (GrayU8) null);
        int w2 = gray2.width; int h2 = gray2.height;
        boolean[][] fg2 = new boolean[h2][w2];
        for (int y = 0; y < h2; y++) {
            for (int x = 0; x < w2; x++) {
                int v = gray2.get(x, y) & 0xFF;
                fg2[y][x] = v < 128;
            }
        }

        // Connected component labeling (two-pass) to find bounding boxes
        List<int[]> boxes = connectedComponentBoundingBoxes(fg2);

        // Filter boxes and run OCR per box
        PreprocessResult result = new PreprocessResult();
        result.deskewAngleDegrees = angleDeg;

        ITesseract tesseract = new Tesseract();

        // Resolve tessdata path: prefer TESSDATA_PREFIX, then system property 'tess4j.data.path',
        // otherwise try to use a bundled resource under /static/models and copy it to a temp tessdata dir.
        String tessdataPrefix = System.getenv("TESSDATA_PREFIX");
        if (tessdataPrefix == null || tessdataPrefix.isBlank()) {
            String prop = System.getProperty("tess4j.data.path");
            if (prop != null && !prop.isBlank()) tessdataPrefix = prop;
        }

        java.nio.file.Path tessdataDir = null;
        if (tessdataPrefix != null && !tessdataPrefix.isBlank()) {
            tessdataDir = java.nio.file.Path.of(tessdataPrefix);
        }

        // If tessdataDir is still null or doesn't contain the requested traineddata, try to extract bundled traineddata
        boolean traineddataExists = false;
        if (tessdataDir != null) {
            traineddataExists = java.nio.file.Files.exists(tessdataDir.resolve(lang + ".traineddata"));
        }

        if (!traineddataExists) {
            // Try to find bundled resource under classpath: /static/models/<lang>.traineddata
            String resourcePath = "/static/models/" + lang + ".traineddata";
            java.net.URL res = this.getClass().getResource(resourcePath);
            if (res != null) {
                // copy to temp tessdata dir
                java.nio.file.Path tmp = java.nio.file.Files.createTempDirectory("tessdata-bundled-");
                java.nio.file.Path dest = tmp.resolve(lang + ".traineddata");
                try (java.io.InputStream in = res.openStream()) {
                    java.nio.file.Files.copy(in, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                tessdataDir = tmp;
                traineddataExists = true;
            }
        }

        if (tessdataDir == null || !traineddataExists) {
            throw new IOException("Tessdata for language '" + lang + "' not found. Set TESSDATA_PREFIX or place " +
                    lang + ".traineddata into a tessdata directory. Searched: " + tessdataPrefix + " and classpath:/static/models/");
        }

        // Set datapath to the directory containing the traineddata files
        tesseract.setDatapath(tessdataDir.toAbsolutePath().toString());
        if (lang != null && !lang.isEmpty()) tesseract.setLanguage(lang);

        for (int[] b : boxes) {
            int bx = b[0], by = b[1], bw = b[2], bh = b[3];
            // Basic filters
            if (bw < 16 || bh < 8) continue;
            if (bw * bh < 200) continue;

            // Crop using BufferedImage#getSubimage
            int cx = Math.max(0, bx);
            int cy = Math.max(0, by);
            int cw = Math.min(bw, deskewed.getWidth() - cx);
            int ch = Math.min(bh, deskewed.getHeight() - cy);
            if (cw <= 0 || ch <= 0) continue;

            BufferedImage crop = deskewed.getSubimage(cx, cy, cw, ch);
            String text = "";
            try {
                text = tesseract.doOCR(crop);
            } catch (TesseractException e) {
                text = "" + e.getMessage();
            }

            Region r = new Region(bx, by, bw, bh);
            r.text = text == null ? "" : text.trim();
            result.regions.add(r);
        }

        return result;
    }

    // Estimate skew angle (radians) using PCA on foreground points
    private double estimateSkewAngleRad(boolean[][] fg) {
        int h = fg.length; if (h == 0) return 0;
        int w = fg[0].length;
        long count = 0;
        double sx = 0, sy = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (fg[y][x]) {
                    count++;
                    sx += x;
                    sy += y;
                }
            }
        }
        if (count < 10) return 0;
        double mx = sx / count;
        double my = sy / count;

        double sxx = 0, syy = 0, sxy = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (fg[y][x]) {
                    double dx = x - mx;
                    double dy = y - my;
                    sxx += dx * dx;
                    syy += dy * dy;
                    sxy += dx * dy;
                }
            }
        }

        // Covariance matrix [sxx sxy; sxy syy]
        // Compute principal eigenvector angle
        double theta = 0;
        double denom = sxx - syy;
        if (Math.abs(sxy) < 1e-9 && Math.abs(denom) < 1e-9) {
            theta = 0;
        } else {
            theta = 0.5 * Math.atan2(2 * sxy, denom);
        }
        return theta; // radians
    }

    // Attempt to deskew using OpenCV: threshold -> findNonZero -> minAreaRect
    private DeskewResult deskewWithOpenCV(BufferedImage input) throws IOException {
        // Convert to Mat
        Mat mat = bufferedImageToMat(input);
        if (mat == null || mat.empty()) return new DeskewResult(0.0, input);

        Mat gray = new Mat();
        if (mat.channels() == 3) {
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
        } else if (mat.channels() == 4) {
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY);
        } else {
            gray = mat.clone();
        }

        Mat binary = new Mat();
        Imgproc.threshold(gray, binary, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        // Invert so text is foreground (white) if needed
        Core.bitwise_not(binary, binary);

        MatOfPoint nonZero = new MatOfPoint();
        Core.findNonZero(binary, nonZero);
        if (nonZero.empty()) return new DeskewResult(0.0, input);

        MatOfPoint2f points2f = new MatOfPoint2f(nonZero.toArray());
        RotatedRect box = Imgproc.minAreaRect(points2f);
        double angle = box.angle; // note: angle in degrees, range [-90,0)
        Size sz = box.size;
        // Adjust angle: when box width < height, add 90
        if (sz.width < sz.height) {
            angle = angle + 90.0;
        }

        // Deskew by rotating by -angle
        double rotateAngle = -angle;

        Point center = new Point(mat.cols() / 2.0, mat.rows() / 2.0);
        Mat rot = Imgproc.getRotationMatrix2D(center, rotateAngle, 1.0);
        Mat rotated = new Mat();
        Imgproc.warpAffine(mat, rotated, rot, new Size(mat.cols(), mat.rows()), Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, new Scalar(255,255,255));

        BufferedImage out = matToBufferedImage(rotated);
        return new DeskewResult(rotateAngle, out);
    }

    private Mat bufferedImageToMat(BufferedImage bi) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(bi, "png", baos);
        byte[] bytes = baos.toByteArray();
        Mat mat = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_UNCHANGED);
        return mat;
    }

    private BufferedImage matToBufferedImage(Mat mat) throws IOException {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".png", mat, mob);
        byte[] bytes = mob.toArray();
        return ImageIO.read(new java.io.ByteArrayInputStream(bytes));
    }

    private BufferedImage rotateImage(BufferedImage input, double angleRad) {
        double angle = angleRad;
        double sin = Math.abs(Math.sin(angle));
        double cos = Math.abs(Math.cos(angle));
        int w = input.getWidth();
        int h = input.getHeight();
        int newW = (int) Math.floor(w * cos + h * sin);
        int newH = (int) Math.floor(h * cos + w * sin);

        BufferedImage result = new BufferedImage(newW, newH, input.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : input.getType());
        Graphics2D g = result.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, newW, newH);

        AffineTransform at = new AffineTransform();
        at.translate(newW / 2.0, newH / 2.0);
        at.rotate(angle);
        at.translate(-w / 2.0, -h / 2.0);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawRenderedImage(input, at);
        g.dispose();
        return result;
    }

    // Two-pass connected component labeling returning bounding boxes [x,y,w,h]
    private List<int[]> connectedComponentBoundingBoxes(boolean[][] fg) {
        int h = fg.length; if (h == 0) return Collections.emptyList();
        int w = fg[0].length;
        int[][] labels = new int[h][w];
        int nextLabel = 1;
        int[] parent = new int[w * h / 2 + 10];
        Arrays.fill(parent, -1);

        // first pass
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!fg[y][x]) continue;
                int left = (x > 0 && fg[y][x - 1]) ? labels[y][x - 1] : 0;
                int up = (y > 0 && fg[y - 1][x]) ? labels[y - 1][x] : 0;
                if (left == 0 && up == 0) {
                    labels[y][x] = nextLabel;
                    if (nextLabel >= parent.length) {
                        parent = Arrays.copyOf(parent, parent.length * 2);
                        Arrays.fill(parent, nextLabel, parent.length, -1);
                    }
                    parent[nextLabel] = nextLabel;
                    nextLabel++;
                } else if (left != 0 && up == 0) {
                    labels[y][x] = left;
                } else if (left == 0 && up != 0) {
                    labels[y][x] = up;
                } else {
                    // both exist, pick left and union
                    labels[y][x] = left;
                    int a = left, b = up;
                    union(parent, a, b);
                }
            }
        }

        // second pass: flatten and compute boxes
        Map<Integer, int[]> boxMap = new HashMap<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!fg[y][x]) continue;
                int lab = labels[y][x];
                int root = find(parent, lab);
                labels[y][x] = root;
                int[] b = boxMap.get(root);
                if (b == null) {
                    b = new int[]{x, y, x, y};
                    boxMap.put(root, b);
                } else {
                    if (x < b[0]) b[0] = x;
                    if (y < b[1]) b[1] = y;
                    if (x > b[2]) b[2] = x;
                    if (y > b[3]) b[3] = y;
                }
            }
        }

        List<int[]> boxes = new ArrayList<>();
        for (int[] b : boxMap.values()) {
            int x0 = b[0], y0 = b[1], x1 = b[2], y1 = b[3];
            boxes.add(new int[]{x0, y0, x1 - x0 + 1, y1 - y0 + 1});
        }
        // sort by area descending (big regions first)
        boxes.sort((a, bb) -> Integer.compare(bb[2] * bb[3], a[2] * a[3]));
        return boxes;
    }

    private int find(int[] parent, int a) {
        if (a <= 0 || a >= parent.length) return a;
        int p = parent[a];
        if (p == a) return a;
        int r = find(parent, p);
        parent[a] = r;
        return r;
    }

    private void union(int[] parent, int a, int b) {
        if (a <= 0 || b <= 0) return;
        int ra = find(parent, a);
        int rb = find(parent, b);
        if (ra == rb) return;
        if (ra < rb) parent[rb] = ra; else parent[ra] = rb;
    }

}
