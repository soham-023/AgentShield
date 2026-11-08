# ============================================================
#  AgentShield: multi-stage Docker build
# ============================================================
#  Build:  docker build -t agentshield .
#  Run:    docker run -p 8080:8080 \
#            -e AGENTSHIELD_DB_URL=jdbc:postgresql://host.docker.internal:5432/agentshield \
#            -e AGENTSHIELD_JWT_SECRET=change-me \
#            agentshield
# ============================================================

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Resolve dependencies first so they are cached between source changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S agentshield && adduser -S agentshield -G agentshield
COPY --from=build /app/target/agentshield-*.jar app.jar
USER agentshield

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
