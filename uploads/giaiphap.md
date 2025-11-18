✅ CẤU TRÚC GIẢI PHÁP
Java backend
    ↓ (start subprocess)
Python OCR Worker (chạy suốt – load model 1 lần)
    ↓↑ stdin/stdout truyền JSON


Thời gian cho từng ảnh OCR ~100–300ms tùy model (không reload model nữa).

🧩 1. Python worker (giữ sống liên tục)
ocr_worker.py

Load mô hình VietOCR & PaddleOCR 1 lần.

Chờ request JSON gửi từ Java.

Trả về JSON.

import sys
import json
from vietocr.tool.config import Cfg
from vietocr.tool.predictor import Predictor
from paddleocr import PaddleOCR
from PIL import Image
import base64
import io

# --- Load model once ---
print("Loading models...", file=sys.stderr)

config = Cfg.load_config_from_name('vgg_transformer')
vietocr_model = Predictor(config)
paddle_model = PaddleOCR(lang='vi')

print("Models loaded", file=sys.stderr)
sys.stderr.flush()

# --- Helper ---
def decode_image(b64):
    img_bytes = base64.b64decode(b64)
    return Image.open(io.BytesIO(img_bytes))

# --- Event loop ---
while True:
    line = sys.stdin.readline()
    if not line:
        continue

    try:
        req = json.loads(line)
        task = req.get("task")
        img_b64 = req.get("image")

        img = decode_image(img_b64)

        if task == "vietocr":
            text = vietocr_model.predict(img)
            resp = {"status": "ok", "result": text}

        elif task == "paddle":
            res = paddle_model.ocr(image=np.array(img))
            text = " ".join([line[1][0] for line in res[0]])
            resp = {"status": "ok", "result": text}

        else:
            resp = {"status": "error", "message": "Unknown task"}

    except Exception as e:
        resp = {"status": "error", "message": str(e)}

    print(json.dumps(resp))
    sys.stdout.flush()


✔ Nhẹ
✔ Không reload model
✔ Giao tiếp JSON
✔ Nhận ảnh dạng Base64 (không cần lưu file)

🧩 2. Java side: khởi động Python worker 1 lần
OCRProcess.java
import java.io.*;
import java.util.Base64;

public class OCRProcess {
    private Process process;
    private BufferedWriter writer;
    private BufferedReader reader;

    public OCRProcess() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("python", "ocr_worker.py");
        pb.redirectErrorStream(true);
        process = pb.start();

        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    }

    public String sendOCR(String task, byte[] imageBytes) throws IOException {
        String b64 = Base64.getEncoder().encodeToString(imageBytes);

        String json = String.format(
                "{\"task\":\"%s\", \"image\":\"%s\"}",
                task, b64
        );

        writer.write(json + "\n");
        writer.flush();

        String response = reader.readLine();
        return response;
    }

    public void close() throws IOException {
        writer.close();
        reader.close();
        process.destroy();
    }
}

🧩 3. Cách dùng trong Java API
OCRProcess ocr = new OCRProcess();

byte[] bytes = Files.readAllBytes(Paths.get("test.jpg"));

String resp1 = ocr.sendOCR("vietocr", bytes);
String resp2 = ocr.sendOCR("paddle", bytes);

System.out.println(resp1);
System.out.println(resp2);

⚡ Tối ưu và mở rộng

Bạn có thể:

✔ 1. Giữ Python worker như Singleton
public class OCRManager {
    private static OCRProcess INSTANCE;

    public static synchronized OCRProcess get() throws IOException {
        if (INSTANCE == null) {
            INSTANCE = new OCRProcess();
        }
        return INSTANCE;
    }
}

✔ 2. Có thể tăng tốc hơn nữa

Dùng ZeroMQ thay stdin/stdout → tốc độ tăng ~20–40%.

Dùng shared memory / pipe binary nếu chỉ OCR cực nặng.

Batch xử lý nhiều ảnh trong 1 request.