#!/bin/bash

# YouSum 서비스 시작 스크립트
echo "Starting YouSum services..."

# Python Flask 서비스 시작 (백그라운드)
echo "Starting Python Whisper service..."
cd /app && python3 yt/yt_whisper.py &
PYTHON_PID=$!

# Spring Boot 애플리케이션 시작
echo "Starting Spring Boot application..."
java -jar \
    -Dspring.profiles.active=production \
    -Xmx1024m \
    -Xms512m \
    app.jar &
JAVA_PID=$!

# 종료 시그널 처리
trap 'echo "Stopping services..."; kill $PYTHON_PID $JAVA_PID; wait' SIGTERM SIGINT

# 서비스 상태 확인
wait $JAVA_PID $PYTHON_PID 