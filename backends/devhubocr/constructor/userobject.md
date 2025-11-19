# UserObject — mô tả & cách sử dụng

Tệp này mô tả lớp `UserObject` (DTO) và cách nạp / truy cập người dùng đã xác thực trong ứng dụng.

## Mục đích

- `UserObject` chứa thông tin người dùng đang đăng nhập (id, email, first/last name, roles, createdAt, displayName).
- Mục tiêu: cung cấp một đối tượng dễ dùng cho controller, service và template để truy xuất thông tin user mà không phải decode JWT hoặc query DB nhiều lần.

## Trường chính

- `Long id` — id người dùng trong bảng `users`.
- `String email` — email (được lưu trong JWT subject).
- `String firstName`, `String lastName` — tên người dùng.
- `Set<String> roles` — các role gán cho user (lấy từ `users_roles`).
- `Instant createdAt` — thời điểm tạo tài khoản (nếu có).
- `String displayName` — tên hiển thị (tự tính nếu không đặt).

## Cách nạp (đã triển khai)

- Interceptor `AuthorizationInterceptor` sẽ:
  - kiểm tra cookie `DEVHUB_AUTH` / header `Authorization` và verify JWT;
  - khi token hợp lệ, lấy `email` từ JWT subject;
  - load row `users` từ DB (id, email, first_name, last_name, created_at);
  - khởi tạo `UserObject.fromMap(row)` và gán `roles` từ `RoleService.getUserRolesByEmail(email)`;
  - lưu `UserObject` vào `AuthContext.set(user)` (ThreadLocal) và `request.setAttribute("currentUser", user)`;
  - xóa `AuthContext` trong `afterCompletion` để tránh leak.

## Cách sử dụng trong code

1) Trong controller (ví dụ handler method) — lấy trực tiếp từ model attribute (đã expose tự động):

```java
// trong controller method, Thymeleaf model đã có 'currentUser'
@GetMapping("/some")
public String somePage(@ModelAttribute("currentUser") UserObject currentUser, Model m) {
    if (currentUser != null) {
        m.addAttribute("name", currentUser.getDisplayName());
    }
    return "html/somepage";
}
```

2) Lấy từ request attribute:

```java
UserObject u = (UserObject) request.getAttribute("currentUser");
```

3) Lấy từ bất kỳ chỗ nào trong cùng thread request (ví dụ service/utility gọi từ controller):

```java
import com.devhub.ocr.app.systems.auth.AuthContext;

UserObject u = AuthContext.get();
if (u != null) {
    String email = u.getEmail();
}
```

4) Trong Thymeleaf template:

```html
<div th:if="${currentUser}">
  Xin chào, <span th:text="${currentUser.displayName}">User</span>
</div>
<div th:unless="${currentUser}">
  <a th:href="@{/auth/sign-in}">Đăng nhập</a>
</div>
```

## Lưu ý & best practices

- `AuthContext.get()` an toàn chỉ trong thread xử lý request — không dùng cho background threads hoặc tasks (nếu muốn dùng cho async, truyền object rõ ràng vào task).
- Nếu muốn injection trực tiếp vào parameter controller như `@CurrentUser UserObject user`, có thể thêm `HandlerMethodArgumentResolver` (tôi có thể implement nếu bạn muốn).
- Interceptor chỉ load user khi JWT hợp lệ; nếu cần load user cho mọi request (kể cả public), có thể điều chỉnh interceptor hoặc thêm filter riêng.
- `UserObject.fromMap(Map<String,Object>)` hỗ trợ map kết quả truy vấn của `DatabasePlugin.query(...)` và tự parse `created_at` (ISO-8601 hoặc epoch millis).

## Tiếp theo (tùy chọn)

- Thêm `@CurrentUser` resolver để inject tự động.
- Đồng bộ `UserObject` với session hoặc redis nếu muốn giữ user trên nhiều node (hiện là stateless JWT + DB lookup on demand).

---
File: `backends/devhubocr/src/main/java/com/devhub/ocr/app/systems/auth/UserObject.java`
File: `backends/devhubocr/src/main/java/com/devhub/ocr/app/systems/auth/AuthContext.java`
Interceptor chịu trách nhiệm nạp: `backends/devhubocr/src/main/java/com/devhub/ocr/app/systems/config/AuthorizationInterceptor.java`
