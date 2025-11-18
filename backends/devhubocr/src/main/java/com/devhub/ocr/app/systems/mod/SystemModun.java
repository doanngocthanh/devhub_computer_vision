
package com.devhub.ocr.app.systems.mod;

import org.reflections.Reflections;
import java.util.Set;
import java.util.stream.Collectors;

public class SystemModun {
    // Lấy động các package con của com.devhub.ocr
    public static java.util.List<String> getUppercasePackages() {
        Reflections reflections = new Reflections("com.devhub.ocr");
        Set<Class<?>> allClasses = reflections.getSubTypesOf(Object.class);
        Set<String> packages = allClasses.stream()
                .map(cls -> cls.getPackage() != null ? cls.getPackage().getName() : "")
                .filter(pkg -> pkg.startsWith("com.devhub.ocr"))
                .collect(Collectors.toSet());
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String pkg : packages) {
            result.add(pkg.toUpperCase());
        }
        return result;
    }

    public static void main(String[] args) {
        Reflections reflections = new Reflections("com.devhub.ocr");
        Set<Class<?>> allClasses = reflections.getTypesAnnotatedWith(javax.annotation.processing.Generated.class, true);
        if (allClasses.isEmpty()) {
            allClasses = reflections.getSubTypesOf(Object.class);
        }

        Set<String> packages = allClasses.stream()
                .map(cls -> cls.getPackage() != null ? cls.getPackage().getName() : "")
                .filter(pkg -> pkg.startsWith("com.devhub.ocr"))
                .collect(Collectors.toSet());

        System.out.println("Packages (Uppercase):");
        for (String pkg : packages) {
            System.out.println("  " + pkg.toUpperCase());
        }

        System.out.println("\nClasses found:");
        for (Class<?> cls : allClasses) {
            String pkgName = (cls.getPackage() != null) ? cls.getPackage().getName() : "(no package)";
            System.out.println("  " + cls.getName() + " (package: " + pkgName + ")");
        }

        System.out.println("\nTotal packages: " + packages.size());
        System.out.println("Total classes: " + allClasses.size());
    }
}
