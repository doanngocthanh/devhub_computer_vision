
# DevHub OCR — Project Report, Structure & Developer Guide

This document summarizes the `backends/devhubocr` backend: project layout, templates/layout approach, authentication/authorization flow, database & migration strategy, menu/role features, and developer usage notes (including `UserObject` integration). It synthesizes content from `projects.md` and `userobject.md` into a single reference.

## 1. Project overview

- Project: `backends/devhubocr` — Spring Boot application using Thymeleaf for server-side HTML rendering.
- Language: Java (Maven), Thymeleaf templates, SQLite (sql2o via DatabasePlugin).
- UI: Tabler admin template (assets copied into `static/`).

Key folders:

- `src/main/java/com/devhub/ocr/` — application Java sources.
	- Modules use the convention `AA/A0/AAA0_0100` etc.
		- `trx/` — controllers (request handlers)
		- `mod/` — business logic modules
		- `batch/` — scheduled jobs
- `src/main/resources/templates/` — Thymeleaf templates. Current pages use full-page HTML files with fragment includes under `templates/common/`.
- `src/main/resources/static/` — CSS/JS (Tabler assets) and project static files.
- `db_local/` — SQLite database file and migrations (`db_local/database.db`, `db_local/sql/*.sql`).

## 2. Templates & Layout

- Approach: full-page templates that include shared fragments (recommended for simplicity with Thymeleaf). Shared fragments live in `templates/common/`:
	- `header.html`, `footer.html`, `layout.html`, `fragments.html`, `sidebar.html` (menu)
- Page templates are under `templates/html/{p1}/{p2}/{page}/` (example: `templates/html/AA/A0/AAA0_0100/AAA0_0100.html`).
- Use Tabler assets from `static/css` and `static/js`. Prefer using `th:href` / `th:src` to keep paths correct in different environments.

## 3. Static files & uploads

- Uploads are served from the project `uploads/` directory via `WebMvcConfig` mapping `/uploads/** -> file:./uploads/`.
- There is an uploads health endpoint and controllers in `app/systems/file` (see `UploadsController`).

## 4. Authentication & User object

Authentication design:

- JWT token stored in cookie `DEVHUB_AUTH` (or `Authorization: Bearer <token>` header).
- `AuthService` handles register/authenticate and JWT generation/validation. Uses java-jwt and BCrypt for password hashing.

User object & convenience access:

- `UserObject` (class at `src/main/java/com/devhub/ocr/app/systems/auth/UserObject.java`) is a DTO that holds:
	- `id`, `email`, `firstName`, `lastName`, `roles` (Set<String>), `createdAt`, `displayName`.
- `AuthorizationInterceptor` verifies the JWT, extracts `email` (subject), and then loads the user row from `users` table.
- Interceptor builds a `UserObject` using `UserObject.fromMap(row)`, attaches roles via `RoleService.getUserRolesByEmail(email)`, and then stores it in:
	- `AuthContext` (a ThreadLocal holder), and
	- request attribute `currentUser`.
- `CurrentUserModelAdvice` exposes `currentUser` into all controller models/templates automatically.

Usage patterns (examples):

- In Thymeleaf templates: use `${currentUser.displayName}` or `th:if="${currentUser}"`.
- In controllers: accept `@ModelAttribute("currentUser") UserObject currentUser`, or fetch from `AuthContext.get()` or `request.getAttribute("currentUser")`.

Notes:

- `AuthContext` is request-thread-scoped (ThreadLocal) and cleared after completion. Do not use it in background threads; pass user object explicitly into async tasks.
- If you prefer method parameter injection (`@CurrentUser`), implement a `HandlerMethodArgumentResolver` (not currently implemented by default).

## 5. Authorization & path-role mapping

- Role model:
	- `roles(id, role, description)` — canonical roles
	- `users_roles(id, user_id, role)` — user-role assignments
	- `path_roles(id, path, role)` — roles that apply to specific path keys (dotted keys like `AA.A0.AAA0_0100`)

- `AuthorizationInterceptor` behavior:
	- early checks JWT from cookie/header and fetches user roles; IT role bypasses checks.
	- derives candidate path keys from the request URI (most specific to least), queries `path_roles` for matching roles, and enforces access.
	- special-case: if a role `RLZZANY` is assigned to a path, allow everyone.

Implementation notes & fixes applied:

- `RoleService` includes utilities to get/set roles for a path and for users, and to bootstrap `roles` and `users_roles` tables.
- Duplicate `(path,role)` rows were observed in DB historically; to fix this the code now:
	- cleans malformed comma-separated path rows on startup (splits into normalized rows),
	- dedupes existing entries with `DELETE FROM path_roles WHERE id NOT IN (SELECT MIN(id) FROM path_roles GROUP BY path, role)`,
	- uses `INSERT OR IGNORE` when inserting path-role rows to avoid duplicate inserts.
- A migration `db_local/sql/20251119__unique_path_roles.sql` was added to create a UNIQUE index on `(path, role)` to prevent duplicates going forward.

## 6. Menu management

- Menus are stored in a `menus` table with fields like `id, title, icon, path, parent_id, roles`.
- `MenuService` manages CRUD and builds a tree for the sidebar. `AutoMenu` registration scans annotated controllers and inserts missing menu rows on startup.
- Admin pages (examples) exist to manage menus (`AAA0_0102`) and to map menus into the Path Roles admin (`AAA0_0100`).

## 7. Database & migrations

- Database file (SQLite): `/workspaces/devhub_computer_vision/db_local/database.db`.
- Migrations directory: `/workspaces/devhub_computer_vision/db_local/sql`.
	- Migration runner applies scripts in lexical order and records applied migrations in `schema_migrations`.
	- Avoid complex SQL structures with embedded semicolons that confuse naive statement splitters.
- Example migrations added:
	- `20251118__create_roles_tables.sql` — creates roles, users_roles, path_roles and indexes.
	- `20251119__unique_path_roles.sql` — unique index and dedupe command.

## 8. Running & testing

Build and run (dev):

```bash
cd backends/devhubocr
./mvnw -DskipTests package
./mvnw -DskipTests spring-boot:run
```

Quick checks:

- Verify migrations applied: check `schema_migrations` table and the created tables.
- Inspect `path_roles` after dedupe:

```bash
sqlite3 /workspaces/devhub_computer_vision/db_local/database.db "SELECT id,path,role FROM path_roles ORDER BY id;"
```

## 9. Developer notes & recommendations

- Use the `UserObject`/`AuthContext` pattern for simple per-request user data access. For more advanced security or integration with Spring Security, consider migrating to Spring Security's Authentication and Principal abstractions.
- Keep `menus` in DB for dynamic sidebars. Use `MenuService` to build hierarchical structures.
- Add a migration to enforce `UNIQUE(path,role)` (already added as `20251119__unique_path_roles.sql`).
- Consider adding a `@CurrentUser` resolver for nicer controller signatures.

## 10. Files of interest (quick index)

- Java
	- `src/main/java/com/devhub/ocr/app/systems/config/AuthorizationInterceptor.java` — JWT verification, role checks, and user loading into `AuthContext`.
	- `src/main/java/com/devhub/ocr/app/systems/auth/UserObject.java` — DTO for current user.
	- `src/main/java/com/devhub/ocr/app/systems/auth/AuthContext.java` — ThreadLocal holder.
	- `src/main/java/com/devhub/ocr/auth/mod/AuthService.java` — register & authenticate.
	- `src/main/java/com/devhub/ocr/auth/mod/RoleService.java` — role/path management and cleanup.
	- `src/main/java/com/devhub/ocr/app/systems/config/WebMvcConfig.java` — interceptor registration and resource handlers.
	- `src/main/java/com/devhub/ocr/app/systems/auth/CurrentUserModelAdvice.java` — exposes `currentUser` to views.
	- `src/main/java/com/devhub/ocr/app/systems/menu/MenuService.java` — menu CRUD and tree builder (if present).

- Templates
	- `src/main/resources/templates/common/*` — shared fragments (header/footer/sidebar/layout).
	- `src/main/resources/templates/html/**` — page templates.

- DB & Migrations
	- `db_local/database.db` — current SQLite DB file.
	- `db_local/sql/` — migration scripts (e.g. `20251118__create_roles_tables.sql`, `20251119__unique_path_roles.sql`).

## 11. Next steps (suggested)

1. Optionally implement `@CurrentUser` method argument resolver for controllers.
2. Run full application smoke-tests (login, menu admin, path-role admin) and verify templates show `currentUser` as expected.
3. Harden migrations: convert SQL scripts to use a robust runner or switch to Flyway/Liquibase for complex environments.
4. Add tests around `RoleService` to ensure dedupe, set/get path roles behave correctly.

---
This report brings together the repository conventions, implemented features, and developer guidance to get productive quickly. If you'd like, I can now:

- run the app and exercise the login → verify `currentUser` in templates, or
- implement `@CurrentUser` resolver so controllers can accept `UserObject` as a parameter.

Tell me which next action you prefer and I'll proceed.

