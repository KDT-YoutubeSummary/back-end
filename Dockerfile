# YouSum Backend Dockerfile
# Java Spring Boot + Python Whisper 환경
FROM openjdk:17-jdk-slim AS builder

# Build arguments
ARG JAR_FILE=build/libs/*.jar

# Install Python and required packages
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    python3-venv \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Gradle files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy source code
COPY src src

# Build the application
RUN chmod +x ./gradlew
RUN ./gradlew bootJar

# Production stage
FROM openjdk:17-jdk-slim

# Install system dependencies
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    python3-venv \
    ffmpeg \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Create Python virtual environment
RUN python3 -m venv /app/venv
ENV PATH="/app/venv/bin:$PATH"

# Install Python dependencies
RUN pip install --no-cache-dir \
    flask==2.3.3 \
    yt-dlp==2023.9.24 \
    faster-whisper==0.9.0 \
    torch \
    numpy

# Copy JAR file from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Copy Python scripts
COPY yt/ yt/

# Copy resources if needed
COPY src/main/resources/textfiles/ textfiles/

# Create necessary directories
RUN mkdir -p /tmp /app/logs

# Expose ports
EXPOSE 8080 8000

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Start script to run both Spring Boot and Python services
COPY start.sh /app/start.sh
RUN chmod +x /app/start.sh

CMD ["/app/start.sh"] 