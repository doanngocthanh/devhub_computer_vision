# Proto samples and recommended patterns

Below are compact proto examples you can use as a starting point. Put these under `protos/` (or `backends/common/protos/`), then generate Java/Python code.

1) File transfer service (streaming)

```proto
syntax = "proto3";
package devhub.file;

option java_package = "com.devhub.ocr.grpc.file";
option java_multiple_files = true;

message UploadMetadata {
  string filename = 1;
  string content_type = 2;
  int64 total_bytes = 3; // optional: client can supply when known
}

message UploadChunk {
  UploadMetadata meta = 1; // present on first chunk
  bytes data = 2;
}

message UploadResult {
  string upload_id = 1;
  string path = 2; // stored file path (server-side)
  bool success = 3;
  string error = 4;
}

message DownloadRequest {
  string upload_id = 1;
}

message DownloadChunk {
  bytes data = 1;
}

service FileService {
  // client-streaming RPC for upload
  rpc Upload(stream UploadChunk) returns (UploadResult);
  // server-streaming RPC for download
  rpc Download(DownloadRequest) returns (stream DownloadChunk);
}
```

2) Control / Spark job API (simple)

```proto
syntax = "proto3";
package devhub.control;

message JobSpec {
  string job_name = 1;
  repeated string args = 2; // optional spark args or file paths
}

message JobHandle { string id = 1; }
message JobStatus { string id = 1; string state = 2; string message = 3; }

service ControlService {
  rpc SubmitSparkJob(JobSpec) returns (JobHandle);
  rpc GetJobStatus(JobHandle) returns (JobStatus);
  rpc CancelJob(JobHandle) returns (JobStatus);
}
```

Notes
- Keep protos minimal at first. Add fields for auth metadata, tracing IDs, and retries as needed.
- Use package and java_package options to control generated Java packages.
- Keep chunk sizes moderate; use streaming to avoid huge memory spikes.
