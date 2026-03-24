# ──────────────────────────────────────────────────────────
#  Stage 1 — Build with Maven wrapper (uses project's mvnw)
# ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy wrapper + pom first (dependency layer cached separately)
COPY .mvn/  .mvn/
COPY mvnw   mvnw
COPY pom.xml pom.xml
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B -q

# Copy source and build (skip tests — CI handles testing)
COPY src ./src
RUN ./mvnw package -DskipTests -B -q

# ──────────────────────────────────────────────────────────
#  Stage 2 — Lean runtime image (JRE only, no compiler)
# ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Run as non-root for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
