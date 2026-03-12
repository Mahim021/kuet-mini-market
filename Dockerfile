# ---- Stage 1: Build ----
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache dependencies by copying pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build (skip tests — H2 tests run locally, not inside Docker build)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Stage 2: Run ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
