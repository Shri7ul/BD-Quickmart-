# =====================================================================
# BD QuickMart backend - production container (Render Web Service)
# ---------------------------------------------------------------------
# Multi-stage build pins BOTH toolchains explicitly:
#   * Build stage  : Maven 3.9.9 + Eclipse Temurin JDK 21
#   * Runtime stage: Eclipse Temurin JRE 21 (no Maven, no JDK, no sources)
#
# Build context is the `backend/` directory (see render.yaml dockerContext).
# Render injects `PORT`; the app binds via ${PORT:${SERVER_PORT:8080}} in
# application.yml. EXPOSE below is documentation only - PORT is authoritative.
#
# NO credentials are baked into this image. All runtime configuration is
# supplied through environment variables / Render secret env vars.
# =====================================================================

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
# Tests are run separately in CI (`mvn -B clean verify`). Here we only produce
# the executable jar; -DskipTests compiles test sources but does not run them.
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as a non-root user.
RUN groupadd --system bdqm && useradd --system --gid bdqm bdqm

COPY --from=build /workspace/target/bd-quickmart-backend-0.1.0.jar /app/app.jar
RUN chown -R bdqm:bdqm /app
USER bdqm

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
