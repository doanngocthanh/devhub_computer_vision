package com.devhub.ocr.app.plugins.yolo;

import ai.onnxruntime.OrtException;

/**
 * Generic YOLO detector implementation that can work with any YOLO model
 */
public class GenericYOLODetector extends YoloOnnx {

    private String[] classNames;
    private boolean classNamesFromMetadata = false;

    public GenericYOLODetector(String modelPath) {
        super(modelPath);
    }

    public GenericYOLODetector(String modelPath, int targetWidth, int targetHeight, float confThreshold) {
        super(modelPath, targetWidth, targetHeight, confThreshold);
    }

    public GenericYOLODetector(String modelPath, String[] classNames) {
        super(modelPath);
        this.classNames = classNames;
    }

    public GenericYOLODetector(String modelPath, int targetWidth, int targetHeight, float confThreshold,
            String[] classNames) {
        super(modelPath, targetWidth, targetHeight, confThreshold);
        this.classNames = classNames;
    }

    @Override
    protected void configureModel() {
        // Default configuration - can be customized per model
        System.out.println("Configuring generic YOLO model with default settings");

        // Force fixed target dimensions to match model requirements
        targetWidth = 640;
        targetHeight = 640;

        // Log model metadata for debugging
        logModelMetadata();

        // Try to load class names from metadata first
        if (classNames == null && !classNamesFromMetadata) {
            String[] metadataClassNames = readClassNamesFromModel();
            if (metadataClassNames != null && metadataClassNames.length > 0) {
                this.classNames = metadataClassNames;
                this.classNamesFromMetadata = true;
                System.out.println("Loaded " + metadataClassNames.length + " class names from model metadata");
                System.out.println("Class names: " + java.util.Arrays.toString(metadataClassNames));
            }
        }

        // Default class names if not provided - will be determined from model output
        if (classNames == null) {
            classNames = generateDefaultClassNames(10); // Will be updated based on actual model output
        }
    }

    @Override
    protected void calculateOptimalDimensions(int originalWidth, int originalHeight) {
        // Override to use fixed dimensions instead of maintaining aspect ratio
        // Most YOLO models expect square input (640x640)
        targetWidth = 640;
        targetHeight = 640;

        System.out.println("Using fixed target dimensions: " + targetWidth + "x" + targetHeight);
    }

    @Override
    protected String[] getClassNames() {
        return classNames != null ? classNames : generateDefaultClassNames(80);
    }

    /**
     * Set custom class names for this detector
     */
    public void setClassNames(String[] classNames) {
        this.classNames = classNames;
    }

    /**
     * Generate default class names when not provided
     */
    private String[] generateDefaultClassNames(int numClasses) {
        String[] defaultNames = new String[numClasses];
        for (int i = 0; i < numClasses; i++) {
            defaultNames[i] = "class_" + i;
        }
        return defaultNames;
    }

    /**
     * Set class names from comma-separated string
     */
    public void setClassNamesFromString(String classNamesStr) {
        if (classNamesStr != null && !classNamesStr.trim().isEmpty()) {
            this.classNames = classNamesStr.split(",");
            // Trim whitespace
            for (int i = 0; i < this.classNames.length; i++) {
                this.classNames[i] = this.classNames[i].trim();
            }
        }
    }

    @Override
    protected String[] updateClassNames(int numClasses) {
        // Update class names based on detected number of classes
        System.out.println("Auto-updating class names for " + numClasses + " classes");

        // If we already have class names from metadata and they match the number of
        // classes, keep them
        if (classNamesFromMetadata && this.classNames != null && this.classNames.length == numClasses) {
            System.out.println("Keeping existing class names from model metadata");
            return this.classNames;
        }

        // First try to read class names from model metadata
        String[] modelClassNames = readClassNamesFromModel();
        if (modelClassNames != null && modelClassNames.length == numClasses) {
            System.out.println("Using class names from model metadata");
            this.classNames = modelClassNames;
            this.classNamesFromMetadata = true;
            return this.classNames;
        }

        // If no model metadata or custom class names provided, generate meaningful
        // defaults
        if (this.classNames == null || this.classNames.length != numClasses) {
            this.classNames = generateMeaningfulClassNames(numClasses);
            this.classNamesFromMetadata = false;
        }

        return this.classNames;
    }

    /**
     * Try to read class names from ONNX model metadata
     */
    private String[] readClassNamesFromModel() {
        try {
            if (session == null) {
                System.out.println("Session not initialized - cannot read metadata");
                return null;
            }

            // Get model metadata
            var metadata = session.getMetadata();
            System.out.println("Model metadata - Producer: " + metadata.getProducerName());
            System.out.println("Model metadata - Description: " + metadata.getDescription());

            // Check custom metadata for class names
            var customMetadata = metadata.getCustomMetadata();
            System.out.println("Custom metadata keys: " + customMetadata.keySet());

            // Try common keys for class names
            String[] possibleKeys = { "names", "class_names", "classes", "labels" };
            for (String key : possibleKeys) {
                var classNamesValue = metadata.getCustomMetadataValue(key);
                if (classNamesValue.isPresent()) {
                    String classNamesStr = classNamesValue.get();
                    System.out.println("Found class names in metadata[" + key + "]: " + classNamesStr);

                    // Parse class names - could be JSON array, comma-separated, etc.
                    String[] parsedNames = parseClassNamesString(classNamesStr);
                    if (parsedNames != null && parsedNames.length > 0) {
                        return parsedNames;
                    }
                }
            }

            System.out.println("No class names found in model metadata");
            return null;

        } catch (OrtException e) {
            System.err.println("ONNX Runtime error reading model metadata: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Error reading model metadata: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse class names string from metadata (supports various formats)
     */
    private String[] parseClassNamesString(String classNamesStr) {
        if (classNamesStr == null || classNamesStr.trim().isEmpty()) {
            return null;
        }

        String trimmed = classNamesStr.trim();

        // Try JSON object format: {0: 'name1', 1: 'name2', ...}
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                // Parse JSON object format manually
                String content = trimmed.substring(1, trimmed.length() - 1);
                String[] pairs = content.split(",");

                // Find the maximum index to determine array size
                int maxIndex = -1;
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":");
                    if (keyValue.length == 2) {
                        try {
                            int index = Integer.parseInt(keyValue[0].trim());
                            maxIndex = Math.max(maxIndex, index);
                        } catch (NumberFormatException e) {
                            // Skip non-numeric keys
                        }
                    }
                }

                if (maxIndex >= 0) {
                    String[] result = new String[maxIndex + 1];

                    // Parse each key-value pair
                    for (String pair : pairs) {
                        String[] keyValue = pair.split(":", 2);
                        if (keyValue.length == 2) {
                            try {
                                int index = Integer.parseInt(keyValue[0].trim());
                                String value = keyValue[1].trim().replaceAll("['\"]", "");
                                if (index >= 0 && index < result.length) {
                                    result[index] = value;
                                }
                            } catch (NumberFormatException e) {
                                // Skip non-numeric keys
                            }
                        }
                    }

                    // Fill any null entries with default names
                    for (int i = 0; i < result.length; i++) {
                        if (result[i] == null) {
                            result[i] = "class_" + i;
                        }
                    }

                    return result;
                }
            } catch (Exception e) {
                System.err.println("Failed to parse JSON object: " + e.getMessage());
            }
        }

        // Try JSON array format: ["class1", "class2", ...]
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                // Simple JSON array parsing (without full JSON library)
                String content = trimmed.substring(1, trimmed.length() - 1);
                String[] parts = content.split(",");
                String[] result = new String[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    result[i] = parts[i].trim().replaceAll("\"", "");
                }
                return result;
            } catch (Exception e) {
                System.err.println("Failed to parse JSON array: " + e.getMessage());
            }
        }

        // Try comma-separated format: class1,class2,class3
        if (trimmed.contains(",")) {
            String[] parts = trimmed.split(",");
            for (int i = 0; i < parts.length; i++) {
                parts[i] = parts[i].trim();
            }
            return parts;
        }

        // Single class name
        return new String[] { trimmed };
    }

    /**
     * Generate meaningful class names based on number of classes and context
     */
    private String[] generateMeaningfulClassNames(int numClasses) {
        // For common YOLO models, use known class patterns
        if (numClasses == 80) {
            // COCO dataset classes
            return new String[] {
                    "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
                    "traffic light",
                    "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep",
                    "cow",
                    "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase",
                    "frisbee",
                    "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard",
                    "surfboard", "tennis racket", "bottle",
                    "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich", "orange",
                    "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch", "potted plant", "bed",
                    "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave",
                    "oven",
                    "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier",
                    "toothbrush"
            };
        } else if (numClasses == 1) {
            // Single class detection (could be CCCD, face, etc.)
            return new String[] { "object" };
        } else if (numClasses == 2) {
            // Binary classification (could be person/no-person, card/no-card, etc.)
            return new String[] { "background", "target" };
        } else if (numClasses <= 10) {
            // Small number of classes, use descriptive names
            String[] names = new String[numClasses];
            for (int i = 0; i < numClasses; i++) {
                names[i] = "object_" + (i + 1);
            }
            return names;
        }

        // Default fallback for any number of classes
        return generateDefaultClassNames(numClasses);
    }

    /**
     * Log model metadata for debugging purposes
     */
    private void logModelMetadata() {
        try {
            if (session == null) {
                System.out.println("Session not available for metadata logging");
                return;
            }

            var metadata = session.getMetadata();
            System.out.println("=== Model Metadata ===");
            System.out.println("Producer: " + metadata.getProducerName());
            System.out.println("Graph Name: " + metadata.getGraphName());
            System.out.println("Description: " + metadata.getDescription());
            System.out.println("Domain: " + metadata.getDomain());
            System.out.println("Version: " + metadata.getVersion());

            var customMetadata = metadata.getCustomMetadata();
            if (!customMetadata.isEmpty()) {
                System.out.println("Custom metadata:");
                for (var entry : customMetadata.entrySet()) {
                    System.out.println("  " + entry.getKey() + ": " + entry.getValue());
                }
            } else {
                System.out.println("No custom metadata found");
            }
            System.out.println("======================");

        } catch (Exception e) {
            System.err.println("Error logging model metadata: " + e.getMessage());
        }
    }
}