package com.devhub.ocr.app.plugins;

public class FileSystems {
    private final static String userDir = System.getProperty("user.dir");
    private final static String uploadDir = userDir + "/uploads/";

    private boolean SaveFiles(Class<?> ServiceCallerm, String fileName, boolean isHash, Byte[] fileData) {
        return true;
    }

    static {
        System.out.println("FileSystems initialized");
        System.out.println(userDir);
        System.out.println(uploadDir);
    }

    public static void main(String[] args) {

    }
}
