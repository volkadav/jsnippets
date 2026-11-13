# Build stage - pin the base image to an audited digest for reproducibility.
FROM eclipse-temurin:21-jdk-alpine-3.24@sha256:82801f9dcdcbad5a05082aba8c86d1da5b72ccb498e71318b1f91b5a477887b3 AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first for dependency caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Cache bust arg - set to source hash or timestamp to force rebuild
ARG CACHEBUST=1

# Copy source code (cache invalidates when src changes OR CACHEBUST changes)
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Runtime stage, use a pinned JRE-only base.
FROM eclipse-temurin:21-jre-alpine-3.24@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474

# Build arguments for dynamic metadata
ARG BUILD_DATE=unknown
ARG APP_VERSION=unknown
ARG VCS_REF=unknown

# Expose build metadata to the application (surfaced as a tooltip on the home page).
# APP_VERSION is a fallback; the authoritative version comes from build-info.properties.
ENV APP_VERSION="${APP_VERSION}" \
  APP_GIT_COMMIT="${VCS_REF}"

# OCI Image metadata labels
LABEL org.opencontainers.image.title="jsnippets" \
  org.opencontainers.image.description="Snippets Server - A progress snippet sharing platform" \
  org.opencontainers.image.version="${APP_VERSION}" \
  org.opencontainers.image.created="${BUILD_DATE}" \
  org.opencontainers.image.revision="${VCS_REF}" \
  org.opencontainers.image.source="https://github.com/volkadav/jsnippets" \
  org.opencontainers.image.licenses="Apache-2.0" \
  org.opencontainers.image.vendor="norrisjackson.com"

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy the built artifact from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose application port
EXPOSE 8080

# Health check (optional but recommended)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM optimization flags
ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:+UseG1GC \
  -XX:+OptimizeStringConcat \
  -Djava.security.egd=file:/dev/./urandom"

# Run the application with optimized JVM settings
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
