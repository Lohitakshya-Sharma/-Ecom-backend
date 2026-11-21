# Stage 1: Build the application with Maven Wrapper
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /build

# Copy Maven wrapper and POM to leverage Docker cache for dependencies
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy project source and package the executable JAR
COPY src src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Lightweight, secure JRE runtime container
FROM eclipse-temurin:17-jre

WORKDIR /app

# Install curl for Docker healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Run as a dedicated non-root user for security
RUN groupadd -r appgroup && useradd -r -g appgroup -s /sbin/nologin appuser

# Copy executable JAR from builder stage
COPY --from=builder /build/target/ecommerce-0.0.1-SNAPSHOT.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]