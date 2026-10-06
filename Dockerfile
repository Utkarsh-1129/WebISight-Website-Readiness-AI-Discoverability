# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Leverage Docker layer caching: resolve dependencies before copying source
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a non-root user
RUN useradd --system --create-home appuser
USER appuser

COPY --from=build /build/target/ai-readiness-webapp.jar app.jar

EXPOSE 8080

# Reasonable JVM defaults for a small container; override via JAVA_OPTS if needed
ENV JAVA_OPTS=""

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
