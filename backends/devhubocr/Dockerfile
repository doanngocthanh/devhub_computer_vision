FROM eclipse-temurin:21-jdk-noble as builder
WORKDIR /build
# copy only what's needed for a maven build (leverage cache)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
# Ensure mvnw is executable
RUN chmod +x mvnw || true
# Build jar (skip tests by default in image build)
RUN ./mvnw -DskipTests package -e -B

# runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app
# copy the built jar from builder
# Install Python 3.10 if available in distro, otherwise fall back to system python3.
# We install pip as well. Keep the layer small and clean apt lists after install.
RUN apt-get update \
		&& apt-get install -y --no-install-recommends ca-certificates curl gnupg2 \
		&& (apt-get install -y --no-install-recommends python3.10 python3.10-venv python3-pip || apt-get install -y --no-install-recommends python3 python3-pip) \
		&& rm -rf /var/lib/apt/lists/* || true

# Make sure `python` and `pip` commands are available
RUN if command -v python3.10 >/dev/null 2>&1; then \
			ln -sf /usr/bin/python3.10 /usr/bin/python || ln -sf /usr/bin/python3 /usr/bin/python; \
		else \
			ln -sf /usr/bin/python3 /usr/bin/python; \
		fi \
		&& if command -v pip3 >/dev/null 2>&1; then ln -sf /usr/bin/pip3 /usr/bin/pip || true; fi

COPY --from=builder /build/target/*.jar app.jar
# expose port
EXPOSE 8080
# default runtime environment
ENV JAVA_OPTS="-Xms128m -Xmx512m"
# By default database and migrations will be read from /data inside container
ENV devhub.db.path=/data/database.db
ENV devhub.db.migrations=/data/sql
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.datasource.url=jdbc:sqlite:${devhub.db.path} -jar /app/app.jar"]
