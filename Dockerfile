# -----------------------------------------------------------------------------
# Stage 1: Build the Spring Boot application (named as 'builder')
# -----------------------------------------------------------------------------
FROM openjdk:17-jdk-slim AS builder

# Set working directory for the builder stage
WORKDIR /app

# Copy Gradle wrapper files and source code
# Cache Gradle dependencies by copying only necessary files first
COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle . 
# build.gradle 파일도 빌드에 필수적이므로 추가
COPY src src

# Make gradlew executable
RUN chmod +x ./gradlew

# Download dependencies (this step will be cached unless build.gradle changes)
# --no-daemon 플래그는 Docker 환경에서 Gradle 데몬이 백그라운드에서 실행되지 않도록 하여 빌드 안정성을 높입니다.
# build.gradle만 복사하여 의존성을 미리 다운로드하면, 소스 코드가 변경되어도 이 단계는 캐싱되어 빌드 속도가 빨라집니다.
RUN ./gradlew dependencies --no-daemon

# Build the Spring Boot JAR
# --no-daemon 플래그는 Docker 환경에서 Gradle 데몬이 백그라운드에서 실행되지 않도록 하여 빌드 안정성을 높입니다.
RUN ./gradlew bootJar --no-daemon

# -----------------------------------------------------------------------------
# Stage 2: Create the final production-ready image (named as 'runner')
# -----------------------------------------------------------------------------
# Python 및 Java 런타임 모두를 포함할 수 있는 경량 이미지 선택
FROM openjdk:17-jdk-slim AS runner 
# 또는 ubuntu:22.04 등 필요한 베이스 이미지로 변경 가능

# 환경 변수 설정
ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8

# 시스템 패키지 설치 및 정리
# build-essential은 보통 빌드 시에만 필요하고 런타임 이미지에는 필요하지 않습니다.
# 하지만 Python 라이브러리 (특히 numpy, torch 등) 중 일부는 C/C++ 컴파일이 필요할 수 있으므로,
# 런타임 이미지에서 설치해야 한다면 포함시켜야 합니다.
# 여기서는 런타임에 필요하다고 가정하고 포함시켰습니다.
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    python3-venv \
    ffmpeg \
    curl \
    build-essential \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# 작업 디렉토리 설정
WORKDIR /app

# Python 가상 환경 설정
RUN python3 -m venv /app/venv
ENV PATH="/app/venv/bin:$PATH" 
# 가상 환경 경로를 PATH에 추가

# Python 라이브러리 설치
RUN pip install --upgrade pip
RUN pip install flask==2.3.3
RUN pip install yt-dlp==2023.9.24
RUN pip install numpy
# PyTorch URL에서 '<'와 '>'를 제거했습니다. 이는 쉘 문법 오류를 유발할 수 있습니다.
RUN pip install torch --index-url https://download.pytorch.org/whl/cpu
RUN pip install faster-whisper==0.9.0

# Build Stage에서 생성된 Spring Boot JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# Python 스크립트 및 시작 스크립트 복사
COPY yt/ yt/
COPY start.sh /app/start.sh

# 필요한 디렉토리 생성 및 권한 설정
RUN chmod +x /app/start.sh
RUN mkdir -p /tmp /app/logs /app/textfiles

# 포트 노출
EXPOSE 8080 8000

# Health check
# URL에서 '<'와 '>'를 제거했습니다.
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 컨테이너 시작 시 실행될 기본 명령
CMD ["/app/start.sh"]
