./mvnw spring-boot:run

## Chạy Playwright (Java) như chương trình

Tôi đã thêm một lớp tiện ích `PlaywrightRunner` để chạy Playwright như một chương trình Java (không phải test). Cách dùng:

- Biên dịch module:

```
cd backends/devhubocr
./mvnw -DskipTests package
```

- Chạy trực tiếp bằng Maven exec (đã cấu hình `exec-maven-plugin` trong `pom.xml`):

```
./mvnw exec:java -Dexec.mainClass=com.devhub.ocr.playwright.PlaywrightRunner -Dexec.args="https://example.com playwright-screenshot.png"
```

Tham số đầu tiên là URL (mặc định `https://example.com`), tham số thứ hai là đường dẫn file ảnh đầu ra (mặc định `playwright-screenshot.png`).

Lưu ý quan trọng về browser binaries:

- Playwright cần các trình duyệt (Chromium/Firefox/WebKit) được cài đặt. Thông thường, khi bạn chạy lần đầu Playwright Java sẽ tải các binary này tự động nếu môi trường cho phép. Nếu bạn cần cài thủ công, cài Node.js và chạy `npx playwright install` trong môi trường có Internet, hoặc tham khảo tài liệu Playwright để cài trên CI/servers không có Internet.

Nếu build gặp lỗi do thiếu native browser binaries, hãy làm theo hướng dẫn trên hoặc chạy `npx playwright install` trên máy dev trước.
