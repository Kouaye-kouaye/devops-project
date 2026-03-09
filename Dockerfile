# ══════════════════════════════════════════════════════════════════════════════
# Dockerfile multi-stage — DeployFast Task Manager
# Stage 1 : Build (Maven + JDK 21)
# Stage 2 : Runtime (JRE 21 Alpine minimal)
# ══════════════════════════════════════════════════════════════════════════════

# ─── STAGE 1 : BUILD ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

LABEL maintainer="devops@deployfast.io"
LABEL stage="builder"

WORKDIR /build

# Copier d'abord le pom.xml pour profiter du cache des dépendances Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress 2>/dev/null || true

# Copier les sources
COPY src ./src

# Build sans les tests (les tests sont exécutés dans le pipeline CI)
RUN mvn clean package -DskipTests -B --no-transfer-progress

# ─── STAGE 2 : RUNTIME ───────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="devops@deployfast.io"
LABEL version="1.0.0"
LABEL description="DeployFast Task Manager REST API"

# Sécurité : utilisateur non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copier uniquement le JAR depuis le stage builder
COPY --from=builder /build/target/*.jar app.jar

# Changement de propriétaire
RUN chown appuser:appgroup app.jar

USER appuser

# Exposition du port
EXPOSE 8080

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Options JVM pour conteneur (mémoire adaptée)
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
