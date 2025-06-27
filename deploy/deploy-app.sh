#!/bin/bash

# YouSum 애플리케이션 배포 스크립트
set -e

echo "🚀 YouSum 애플리케이션 EC2 배포를 시작합니다..."

# aws-resources.txt에서 정보 로드
if [ ! -f "aws-resources.txt" ]; then
    echo "❌ aws-resources.txt 파일이 없습니다. 먼저 인프라 설정을 완료하세요."
    exit 1
fi

source aws-resources.txt

# 환경변수 파일 확인
if [ ! -f ".env.production" ]; then
    echo "❌ .env.production 파일이 없습니다. create-rds.sh를 실행하거나 수동으로 생성하세요."
    exit 1
fi

# 1. 프로젝트 파일들을 EC2로 복사
echo "📦 프로젝트 파일을 EC2로 복사 중..."

# 배포에 필요한 파일들만 선별하여 임시 디렉토리에 복사
TEMP_DIR=$(mktemp -d)
echo "임시 디렉토리: $TEMP_DIR"

# 필수 파일들 복사
cp -r src $TEMP_DIR/
cp -r gradle $TEMP_DIR/
cp -r yt $TEMP_DIR/
cp build.gradle $TEMP_DIR/
cp settings.gradle $TEMP_DIR/
cp gradlew $TEMP_DIR/
cp gradlew.bat $TEMP_DIR/
cp Dockerfile $TEMP_DIR/
cp start.sh $TEMP_DIR/
cp init.sql $TEMP_DIR/
cp .env.production $TEMP_DIR/.env

# production용 docker-compose 파일 생성
cat > $TEMP_DIR/docker-compose.production.yml << 'EOF'
version: '3.8'

services:
  yousum-backend:
    build: .
    container_name: yousum-backend
    ports:
      - "8080:8080"
      - "8000:8000"
    env_file:
      - .env
    environment:
      - SPRING_PROFILES_ACTIVE=production
    volumes:
      - ./logs:/app/logs
      - /tmp:/tmp
    restart: unless-stopped
    networks:
      - yousum-network

networks:
  yousum-network:
    driver: bridge
EOF

# 배포 스크립트 생성
cat > $TEMP_DIR/deploy-on-ec2.sh << 'EOF'
#!/bin/bash

# EC2에서 실행될 배포 스크립트
set -e

echo "🔧 EC2에서 YouSum 애플리케이션 배포 시작..."

# 기존 컨테이너 정리
echo "🧹 기존 컨테이너 정리 중..."
docker-compose -f docker-compose.production.yml down || true
docker system prune -f

# AWS 자격 증명 설정 (환경변수에서)
if [ ! -z "$AWS_ACCESS_KEY_ID" ] && [ ! -z "$AWS_SECRET_ACCESS_KEY" ]; then
    aws configure set aws_access_key_id $AWS_ACCESS_KEY_ID
    aws configure set aws_secret_access_key $AWS_SECRET_ACCESS_KEY
    aws configure set default.region $AWS_REGION
    echo "✅ AWS 자격 증명 설정 완료"
fi

# 애플리케이션 빌드 및 시작
echo "🏗️ 애플리케이션 빌드 중..."
docker-compose -f docker-compose.production.yml build --no-cache

echo "🚀 애플리케이션 시작 중..."
docker-compose -f docker-compose.production.yml up -d

# 헬스 체크
echo "🔍 애플리케이션 상태 확인 중..."
sleep 30

# 컨테이너 상태 확인
docker ps

# API 헬스 체크
if curl -f http://localhost:8080/actuator/health; then
    echo "✅ 애플리케이션이 성공적으로 배포되었습니다!"
else
    echo "❌ 애플리케이션 헬스 체크 실패"
    echo "로그 확인:"
    docker-compose -f docker-compose.production.yml logs --tail=50
    exit 1
fi

echo "📋 배포 완료 정보:"
echo "Spring Boot API: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8080"
echo "Python Whisper: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8000"
echo "로그 확인: docker-compose -f docker-compose.production.yml logs -f"
EOF

chmod +x $TEMP_DIR/deploy-on-ec2.sh

# 2. EC2로 파일 전송
echo "📤 EC2로 파일 전송 중..."
rsync -avz -e "ssh -i ${KEY_NAME}.pem -o StrictHostKeyChecking=no" \
    --exclude='.git' \
    --exclude='build' \
    --exclude='bin' \
    --exclude='*.log' \
    $TEMP_DIR/ ubuntu@$PUBLIC_IP:/home/ubuntu/yousum/

# 3. EC2에서 배포 실행
echo "🎯 EC2에서 배포 실행 중..."
ssh -i ${KEY_NAME}.pem -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP << 'ENDSSH'
cd /home/ubuntu/yousum

# 실행 권한 부여
chmod +x deploy-on-ec2.sh
chmod +x gradlew
chmod +x start.sh

# 배포 실행
./deploy-on-ec2.sh
ENDSSH

# 4. 배포 상태 확인
echo "🔍 배포 상태 최종 확인..."
sleep 10

# API 엔드포인트 테스트
if curl -f "http://$PUBLIC_IP:8080/actuator/health"; then
    echo ""
    echo "🎉 YouSum 애플리케이션이 성공적으로 배포되었습니다!"
    echo ""
    echo "📋 접속 정보:"
    echo "🌐 Spring Boot API: http://$PUBLIC_IP:8080"
    echo "🐍 Python Whisper: http://$PUBLIC_IP:8000"
    echo "📊 API 문서: http://$PUBLIC_IP:8080/swagger-ui.html"
    echo "❤️ Health Check: http://$PUBLIC_IP:8080/actuator/health"
    echo ""
    echo "📝 관리 명령어:"
    echo "SSH 접속: ssh -i ${KEY_NAME}.pem ubuntu@$PUBLIC_IP"
    echo "로그 확인: docker-compose -f docker-compose.production.yml logs -f"
    echo "컨테이너 재시작: docker-compose -f docker-compose.production.yml restart"
    echo "컨테이너 중지: docker-compose -f docker-compose.production.yml down"
else
    echo "❌ 배포 실패. SSH로 접속하여 로그를 확인하세요:"
    echo "ssh -i ${KEY_NAME}.pem ubuntu@$PUBLIC_IP"
    echo "cd /home/ubuntu/yousum && docker-compose -f docker-compose.production.yml logs"
fi

# 임시 디렉토리 정리
rm -rf $TEMP_DIR
echo "🧹 임시 파일 정리 완료" 