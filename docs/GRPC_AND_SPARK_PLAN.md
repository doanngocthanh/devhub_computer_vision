# gRPC + Apache Spark integration plan (high level)

Goal
- Add gRPC endpoints for file transfer and control so Java backend (`backends/devhubocr`) and Python helpers (`backends/python`) can exchange files and commands.
- Use Apache Spark (PySpark) for processing large files (OCR pipelines, batch transforms).
- Keep tokens/secrets on server-side; use TLS and auth for gRPC.

Reference implementation and inspiration
- Dynamic-gRPC-Introspection-System: https://github.com/doanngocthanh/Dynamic-gRPC-Introspection-System — use ideas for service reflection/introspection and dynamic loading of proto metadata if/when needed.

Repository layout (where changes live)
- Java gRPC server: `/workspaces/devhub_computer_vision/backends/devhubocr` (Spring Boot app)
- Python gRPC server/clients: `/workspaces/devhub_computer_vision/backends/python`
- Docs and artifacts: `/workspaces/devhub_computer_vision/docs`

Top-level components
- Proto definitions: a single source-of-truth directory (e.g., `protos/` at repo root or `backends/common/protos/`). Keep versioning and comments.
- Java server: implement service bindings (generated stubs) inside `com.devhub.ocr.grpc.*`. Expose gRPC on its own port (e.g., 6565). Integrate with Spring Boot lifecycle.
- Python worker: lightweight gRPC client/server for worker and Spark submission orchestration.
- File storage: persist uploads to local `uploads/` or S3-compatible storage. Spark reads from the same storage location.

File transfer design
- Use gRPC streaming (client-streaming for upload, server-streaming for download) to support large files and progress reporting.
- Chunk size: 64KB–1MB per message (tune per infra). Include metadata (original filename, mime-type, total-size when known).
- Optionally support resumable uploads: keep UUID upload id and accept offset/seek commands in RPC (adds complexity).
- Persist uploaded files to disk as they arrive; write atomically (write to temp then rename) to avoid partial reads by Spark.

Proto and service boundaries
- FileService (gRPC): Upload (stream UploadChunk) -> UploadResult; Download (DownloadRequest) -> stream DownloadChunk; GetStatus(UploadId) -> Status. See `docs/PROTO_SAMPLES.md` for a sample proto.
- ControlService (gRPC): SubmitSparkJob(JobSpec) -> JobHandle, GetJobStatus(JobHandle) -> JobStatus, CancelJob(JobHandle).

Security
- Use TLS for gRPC in production. For local dev, allow plaintext but keep it separate.
- Authenticate requests via JWT or mTLS. Validate user identity in Java Spring layer and map to filesystem namespaces (per-user directories) to avoid cross-user leakage.
- Never send bot/telegram tokens over gRPC to untrusted clients. Only server-side services access tokens.

Apache Spark integration
- Execution mode: start with local or standalone cluster (for development). For production, use YARN/Kubernetes/EMR as appropriate.
- Use PySpark jobs for heavy-lifting; Python worker will submit jobs using `spark-submit` or REST API.
- Data formats: use Parquet/Avro for intermediate structured data. For raw files (images/PDF), store as files and pass file paths to Spark tasks.
- Resource sizing: set driver/executor memory and cores based on input sizes. Provide configuration template for common input sizes.

Testing and CI
- Unit tests: generate stubs and write unit tests for service logic (Java: junit; Python: pytest).
- Integration tests: run an in-memory gRPC server/test client; create small files and assert upload/download and Spark job submission flow.
- Add a lightweight Docker Compose to start Java backend + Python worker + optional local Spark (or use a hosted spark for CI smoke tests).

Operational notes
- Logging: propagate tracing IDs in gRPC metadata for correlation.
- Backpressure: the Java server should enforce rate-limits and use disk buffering to prevent OOM on large concurrent uploads.
- Cleanup: implement retention policies for temporary files.

Next steps (minimal implementation plan)
1. Create `protos/` with `file_service.proto` and `control_service.proto` (see samples).
2. Add protobuf/maven plugin and a small integration module in `backends/devhubocr` to generate Java classes during build.
3. Create a Python package in `backends/python` with a gRPC client and a `grpc_server.py` skeleton.
4. Implement upload streaming in Java service and a Python client to test it.
5. Add Spark job submission flow (start with local `spark-submit` wrapper in Python).

If you confirm, I'll create the proto files and a small skeleton implementation plan (build snippets and commands) in `docs/` and add the proto files under `protos/` so we can begin code generation.
