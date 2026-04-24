# Stage 1: Build with Maven
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
  # Cache dependencies (faster rebuilds)
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests
  
  # Stage 2: Run with JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
  # Copy JAR from builder
COPY --from=builder /app/target/tech-pulse-*.jar ./app.jar
  # Expose port (matches Spring Boot's server.port)
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]