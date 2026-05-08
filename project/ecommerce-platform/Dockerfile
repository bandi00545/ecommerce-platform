# =============================================================================
# Dockerfile — used by ALL microservices
# Multi-stage build: Build JAR in stage 1, run lean JRE in stage 2
#
# USAGE (from service root directory, e.g. user-service/):
#   docker build --build-arg SERVICE_NAME=user-service -t user-service:1.0.0 .
#
# Or via docker-compose.yml (which sets build context and args automatically)
# =============================================================================

# ─────────────────────────────────────────────────────────────
# STAGE 1: Build — Maven + JDK 17 — compiles the JAR
# ─────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /workspace

# Copy parent POM first (layer caching: only re-download deps when POM changes)
COPY pom.xml .

# Copy ecommerce-common (all services depend on it, must be built first)
COPY ecommerce-common/pom.xml               ecommerce-common/pom.xml
COPY ecommerce-common/src                   ecommerce-common/src

# Copy the specific service POM and source
ARG SERVICE_NAME
COPY ${SERVICE_NAME}/pom.xml               ${SERVICE_NAME}/pom.xml
COPY ${SERVICE_NAME}/src                   ${SERVICE_NAME}/src

# Download dependencies (cached layer if POM unchanged)
RUN mvn dependency:go-offline -pl ecommerce-common,${SERVICE_NAME} -am -q

# Build: install common, then package service (skip tests in Docker build)
RUN mvn install -pl ecommerce-common -am -DskipTests -q && \
    mvn package -pl ${SERVICE_NAME} -DskipTests -q

# Find and rename the JAR for consistent naming
RUN find ${SERVICE_NAME}/target -name "*.jar" \
        ! -name "*-sources.jar" \
        ! -name "*-javadoc.jar" \
        -exec cp {} /app.jar \;

# ─────────────────────────────────────────────────────────────
# STAGE 2: Runtime — lean JRE 17 only (no Maven, no JDK)
# eclipse-temurin:17-jre is ~250MB vs ~600MB for full JDK
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

# Create non-root user for security (never run as root in containers)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app.jar app.jar

# Set ownership
RUN chown appuser:appgroup app.jar

USER appuser

# JVM tuning for containerized environments:
# -XX:+UseContainerSupport       → respects Docker CPU/memory limits
# -XX:MaxRAMPercentage=75.0      → use 75% of container memory for heap
# -XX:+UseG1GC                   → G1 GC for low latency
# -XX:+ExitOnOutOfMemoryError    → crash fast (let Docker restart) on OOM
# -Djava.security.egd            → faster SecureRandom (important for JWT)
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:prod}", \
    "-jar", "app.jar"]

# Health check: Docker monitors this and restarts unhealthy containers
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:${SERVER_PORT:-8080}/actuator/health | \
        grep -q '"status":"UP"' || exit 1
