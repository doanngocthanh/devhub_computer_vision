
# ✅ 1. Mục tiêu của trang **WLA0_0100 – Pipeline Builder**

Trang này cho phép:

### ✔ Chọn pipeline

* Hiển thị danh sách pipeline từ DB
* Cho phép tạo mới / chỉnh sửa pipeline

### ✔ Xây dựng pipeline bằng UI (kiểu n8n)

* Kéo thả các *Pipeline Step* (Java class)
* Mỗi step có input/output types
* UI cho phép mapping input ←→ output
* Validate step-by-step

### ✔ Lưu pipeline thành JSON workflow

* Ví dụ:

```json
{
  "pipelineId": 1,
  "name": "OCR + AutoLabel",
  "steps": [
    {
      "id": "step1",
      "bean": "LoadImageStep",
      "input": { "path": "${file.uploadPath}" },
      "outputKey": "img"
    },
    {
      "id": "step2",
      "bean": "OnnxDetectStep",
      "input": { "image": "${img}" },
      "outputKey": "bbox"
    },
    {
      "id": "step3",
      "bean": "AutoLabelStep",
      "input": { "image": "${img}", "bbox": "${bbox}" }
    }
  ]
}
```

---

# ✅ 2. Kiến trúc tổng thể Pipeline trong Java

Bạn triển khai như sau:

---

## **2.1. Interface chung của một Pipeline Step**

`src/main/java/com/devhub/ocr/pipeline/PipelineStep.java`

```java
public interface PipelineStep {
    String getName(); // ví dụ: "LoadImage"
    List<PipelineParam> getInputParams();
    List<PipelineParam> getOutputParams();

    PipelineResult execute(Map<String, Object> input) throws Exception;
}
```

---

## **2.2. Kiểu dữ liệu PipelineParam**

```java
public class PipelineParam {
    private String name;          // "image"
    private String type;          // "file", "string", "int", "json", "buffer"
    private boolean required;
}
```

---

## **2.3. Kiểu dữ liệu PipelineResult**

```java
public class PipelineResult {
    private Map<String, Object> output;  // output của step
}
```

---

# ✅ 3. Cách xử lý input / output “file”

### Nếu input là `file`:

Trên UI, bạn sẽ upload file → lưu vào `/uploads/tmp/...` → truyền vào pipeline step dưới dạng:

```json
{
  "file": "/uploads/tmp/123.png"
}
```

Java đọc file trực tiếp theo path.

---

### Nếu output là `file`:

Step trả về:

```java
result.put("file", new File("/path/to/output.jpg"));
```

Controller chạy pipeline sẽ:

* copy sang `/uploads/pipeline_outputs/...`
* trả về URL an toàn `/files/pipeline/...`

---

# ✅ 4. Tạo module WLA0_0100 theo chuẩn cấu trúc A0

**Bạn làm đúng kiến trúc QA module của dự án**:

```
src/main/java/com/devhub/ocr/QA/A0/WLA0_0100/
│
├── trx/      ← Controller Layer
│   ├── WLA0_0100Controller.java
│
├── mod/      ← Business logic
│   ├── WLA0_0100Mod.java
│
└── dto/      ← DTO cho JSON pipeline
    ├── PipelineDTO.java
    ├── PipelineStepDTO.java
    ├── PipelineConnectionDTO.java
```

UI Template:

```
src/main/resources/templates/html/A0/WLA0_0100/index.html
```

Menu:

* thêm vào DB (bảng menus) → path `/A0/WLA0_0100`

---

# ✅ 5. Gợi ý các file cụ thể bạn cần tạo

---

## **5.1. DTO pipeline**

`PipelineDTO.java`

```java
@Data
public class PipelineDTO {
    private Long id;
    private String name;
    private List<PipelineStepDTO> steps;
}
```

`PipelineStepDTO.java`

```java
@Data
public class PipelineStepDTO {
    private String id;
    private String bean;
    private Map<String, Object> input;
    private String outputKey;
}
```

---

## **5.2. Controller**

`WLA0_0100Controller.java`

```java
@Controller
@RequestMapping("/A0/WLA0_0100")
public class WLA0_0100Controller {

    @Autowired WLA0_0100Mod mod;

    @GetMapping("")
    public String index(Model model) {
        model.addAttribute("pipelines", mod.listPipelines());
        return "html/A0/WLA0_0100/index";
    }

    @ResponseBody
    @PostMapping("/save")
    public Map<String, Object> save(@RequestBody PipelineDTO dto) {
        return mod.savePipeline(dto);
    }

    @ResponseBody
    @PostMapping("/run")
    public Map<String, Object> run(@RequestBody PipelineDTO dto) {
        return mod.runPipeline(dto);
    }
}
```

---

## **5.3. Business logic**

`WLA0_0100Mod.java`

```java
@Service
public class WLA0_0100Mod {

    @Autowired
    private PipelineRegistry registry;

    public List<Map<String, Object>> listPipelines() {
        // load from SQLite
    }

    public Map<String,Object> savePipeline(PipelineDTO dto) {
        // insert/update JSON workflow
    }

    public Map<String, Object> runPipeline(PipelineDTO dto) {
        Map<String, Object> context = new HashMap<>();

        for (PipelineStepDTO s : dto.getSteps()) {

            PipelineStep step = registry.getStep(s.getBean());
            Map<String, Object> input = resolveInput(s.getInput(), context);

            PipelineResult result = step.execute(input);

            if (s.getOutputKey() != null) {
                context.put(s.getOutputKey(), result.getOutput());
            }
        }
        return context;
    }
}
```

---

## **5.4. Đăng ký step**

`PipelineRegistry.java`

```java
@Component
public class PipelineRegistry {
    @Autowired List<PipelineStep> steps;

    private Map<String, PipelineStep> map;

    @PostConstruct
    public void init() {
        map = new HashMap<>();
        for (PipelineStep s : steps) map.put(s.getClass().getSimpleName(), s);
    }

    public PipelineStep getStep(String name) {
        return map.get(name);
    }

    public Collection<PipelineStep> list() {
        return map.values();
    }
}
```

---

# ✅ 6. Template Thymeleaf UI (drag-drop pipeline)

`templates/html/A0/WLA0_0100/index.html`

Gợi ý (skeleton):

```html
<div class="pipeline-builder">
  <div id="step-list"></div>
  <div id="canvas"></div>
</div>

<script src="/assets/pipeline-builder.js"></script>
```

Bạn viết JS giống n8n:

* Kéo thả step
* Set input/output mapping
* Validate type
* Export JSON → gọi `/A0/WLA0_0100/save`

---

# ✅ 7. SQLite migrations

Tạo file:

`db_local/sql/20251121_create_pipeline_tables.sql`

```sql
CREATE TABLE pipelines (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    workflow_json TEXT NOT NULL,
    created_at TEXT,
    updated_at TEXT
);
```

---

# ✅ 8. Danh sách step mẫu bạn có thể tạo ngay

```
LoadImageStep
OnnxDetectStep
ResizeStep
OtsuThresholdStep
AutoLabelStep
SaveFileStep
```

---

# 👉 Tôi có thể viết trọn bộ code mẫu 100% đầy đủ cho bạn

Chỉ cần bạn trả lời 2 câu:

### **1. Pipeline Builder muốn UI giống n8n hay giống Node-RED?**

* n8n → node tròn, nhiều port
* Node-RED → hình chữ nhật, line connect đơn giản

### **2. Bạn muốn tôi tạo sẵn 3 step mẫu nào?**

Ví dụ:

* LoadImageStep
* OnnxInferenceStep
* AutoLabelStep

---
