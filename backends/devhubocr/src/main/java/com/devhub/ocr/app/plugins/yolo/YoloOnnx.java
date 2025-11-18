package com.devhub.ocr.app.plugins.yolo;


import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import javax.imageio.ImageIO;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

/**
 * Abstract base class for YOLOv8 ONNX detection
 * Supports different models and automatic image dimension handling
 */
public abstract class YoloOnnx {

    // Model configuration
    protected int targetWidth = 640;
    protected int targetHeight = 640;
    protected final int channels = 3;
    protected float confThreshold = 0.25f;
    protected float nmsThreshold = 0.45f;

    // Normalization parameters
    protected float[] mean = { 0.0f, 0.0f, 0.0f };
    protected float[] std = { 1.0f, 1.0f, 1.0f };

    // ONNX Runtime objects
    protected OrtEnvironment env;
    protected OrtSession session;
    protected String modelPath;

    /**
     * Detection result class
     */
    public static class Detection {
        public float x1, y1, x2, y2;
        public float confidence;
        public int classId;
        public String className;

        public Detection(float x1, float y1, float x2, float y2, float confidence, int classId, String className) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.confidence = confidence;
            this.classId = classId;
            this.className = className;
        }

        @Override
        public String toString() {
            return String.format("Detection[x1=%.1f, y1=%.1f, x2=%.1f, y2=%.1f, conf=%.3f, class=%d(%s)]",
                    x1, y1, x2, y2, confidence, classId, className);
        }

        public float getWidth() {
            return x2 - x1;
        }

        public float getHeight() {
            return y2 - y1;
        }

        public float getCenterX() {
            return (x1 + x2) / 2;
        }

        public float getCenterY() {
            return (y1 + y2) / 2;
        }
    }

    /**
     * Constructor
     */
    public YoloOnnx(String modelPath) {
        this.modelPath = modelPath;
        initializeModel();
    }

    /**
     * Constructor with custom parameters
     */
    public YoloOnnx(String modelPath, int targetWidth, int targetHeight, float confThreshold) {
        this.modelPath = modelPath;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.confThreshold = confThreshold;
        initializeModel();
    }

    /**
     * Initialize ONNX model
     */
    private void initializeModel() {
        try {
            this.env = OrtEnvironment.getEnvironment();
            this.session = env.createSession(modelPath, new OrtSession.SessionOptions());

            System.out.println("Model loaded successfully: " + modelPath);
            System.out.println("Input names: " + session.getInputNames());
            System.out.println("Output names: " + session.getOutputNames());

            // Allow subclasses to configure model-specific parameters
            configureModel();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load ONNX model: " + modelPath, e);
        }
    }

    /**
     * Abstract method for subclasses to configure model-specific parameters
     */
    protected abstract void configureModel();

    /**
     * Abstract method for subclasses to define class names
     */
    protected abstract String[] getClassNames();

    /**
     * Detect objects in image from file path
     */
    public Detection[] detect(String imagePath) throws IOException {
        BufferedImage image = ImageIO.read(new File(imagePath));
        return detect(image);
    }

    /**
     * Detect objects in BufferedImage
     */
    public Detection[] detect(BufferedImage originalImage) {
        try {
            System.out.println("Processing image: " + originalImage.getWidth() + "x" + originalImage.getHeight());

            // Auto-calculate target dimensions maintaining aspect ratio
            calculateOptimalDimensions(originalImage.getWidth(), originalImage.getHeight());

            // Preprocess image
            BufferedImage resizedImage = resizeImage(originalImage, targetWidth, targetHeight);
            float[] inputData = imageToTensorData(resizedImage, mean, std);

            // Create tensor and run inference
            long[] shape = new long[] { 1, channels, targetHeight, targetWidth };

            try (OnnxTensor tensor = OnnxTensor.createTensor(env, java.nio.FloatBuffer.wrap(inputData), shape)) {

                String inputName = session.getInputNames().iterator().next();
                Map<String, OnnxTensor> inputMap = Collections.singletonMap(inputName, tensor);

                long startTime = System.currentTimeMillis();
                OrtSession.Result result = session.run(inputMap);
                long endTime = System.currentTimeMillis();

                System.out.println("Inference time: " + (endTime - startTime) + "ms");

                // Process output
                String outputName = session.getOutputNames().iterator().next();
                Object outputValue = result.get(outputName);

                Detection[] detections = processOutput(outputValue, originalImage.getWidth(),
                        originalImage.getHeight());

                result.close();
                return detections;
            }

        } catch (Exception e) {
            throw new RuntimeException("Detection failed", e);
        }
    }

    /**
     * Calculate optimal dimensions maintaining aspect ratio
     */
    protected void calculateOptimalDimensions(int originalWidth, int originalHeight) {
        // Default behavior: use fixed target dimensions
        // Subclasses can override for different strategies

        float aspectRatio = (float) originalWidth / originalHeight;

        if (aspectRatio > 1.0) {
            // Landscape: fix width, adjust height
            targetWidth = 640;
            targetHeight = Math.round(640 / aspectRatio);
            // Ensure height is multiple of 32 (common YOLO requirement)
            targetHeight = ((targetHeight + 31) / 32) * 32;
        } else {
            // Portrait: fix height, adjust width
            targetHeight = 640;
            targetWidth = Math.round(640 * aspectRatio);
            // Ensure width is multiple of 32
            targetWidth = ((targetWidth + 31) / 32) * 32;
        }

        System.out.println("Auto-calculated target dimensions: " + targetWidth + "x" + targetHeight);
    }

    /**
     * Process model output - can be overridden by subclasses
     */
    protected Detection[] processOutput(Object outputValue, int originalWidth, int originalHeight) {
        try {
            System.out.println("Processing output value type: " + outputValue.getClass().getName());

            OnnxTensor outputTensor = extractTensorFromOutput(outputValue);
            long[] shape = outputTensor.getInfo().getShape();
            System.out.println("Output shape: " + Arrays.toString(shape));

            if (shape.length == 3) {
                System.out.println("Processing 3D output tensor");
                float[][][] output = (float[][][]) outputTensor.getValue();
                return processDetections(output[0], originalWidth, originalHeight);
            } else if (shape.length == 2) {
                System.out.println("Processing 2D output tensor");
                float[][] output = (float[][]) outputTensor.getValue();
                return processDetections(output, originalWidth, originalHeight);
            } else if (shape.length == 4) {
                System.out.println("Processing 4D output tensor");
                float[][][][] output = (float[][][][]) outputTensor.getValue();
                // For 4D output, typically [batch, classes+coords, height, width] or [batch,
                // anchors, coords+classes]
                if (output.length > 0 && output[0].length > 0) {
                    // Convert 4D to 2D: [classes+coords, num_detections]
                    int numClasses = output[0].length;
                    int numDetections = output[0][0].length * output[0][0][0].length;
                    float[][] converted = new float[numClasses][numDetections];

                    int idx = 0;
                    for (int h = 0; h < output[0][0].length; h++) {
                        for (int w = 0; w < output[0][0][0].length; w++) {
                            for (int c = 0; c < numClasses; c++) {
                                converted[c][idx] = output[0][c][h][w];
                            }
                            idx++;
                        }
                    }
                    return processDetections(converted, originalWidth, originalHeight);
                }
            } else {
                System.out.println("Unsupported output shape format: " + Arrays.toString(shape));
                return new Detection[0];
            }

        } catch (Exception e) {
            System.err.println("Error processing output: " + e.getMessage());
            e.printStackTrace();
            return new Detection[0];
        }

        return new Detection[0];
    }

    /**
     * Extract tensor from various output formats
     */
    private OnnxTensor extractTensorFromOutput(Object outputValue) {
        if (outputValue instanceof java.util.Optional) {
            java.util.Optional<?> optional = (java.util.Optional<?>) outputValue;
            if (optional.isPresent() && optional.get() instanceof OnnxTensor) {
                return (OnnxTensor) optional.get();
            }
            throw new RuntimeException("Optional output is empty or not an OnnxTensor");
        } else if (outputValue instanceof OnnxTensor) {
            return (OnnxTensor) outputValue;
        } else {
            throw new RuntimeException("Unexpected output type: " + outputValue.getClass());
        }
    }

    /**
     * Process detections from YOLOv8 output
     */
    protected Detection[] processDetections(float[][] detections, int originalWidth, int originalHeight) {
        float scaleX = (float) originalWidth / targetWidth;
        float scaleY = (float) originalHeight / targetHeight;

        int numDetections = detections[0].length;
        int numClasses = detections.length - 4;
        System.out.println("Number of detections: " + numDetections);
        System.out.println("Number of classes: " + numClasses);
        System.out.println("Confidence threshold: " + confThreshold);

        // Update class names if needed
        String[] classNames = getClassNames();
        if (classNames.length != numClasses) {
            System.out.println("Updating class names from " + classNames.length + " to " + numClasses + " classes");
            classNames = updateClassNames(numClasses);
        }

        java.util.List<Detection> validDetections = new java.util.ArrayList<>();

        for (int i = 0; i < numDetections; i++) {
            float centerX = detections[0][i];
            float centerY = detections[1][i];
            float width = detections[2][i];
            float height = detections[3][i];

            // Find the class with highest confidence
            int bestClass = 0;
            float maxClassConf = detections[4][i];

            for (int c = 1; c < numClasses; c++) {
                float classConf = detections[4 + c][i];
                if (classConf > maxClassConf) {
                    maxClassConf = classConf;
                    bestClass = c;
                }
            }

            if (maxClassConf > confThreshold) {
                // Convert from center format to corner format
                float x1 = (centerX - width / 2) * scaleX;
                float y1 = (centerY - height / 2) * scaleY;
                float x2 = (centerX + width / 2) * scaleX;
                float y2 = (centerY + height / 2) * scaleY;

                // Ensure coordinates are within image bounds
                x1 = Math.max(0, Math.min(x1, originalWidth));
                y1 = Math.max(0, Math.min(y1, originalHeight));
                x2 = Math.max(0, Math.min(x2, originalWidth));
                y2 = Math.max(0, Math.min(y2, originalHeight));

                String className = (bestClass < classNames.length) ? classNames[bestClass] : "Unknown";

                Detection detection = new Detection(x1, y1, x2, y2, maxClassConf, bestClass, className);
                validDetections.add(detection);
            }
        }
        System.out.println("Valid detections before NMS: " + validDetections.size());

        // Apply NMS to remove overlapping detections
        java.util.List<Detection> nmsDetections = applyNMS(validDetections, nmsThreshold);

        // Sort by confidence (highest first) for better presentation
        nmsDetections.sort((a, b) -> Float.compare(b.confidence, a.confidence));

        System.out.println("Final detections after NMS: " + nmsDetections.size());

        // Log details of final detections
        for (Detection det : nmsDetections) {
            System.out.println("  " + det.className + " (class " + det.classId + "): confidence=" +
                    String.format("%.3f", det.confidence) +
                    ", bbox=[" + (int) det.x1 + "," + (int) det.y1 + "," + (int) det.x2 + "," + (int) det.y2 + "]");
        }

        return nmsDetections.toArray(new Detection[0]);
    }

    /**
     * Apply Non-Maximum Suppression to remove overlapping detections
     */
    private java.util.List<Detection> applyNMS(java.util.List<Detection> detections, float nmsThreshold) {
        if (detections.isEmpty())
            return detections;

        // Sort detections by confidence (highest first)
        detections.sort((a, b) -> Float.compare(b.confidence, a.confidence));

        java.util.List<Detection> result = new java.util.ArrayList<>();
        boolean[] suppressed = new boolean[detections.size()];

        for (int i = 0; i < detections.size(); i++) {
            if (suppressed[i])
                continue;

            Detection current = detections.get(i);
            result.add(current);

            // Suppress overlapping detections
            for (int j = i + 1; j < detections.size(); j++) {
                if (suppressed[j])
                    continue;

                Detection other = detections.get(j);
                float iou = calculateIoU(current, other);

                if (iou > nmsThreshold) {
                    suppressed[j] = true;
                }
            }
        }

        return result;
    }

    /**
     * Calculate Intersection over Union (IoU) between two detections
     */
    private float calculateIoU(Detection a, Detection b) {
        // Calculate intersection
        float intersectionX1 = Math.max(a.x1, b.x1);
        float intersectionY1 = Math.max(a.y1, b.y1);
        float intersectionX2 = Math.min(a.x2, b.x2);
        float intersectionY2 = Math.min(a.y2, b.y2);

        if (intersectionX2 <= intersectionX1 || intersectionY2 <= intersectionY1) {
            return 0.0f; // No intersection
        }

        float intersectionArea = (intersectionX2 - intersectionX1) * (intersectionY2 - intersectionY1);

        // Calculate union
        float areaA = (a.x2 - a.x1) * (a.y2 - a.y1);
        float areaB = (b.x2 - b.x1) * (b.y2 - b.y1);
        float unionArea = areaA + areaB - intersectionArea;
        return intersectionArea / unionArea;
    }

    /**
     * Resize image with high quality
     */
    protected BufferedImage resizeImage(BufferedImage original, int targetWidth, int targetHeight) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return resized;
    }

    /**
     * Convert BufferedImage to tensor data in NCHW format
     */
    protected float[] imageToTensorData(BufferedImage image, float[] mean, float[] std) {
        int width = image.getWidth();
        int height = image.getHeight();

        float[] tensorData = new float[channels * height * width];
        int idx = 0;

        for (int c = 0; c < channels; c++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = image.getRGB(x, y);
                    float value;

                    if (c == 0) {
                        value = ((rgb >> 16) & 0xFF) / 255.0f; // R
                    } else if (c == 1) {
                        value = ((rgb >> 8) & 0xFF) / 255.0f; // G
                    } else {
                        value = (rgb & 0xFF) / 255.0f; // B
                    }

                    value = (value - mean[c]) / std[c];
                    tensorData[idx++] = value;
                }
            }
        }

        return tensorData;
    }

    /**
     * Close resources
     */
    public void close() {
        try {
            if (session != null) {
                session.close();
            }
            if (env != null) {
                env.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Getters and setters
    public void setConfThreshold(float confThreshold) {
        this.confThreshold = confThreshold;
    }

    public void setNmsThreshold(float nmsThreshold) {
        this.nmsThreshold = nmsThreshold;
    }

    public void setNormalization(float[] mean, float[] std) {
        this.mean = mean.clone();
        this.std = std.clone();
    }

    public int getTargetWidth() {
        return targetWidth;
    }

    public int getTargetHeight() {
        return targetHeight;
    }

    /**
     * Update class names based on actual number of classes detected from model
     * output
     */
    protected String[] updateClassNames(int numClasses) {
        // This method can be overridden by subclasses to provide specific class names
        // For now, generate generic class names
        String[] newClassNames = new String[numClasses];
        for (int i = 0; i < numClasses; i++) {
            newClassNames[i] = "class_" + i;
        }
        return newClassNames;
    }
}