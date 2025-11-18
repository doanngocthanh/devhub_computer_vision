# 📋 Đề xuất cấu trúc & logic cho dự án DevHub OCR

## 🎯 Mục tiêu bổ sung
- Tự động viết code module MVC mới
- Tự debug và test
- Tự động push code sau khi pass test
- Sử dụng Gemini API cho việc code generation

---

## 🏗️ Cấu trúc dự án đã được tối ưu

```
devhubocr/
├── src/main/java/com/devhub/ocr/
│   ├── AA/A0/AAA0_0100/          # Module chức năng
│   │   ├── trx/                  # Controllers
│   │   │   └── AAA0_0100.java   # Controller chính
│   │   ├── mod/                  # Business Logic
│   │   │   └── AAA0_0100Mod.java
│   │   └── batch/                # Scheduled Jobs
│   │       └── AAA0_0100Batch.java
│   │
│   ├── core/                     # Core system modules
│   │   ├── codegen/              # Code generation module
│   │   │   ├── trx/
│   │   │   │   └── CodeGenController.java
│   │   │   └── mod/
│   │   │       ├── GeminiService.java
│   │   │       ├── CodeGeneratorMod.java
│   │   │       ├── TestRunnerMod.java
│   │   │       └── GitPushMod.java
│   │   │
│   │   └── layout/               # Layout & routing system
│   │       ├── trx/
│   │       │   └── BaseController.java
│   │       └── mod/
│   │           └── LayoutService.java
│   │
│   └── config/
│       ├── WebMvcConfig.java     # Spring MVC config
│       └── ThymeleafConfig.java  # Template config
│
├── src/main/resources/
│   ├── templates/
│   │   ├── layouts/
│   │   │   └── default.html      # Layout chính (wrapper)
│   │   │
│   │   └── html/                 # Fragments/Pages
│   │       └── AA/A0/AAA0_0100/
│   │           └── AAA0_0100.html  # Content fragment
│   │
│   ├── static/
│   │   ├── css/
│   │   ├── js/
│   │   └── dist/                 # Tabler assets
│   │
│   └── application.yml
│
└── pom.xml
```

---

## 🔧 Giải pháp Layout + Fragment (Thymeleaf)

### 1. **Layout chính** (`layouts/default.html`)
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title th:text="${pageTitle} ?: 'DevHub OCR'">DevHub OCR</title>
    
    <!-- Tabler CSS -->
    <link href="/dist/css/tabler.min.css" rel="stylesheet"/>
    <link href="/dist/css/tabler-vendors.min.css" rel="stylesheet"/>
</head>
<body>
    <div class="page">
        <!-- Sidebar -->
        <aside class="navbar navbar-vertical navbar-expand-lg">
            <div class="container-fluid">
                <h1 class="navbar-brand">DevHub OCR</h1>
                <!-- Navigation menu -->
                <ul class="navbar-nav">
                    <li class="nav-item">
                        <a class="nav-link" href="/AA/A0/AAA0_0100">
                            Module AAA0_0100
                        </a>
                    </li>
                </ul>
            </div>
        </aside>
        
        <!-- Main content area -->
        <div class="page-wrapper">
            <div class="page-header">
                <h2 class="page-title" th:text="${pageTitle}">Page Title</h2>
            </div>
            
            <div class="page-body">
                <div class="container-xl">
                    <!-- Nơi chèn fragment từ controller -->
                    <div layout:fragment="content">
                        <!-- Content sẽ được inject vào đây -->
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <!-- Tabler JS -->
    <script src="/dist/js/tabler.min.js"></script>
</body>
</html>
```

### 2. **Content Fragment** (`html/AA/A0/AAA0_0100/AAA0_0100.html`)
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/default}">

<div layout:fragment="content">
    <div class="card">
        <div class="card-header">
            <h3 class="card-title">Module AAA0_0100</h3>
        </div>
        <div class="card-body">
            <p th:text="${message}">Module content here</p>
            
            <!-- Form example -->
            <form th:action="@{/AA/A0/AAA0_0100/submit}" method="post">
                <div class="mb-3">
                    <label class="form-label">Input</label>
                    <input type="text" class="form-control" name="input"/>
                </div>
                <button type="submit" class="btn btn-primary">Submit</button>
            </form>
        </div>
    </div>
</div>

</html>
```

### 3. **Controller trả về view**
```java
package com.devhub.ocr.AA.A0.AAA0_0100.trx;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.devhub.ocr.AA.A0.AAA0_0100.mod.AAA0_0100Mod;

@Controller
@RequestMapping("/AA/A0/AAA0_0100")
public class AAA0_0100 {
    
    private final AAA0_0100Mod mod;
    
    public AAA0_0100(AAA0_0100Mod mod) {
        this.mod = mod;
    }
    
    @GetMapping
    public String index(Model model) {
        model.addAttribute("pageTitle", "Module AAA0_0100");
        model.addAttribute("message", mod.getData());
        
        // Trả về path tương đối đến fragment
        return "html/AA/A0/AAA0_0100/AAA0_0100";
    }
    
    @PostMapping("/submit")
    public String submit(@RequestParam String input, Model model) {
        String result = mod.processData(input);
        model.addAttribute("pageTitle", "Result");
        model.addAttribute("message", result);
        return "html/AA/A0/AAA0_0100/AAA0_0100";
    }
}
```

---

## 🤖 Module tự động Code Generation với Gemini

### **GeminiService.java** - Gọi Gemini API
```java
package com.devhub.ocr.core.codegen.mod;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

@Service
public class GeminiService {
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    @Value("${gemini.api.url}")
    private String apiUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Gọi Gemini API để sinh code
     */
    public String generateCode(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        String requestBody = String.format("""
            {
                "contents": [{
                    "parts": [{"text": "%s"}]
                }],
                "generationConfig": {
                    "temperature": 0.2,
                    "maxOutputTokens": 8000
                }
            }
            """, prompt.replace("\"", "\\\""));
        
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(
            apiUrl, 
            HttpMethod.POST, 
            entity, 
            String.class
        );
        
        return extractCodeFromResponse(response.getBody());
    }
    
    private String extractCodeFromResponse(String response) {
        // Parse JSON response và lấy code
        // Implementation tùy theo format response của Gemini
        return response; // Placeholder
    }
}
```

### **CodeGeneratorMod.java** - Logic sinh code module
```java
package com.devhub.ocr.core.codegen.mod;

import org.springframework.stereotype.Service;
import java.nio.file.*;
import java.io.IOException;

@Service
public class CodeGeneratorMod {
    
    private final GeminiService geminiService;
    private final String basePackage = "com.devhub.ocr";
    private final String basePath = "src/main/java/com/devhub/ocr";
    
    public CodeGeneratorMod(GeminiService geminiService) {
        this.geminiService = geminiService;
    }
    
    /**
     * Sinh module MVC hoàn chỉnh
     */
    public void generateModule(String moduleCode, String description) throws IOException {
        // Ví dụ: moduleCode = "AAA0_0200"
        String[] parts = parseModuleCode(moduleCode);
        String path = String.join("/", parts);
        
        // 1. Sinh Controller
        String controllerPrompt = buildControllerPrompt(moduleCode, description);
        String controllerCode = geminiService.generateCode(controllerPrompt);
        writeFile(basePath + "/" + path + "/trx/" + moduleCode + ".java", controllerCode);
        
        // 2. Sinh Mod (Business Logic)
        String modPrompt = buildModPrompt(moduleCode, description);
        String modCode = geminiService.generateCode(modPrompt);
        writeFile(basePath + "/" + path + "/mod/" + moduleCode + "Mod.java", modCode);
        
        // 3. Sinh HTML Fragment
        String htmlPrompt = buildHtmlPrompt(moduleCode, description);
        String htmlCode = geminiService.generateCode(htmlPrompt);
        writeFile("src/main/resources/templates/html/" + path + "/" + moduleCode + ".html", htmlCode);
        
        // 4. Sinh Batch (optional)
        if (description.contains("schedule") || description.contains("cron")) {
            String batchPrompt = buildBatchPrompt(moduleCode, description);
            String batchCode = geminiService.generateCode(batchPrompt);
            writeFile(basePath + "/" + path + "/batch/" + moduleCode + "Batch.java", batchCode);
        }
    }
    
    private String buildControllerPrompt(String moduleCode, String description) {
        return String.format("""
            Generate a Spring MVC Controller with these requirements:
            - Package: %s
            - Class name: %s
            - Mapping: /%s
            - Description: %s
            - Follow DevHub OCR conventions:
              * Controller only handles HTTP requests
              * Delegate logic to Mod layer
              * Return Thymeleaf template path
              * Use Model to pass data to view
            - Use Tabler.io design system
            - Return complete, compilable Java code only
            """, 
            basePackage, moduleCode, moduleCode.replace("_", "/"), description);
    }
    
    private String buildModPrompt(String moduleCode, String description) {
        return String.format("""
            Generate a Service class (Mod) with these requirements:
            - Package: %s.mod
            - Class name: %sMod
            - Description: %s
            - Contains all business logic
            - Annotated with @Service
            - Return complete, compilable Java code only
            """, 
            basePackage, moduleCode, description);
    }
    
    private String buildHtmlPrompt(String moduleCode, String description) {
        return String.format("""
            Generate a Thymeleaf HTML fragment with these requirements:
            - Use layout:decorate="~{layouts/default}"
            - Content inside layout:fragment="content"
            - Use Tabler.io components (cards, forms, tables)
            - Description: %s
            - Module code: %s
            - Return complete HTML only
            """, 
            description, moduleCode);
    }
    
    private String[] parseModuleCode(String code) {
        // AAA0_0200 -> [AA, A0, AAA0_0200]
        return new String[]{
            code.substring(0, 2),
            code.substring(2, 4),
            code
        };
    }
    
    private void writeFile(String path, String content) throws IOException {
        Path filePath = Paths.get(path);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content);
    }
}
```

### **TestRunnerMod.java** - Tự động test
```java
package com.devhub.ocr.core.codegen.mod;

import org.springframework.stereotype.Service;
import java.io.*;

@Service
public class TestRunnerMod {
    
    /**
     * Compile và chạy test cho module mới
     */
    public boolean runTests(String moduleCode) {
        try {
            // 1. Compile code
            Process compileProcess = Runtime.getRuntime().exec(
                "mvn compile -DskipTests=false"
            );
            int compileResult = compileProcess.waitFor();
            
            if (compileResult != 0) {
                return false;
            }
            
            // 2. Run tests
            Process testProcess = Runtime.getRuntime().exec(
                "mvn test -Dtest=" + moduleCode + "Test"
            );
            int testResult = testProcess.waitFor();
            
            return testResult == 0;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Debug với Gemini nếu test fail
     */
    public String debugWithGemini(String errorLog, String sourceCode) {
        GeminiService gemini = new GeminiService();
        String prompt = String.format("""
            This code has errors:
            
            ```java
            %s
            ```
            
            Error log:
            ```
            %s
            ```
            
            Fix the code and return only the corrected version.
            """, sourceCode, errorLog);
        
        return gemini.generateCode(prompt);
    }
}
```

### **GitPushMod.java** - Tự động push code
```java
package com.devhub.ocr.core.codegen.mod;

import org.springframework.stereotype.Service;
import java.io.*;

@Service
public class GitPushMod {
    
    public boolean commitAndPush(String moduleCode, String message) {
        try {
            // Git add
            exec("git add .");
            
            // Git commit
            String commitMsg = String.format("[AUTO] %s - %s", moduleCode, message);
            exec("git commit -m \"" + commitMsg + "\"");
            
            // Git push
            exec("git push origin main");
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private void exec(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Git command failed: " + command);
        }
    }
}
```

---

## 📅 Scheduled Job - Chạy hàng ngày

```java
package com.devhub.ocr.core.codegen.batch;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.devhub.ocr.core.codegen.mod.*;

@Component
public class CodeGenBatch {
    
    private final CodeGeneratorMod codeGen;
    private final TestRunnerMod testRunner;
    private final GitPushMod gitPush;
    
    public CodeGenBatch(CodeGeneratorMod codeGen, 
                        TestRunnerMod testRunner, 
                        GitPushMod gitPush) {
        this.codeGen = codeGen;
        this.testRunner = testRunner;
        this.gitPush = gitPush;
    }
    
    @Scheduled(cron = "0 0 2 * * *") // 2 AM mỗi ngày
    public void autoGenerateAndDeploy() {
        try {
            // 1. Đọc task từ queue/database
            String moduleCode = "AAA0_0300"; // Example
            String description = "User management module";
            
            // 2. Generate code
            codeGen.generateModule(moduleCode, description);
            
            // 3. Run tests
            boolean testsPassed = testRunner.runTests(moduleCode);
            
            if (!testsPassed) {
                // Debug với Gemini
                String fixedCode = testRunner.debugWithGemini("error log", "source");
                // Retry...
            }
            
            // 4. Push code
            if (testsPassed) {
                gitPush.commitAndPush(moduleCode, description);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

---

## ⚙️ Configuration

### **application.yml**
```yaml
spring:
  thymeleaf:
    prefix: classpath:/templates/
    suffix: .html
    mode: HTML
    cache: false
    
gemini:
  api:
    key: ${GEMINI_API_KEY}
    url: https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent
    
git:
  auto-push: true
  branch: main
```

### **pom.xml** - Dependencies
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
<dependency>
    <groupId>nz.net.ultraq.thymeleaf</groupId>
    <artifactId>thymeleaf-layout-dialect</artifactId>
</dependency>
```

---

## 🎯 Tóm tắt giải pháp

| Vấn đề | Giải pháp |
|--------|-----------|
| Layout chung | Dùng Thymeleaf Layout Dialect với `layout:decorate` |

| Code generation | Module `core/codegen` với Gemini API |
| Auto debug | `TestRunnerMod` chạy test, gọi Gemini fix lỗi nếu fail |
| Auto push | `GitPushMod` commit và push sau khi pass test |
| Schedule | `@Scheduled` batch chạy mỗi ngày |

Bạn có muốn tôi tạo artifact code mẫu hoàn chỉnh cho từng phần không?