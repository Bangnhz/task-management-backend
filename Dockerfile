# ==========================================
# Stage 1: Build stage with Gradle & JDK 23
# ==========================================
FROM eclipse-temurin:23-jdk-alpine AS build

WORKDIR /app

# Copy gradle wrapper and project definition files
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./

# Fix Windows CRLF line endings on gradlew and give execute permission
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew

# Pre-download dependencies to leverage Docker cache
RUN ./gradlew dependencies --no-daemon || true

# Copy source code and build production jar
COPY src/ src/
RUN ./gradlew bootJar -x test --no-daemon && \
    cp build/libs/$(ls build/libs | grep -v 'plain' | head -n 1) app.jar

# ==========================================
# Stage 2: Runtime environment with JRE 23
# ==========================================
FROM eclipse-temurin:23-jre-alpine

WORKDIR /app

# Run as non-root user for security best practices
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy executable jar from build stage
COPY --from=build --chown=appuser:appgroup /app/app.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

# Configure JVM flags and Spring active profile
ENV JAVA_OPTS="-Xms256m -Xmx512m" \
    SPRING_PROFILES_ACTIVE="prod"

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]


# Stage 1
# JDK + source code
#       ↓
#    build app
#       ↓
#     app.jar
#       ↓
# Stage 2
# JRE + app.jar
#       ↓
#   FINAL IMAGE
#       ↓
#    Container