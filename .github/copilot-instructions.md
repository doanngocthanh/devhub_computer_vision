## Quick instructions for AI coding agents — devhub_computer_vision

This repository contains a Spring Boot backend module `backends/devhubocr` (Java 21, Thymeleaf) plus small Python helpers and SQLite migrations. The goal of this file is to give focused, actionable knowledge an AI agent needs to be productive here.

- Big picture: `backends/devhubocr` is the main service. It renders server-side HTML with Thymeleaf (`src/main/resources/templates`) and uses a local SQLite DB (`db_local/*`). Heavy-lifting CV deps (ONNX, OpenCV) are declared in `pom.xml` and may require native libs on the host.

- Primary dev commands (run from repo root):
  - Build: `cd backends/devhubocr && ./mvnw clean package`
  - Run (dev): `cd backends/devhubocr && ./mvnw spring-boot:run`
  - Tests: `cd backends/devhubocr && ./mvnw test`
  - Build image (optional): `cd backends/devhubocr && ./mvnw spring-boot:build-image`

- Important files & places to read first:
  - `backends/devhubocr/pom.xml` — dependency list (note duplicate OpenCV entries; prefer deduping)
  - `backends/devhubocr/HELP.md` — project-specific build/run notes and caveats
  - `backends/devhubocr/src/main/java/com/devhub/ocr` — Java sources (controllers/services/QA modules)
  - `backends/devhubocr/src/main/resources/templates/common/` — shared Thymeleaf fragments (`header.html`, `sidebar.html`)
  - `db_local/sql/` — ordered SQLite migration scripts
  - `backends/devhubocr/constructor/userobject.md` — describes `UserObject`, `AuthContext`, and `AuthorizationInterceptor` patterns used across controllers/templates

- Conventions & patterns an AI should follow when changing code:
  - UI: update fragments in `templates/common/` for site-wide layout changes (e.g., `sidebar-new.html`). Page templates live in `templates/html/**`.
  - Auth/user: the app uses JWT in cookie `DEVHUB_AUTH` → `AuthorizationInterceptor` builds `UserObject` and stores it in `AuthContext` (ThreadLocal) and `request.setAttribute("currentUser", user)`; prefer using `AuthContext.get()` or the `currentUser` model attribute.
  - Menus/roles: menus and path-role mappings are stored in DB. Use `MenuService` / `RoleService` patterns and DB migrations in `db_local/sql/` when changing schema.
  - DB: migrations are plain .sql files applied lexically. Ensure SQL is SQLite-compatible and avoid complex multi-statement constructs that the runner may not parse.
  - Native deps: when touching ONNX/OpenCV code, add a note that native libraries or platform-specific testing may be required; don't assume unit tests run successfully on CI without them.

- Integration points to check before edits:
  - `pocketbase-kotlin` usages under `src/main/java` (if modifying auth/user flows)
  - `uploads/` mapping (don't return filesystem paths in responses)
  - `compose.yaml` currently has no runnable services — running in Docker Compose may require adding services

- Concrete examples to reference in PRs or patches:
  - To modify sidebar/menu: edit `src/main/resources/templates/common/sidebar-new.html` and update `MenuService` if structure changes.
  - To add a new QA module follow the repo module pattern: create `src/main/java/com/devhub/ocr/QA/A0/<MODULE>/trx` for controllers and `mod` for business logic (see `constructor/final.md` for conventions).
  - Use `backends/devhubocr/constructor/userobject.md` when creating or consuming user-related data structures.

- Quality checks before submitting changes:
  - Run `./mvnw -f backends/devhubocr/pom.xml -DskipTests=false test` locally.
  - If you change DB schema, add a migration file to `db_local/sql/` and verify with `sqlite3 db_local/database.db` queries.

If anything above is unclear or you want this trimmed to a different audience (frontend-only, infra, or tests), tell me which focus and I will iterate.
