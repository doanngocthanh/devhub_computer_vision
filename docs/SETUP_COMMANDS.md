# Setup commands and snippets (protoc, Maven, Python)

This document collects shell commands and minimal config snippets to generate gRPC code and wire up services.

1) Install protoc and plugins (local dev)

Linux (example):

```bash
# download protoc (choose version, e.g. 21.12)
PROTOC_VERSION=21.12
curl -L -o /tmp/protoc.zip https://github.com/protocolbuffers/protobuf/releases/download/v${PROTOC_VERSION}/protoc-${PROTOC_VERSION}-linux-x86_64.zip
unzip -o /tmp/protoc.zip -d $HOME/.local
export PATH="$HOME/.local/bin:$PATH"

# Java plugin (protobuf-java is a maven dependency; for gRPC Java codegen use the maven plugin or gradle plugin)

# Python tools
pip install grpcio grpcio-tools
```

2) Maven plugin snippet (add to `backends/devhubocr/pom.xml`)

Use `protobuf-maven-plugin` or `com.github.os72:protoc-jar-maven-plugin` to generate Java code during the Maven build. Example snippet for `protobuf-maven-plugin`:

```xml
<plugin>
  <groupId>org.xolstice.maven.plugins</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>0.6.1</version>
  <configuration>
    <protocArtifact>com.google.protobuf:protoc:3.21.12:exe:${os.detected.classifier}</protocArtifact>
    <pluginId>grpc-java</pluginId>
    <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.59.0:exe:${os.detected.classifier}</pluginArtifact>
  </configuration>
  <executions>
    <execution>
      <goals>
        <goal>compile</goal>
        <goal>compile-custom</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

3) Generate Python code from protos

```bash
python -m grpc_tools.protoc -I=protos --python_out=backends/python --grpc_python_out=backends/python protos/file_service.proto
```

4) Java – embed gRPC server into Spring Boot
- Create a @Configuration that starts the gRPC server on application start and registers service implementations (see `io.grpc.ServerBuilder`). Consider using existing libraries (e.g., `yidongnan/grpc-spring-boot-starter`) for easier wiring.

5) Docker / docker-compose notes
- Expose gRPC port (e.g., 6565) in `docker-compose.yml` for `devhubocr` and `backends/python` worker when needed.
- Add healthcheck for gRPC (use `grpc-health-probe` binary) or simple TCP check in compose.

6) Spark submit / integration
- Ensure Spark is installed in the environment used by the Python worker or available as a container. For local dev, use `pyspark` installed in a virtualenv or system Python.
- Example: submit local job from Python using subprocess

```py
import subprocess
subprocess.check_call(["spark-submit","--master","local[4]","path/to/job.py","--input", "/data/input", "--output", "/data/out"])
```

7) Testing and quick sanity
- Upload small test files via a test client (Python or Java) to validate streaming and end-to-end persistence.
- Add CI job step to compile protos and build both Java and Python artifacts.

If you want, I can add concrete `pom.xml` edits, a Spring Boot `GrpcServerConfiguration` skeleton, and a Python `grpc_server.py` skeleton under `backends/python` next. Confirm and I'll create those files and wire basic build/test commands.
