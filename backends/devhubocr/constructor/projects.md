dự án dùng templates html, https://tabler.io/admin-template
Phiên bản cập nhật (phù hợp với trạng thái hiện tại của repo)

Mục tiêu
- Mô tả cấu trúc thư mục hiện tại của `backends/devhubocr`.
- Ghi rõ cách templates/layout đang dùng (full-page + header/footer/sidebar fragments).
- Ghi nhận prototype module `core/codegen` (Gemini mock + generator) và vị trí output `generated/`.

1) Tổng quan cấu trúc (tại `backends/devhubocr`)

Project chính: `backends/devhubocr` (Spring Boot + Thymeleaf).

Chỗ đặt code:
- Java: `src/main/java/com/devhub/ocr`
- Templates: `src/main/resources/templates`
- Static assets: `src/main/resources/static` (css/js/icons)

Mô tả module theo quy ước hiện tại:

src/main/java/com/devhub/ocr/
├── AA/A0/AAA0_0100/
│   ├── trx/     (controllers)
│   ├── mod/     (business logic)
│   └── batch/   (scheduled jobs)

Template tương ứng (ví dụ):
`src/main/resources/templates/html/AA/A0/AAA0_0100/AAA0_0100.html`

2) Layout & templates hiện dùng
- Hiện repo dùng "full-page" templates (mỗi trang là một file HTML hoàn chỉnh) và chèn lại các fragment chung:
	- `templates/common/header.html`
	- `templates/common/footer.html`
	- `templates/common/sidebar.html` (menu)

- Các trang tham chiếu Tabler assets local qua `th:href`/`th:src`.

Ghi chú: Trước đây có thử nghiệm với một layout controller-driven; hiện hướng dẫn thực tế là tiếp tục dùng full pages và chèn fragments (ít rủi ro hơn với Thymeleaf khi không dùng layout dialect tất-cả).

4) Static files và uploads
- Tabler CSS/JS được copy vào `static/css` và `static/js`.
- Thêm `WebMvcConfig` để expose filesystem `uploads/` qua `/uploads/**` (mapping `file:./uploads/`).
- Có `UploadsController` để redirect `/uploads` và `/uploads/` về trang chủ, và endpoint `/uploads/health` để kiểm tra.

5) Error handling
- Thêm `AppErrorController` (minimal) để trả về thông tin lỗi trên `/error` (plain text) giúp debug lỗi 404/NoResourceFound.

6) Các file đã thêm/điều chỉnh quan trọng
- `src/main/java/com/devhub/ocr/APP/systems/config/WebMvcConfig.java`
- `src/main/java/com/devhub/ocr/APP/systems/file/UploadsController.java`
- `src/main/java/com/devhub/ocr/APP/systems/error/AppErrorController.java`
- `src/main/resources/templates/common/sidebar.html`
- `src/main/resources/templates/html/home.html` (đã sửa để include common fragments)


7) Chạy & kiểm tra nhanh
- Build:
	```bash
	cd backends/devhubocr
	./mvnw -DskipTests package
	```
- Run (dev):
	```bash
	./mvnw -DskipTests spring-boot:run
	```

 
10) Quy tắc DB & migrations (mới)

- Vị trí database (file SQLite): `/workspaces/devhub_computer_vision/db_local/database.db`.
- DDL / migration SQL files đặt trong folder: `/workspaces/devhub_computer_vision/db_local/sql`.
	- Mỗi file migration là một script SQL (*.sql). Tên file có thể dùng tiền tố ngày/phiên bản như `20251118__create_tables.sql`.
	- Migrations được áp dụng theo thứ tự tên tệp (lexical order).
- Khi ứng dụng Spring Boot khởi động, hệ thống sẽ tự động chạy migrations từ folder trên (có thể override bằng JVM property `-Ddevhub.db.migrations=/path/to/sql`).
- Migrations đã áp dụng được ghi lại trong bảng `schema_migrations` để tránh áp dụng lại.
- Lưu ý: migration runner hiện tách statements theo dấu chấm phẩy (`;`) — tránh dùng cấu trúc SQL phức tạp chứa `;` bên trong comment hoặc function body nếu không chắc.

11) Role & User management (mới)

- Mô tả: Hệ thống đã thêm tính năng quản lý role và gán role cho user. Các bảng liên quan:
	- `roles(id, role, description)`
	- `users_roles(id, user_id, role)`
	- `path_roles(id, path, role)`

- Chức năng đã triển khai:
	- Tạo / xóa roles (RoleService)
	- Gán / bỏ role cho user (theo email)
	- Gán role cho path (định tuyến theo key như `AA.A0.AAA0_0100`)
	- Interceptor kiểm tra JWT cookie `DEVHUB_AUTH` và so khớp role theo đường dẫn (AuthorizationInterceptor)

- Gợi ý báo cáo: bạn nên thêm migration SQL (ví dụ `/db_local/sql/20251118__create_roles_tables.sql`) để đảm bảo bảng roles/path_roles/users_roles được tạo trong môi trường mới.

12) Yêu cầu mới: Trang quản lý người quản lý & menu

- Yêu cầu của dự án (bổ sung):
	- Thêm trang admin để "Thêm người quản lý người dùng" (user manager). Trang này cho phép liệt kê users và gán role quản trị người dùng (ví dụ role `USER_MANAGER` hoặc `USER_ADMIN`).
	- Thêm trang quản lý menu (menu có nhiều level). Menu lưu trữ: title, icon (tên icon), path (URI), parent_id (nếu là submenu), roles (danh sách role cho phép hiển thị menu này).
	- Sidebar (`templates/common/sidebar.html`) sẽ được đồng bộ từ dữ liệu menu trên DB và hiển thị dropdown theo cấu trúc multi-level.

Nội dung implement đã đề xuất:

1. Backend
	 - `MenuService` (dùng `DatabasePlugin`) để quản lý bảng `menus` và trả về cấu trúc cây (tree) cho template.
	 


3. DB migration
	 - Thêm migration SQL `db_local/sql/20251118__create_menus_table.sql` để tạo bảng `menus`.

Lưu ý: Tôi đã thêm các file code/thực thi mẫu (MenuService/Controller/Advice/templates) vào repo để bạn review; hãy kiểm tra và điều chỉnh role name (`USER_MANAGER`) nếu bạn muốn dùng role khác.

Ví dụ workflow:

1. Thêm file migration: `/workspaces/devhub_computer_vision/db_local/sql/20251118__create_users.sql`
2. Khởi động ứng dụng (hoặc gọi endpoint admin): chương trình sẽ áp dụng migration mới vào `db_local/database.db`.
3. Migrations thành công sẽ được ghi vào `schema_migrations`.

--
Thực hiện các thay đổi này nhằm đảm bảo DB được khởi tạo/ cập nhật tự động khi app chạy, tránh lỗi thiếu bảng/ cấu trúc khi deploy hoặc chạy trong dev.
