#!/bin/bash

# YouSum 운영 모니터링 및 관리 스크립트
set -e

# aws-resources.txt에서 정보 로드
if [ ! -f "aws-resources.txt" ]; then
    echo "❌ aws-resources.txt 파일이 없습니다."
    exit 1
fi

source aws-resources.txt

# 함수 정의
show_help() {
    echo "YouSum 운영 관리 도구"
    echo ""
    echo "사용법: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  status      - 서비스 상태 확인"
    echo "  logs        - 실시간 로그 확인"
    echo "  restart     - 서비스 재시작"
    echo "  stop        - 서비스 중지"
    echo "  start       - 서비스 시작"
    echo "  health      - 헬스 체크"
    echo "  ssh         - EC2 SSH 접속"
    echo "  update      - 애플리케이션 업데이트"
    echo "  backup      - 데이터베이스 백업"
    echo "  clean       - 도커 이미지 정리"
    echo ""
}

check_status() {
    echo "🔍 YouSum 서비스 상태 확인 중..."
    echo ""
    
    echo "📊 EC2 인스턴스 상태:"
    aws ec2 describe-instances --instance-ids $INSTANCE_ID --region ap-northeast-2 \
        --query 'Reservations[0].Instances[0].[State.Name,PublicIpAddress,PrivateIpAddress]' \
        --output table
    
    echo ""
    echo "🗄️ RDS 데이터베이스 상태:"
    aws rds describe-db-instances --db-instance-identifier $DB_INSTANCE_ID --region ap-northeast-2 \
        --query 'DBInstances[0].[DBInstanceStatus,Endpoint.Address,Endpoint.Port]' \
        --output table
    
    echo ""
    echo "🐳 Docker 컨테이너 상태:"
    ssh -i ${KEY_NAME}.pem -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP \
        "cd /home/ubuntu/yousum && docker-compose -f docker-compose.production.yml ps"
    
    echo ""
    echo "🌐 API 헬스 체크:"
    if curl -s -f "http://$PUBLIC_IP:8080/actuator/health"; then
        echo "✅ Spring Boot API: 정상"
    else
        echo "❌ Spring Boot API: 비정상"
    fi
    
    if curl -s -f "http://$PUBLIC_IP:8000" > /dev/null; then
        echo "✅ Python Whisper: 정상"
    else
        echo "❌ Python Whisper: 비정상"
    fi
}

show_logs() {
    echo "📄 실시간 로그 확인 중... (Ctrl+C로 종료)"
    ssh -i ${KEY_NAME}.pem -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP \
        "cd /home/ubuntu/yousum && docker-compose -f docker-compose.production.yml logs -f"
}

restart_services() {
    echo "🔄 서비스 재시작 중..."
    ssh -i ${KEY_NAME}.pem -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP \
        "cd /home/ubuntu/yousum && docker-compose -f docker-compose.production.yml restart"
    echo "✅ 서비스가 재시작되었습니다."
    sleep 10
    check_health
}

stop_services() {
    echo "⏹️ 서비스 중지 중..."
    ssh -i ${KEY_NAME}.pem -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP \
        "cd /home/ubuntu/yousum && docker-compose -f docker-compose.production.yml down"
    echo "✅ 서비스가 중지되었습니다."
}

start_services() {
    echo "▶️ 서비스 시작 중..."
    ssh -i ${KEY_NAME}.pem -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP \
        "cd /home/ubuntu/yousum && docker-compose -f docker-compose.production.yml up -d"
    echo "✅ 서비스가 시작되었습니다."
    sleep 15
    check_health
}

check_health() {
    echo "🔍 헬스 체크 수행 중..."
    
    echo "Spring Boot API 헬스체크:"
    curl -s "http://$PUBLIC_IP:8080/actuator/health" | jq '.' || echo "❌ API 응답 없음"
    
    echo ""
    echo "시스템 리소스 사용량:"
    ssh -i ${KEY_NAME}.pem -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP \
        "echo '메모리 사용량:' && free -h && echo '' && echo 'CPU 사용량:' && top -bn1 | grep 'Cpu(s)' && echo '' && echo '디스크 사용량:' && df -h /"
}

ssh_connect() {
    echo "🔗 EC2에 SSH 접속 중..."
    ssh -i ${KEY_NAME}.pem -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP
}

update_app() {
    echo "🔄 애플리케이션 업데이트 중..."
    echo "현재 디렉토리에서 최신 코드를 EC2로 배포합니다."
    read -p "계속하시겠습니까? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        ./deploy/deploy-app.sh
    else
        echo "업데이트가 취소되었습니다."
    fi
}

backup_database() {
    echo "💾 데이터베이스 백업 중..."
    BACKUP_FILE="yousum_backup_$(date +%Y%m%d_%H%M%S).sql"
    
    ssh -i ${KEY_NAME}.pem -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP << EOF
docker exec yousum-backend mysqldump -h $RDS_ENDPOINT -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME > /home/ubuntu/$BACKUP_FILE
echo "백업 파일 생성: /home/ubuntu/$BACKUP_FILE"
EOF

    echo "📦 백업 파일을 로컬로 복사 중..."
    scp -i ${KEY_NAME}.pem ubuntu@$PUBLIC_IP:/home/ubuntu/$BACKUP_FILE ./backups/
    echo "✅ 백업 완료: ./backups/$BACKUP_FILE"
}

clean_docker() {
    echo "🧹 Docker 이미지 및 볼륨 정리 중..."
    ssh -i ${KEY_NAME}.pem -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP \
        "docker system prune -af && docker volume prune -f"
    echo "✅ Docker 정리 완료"
}

# 백업 디렉토리 생성
mkdir -p ./backups

# 명령어 처리
case "${1:-help}" in
    "status")
        check_status
        ;;
    "logs")
        show_logs
        ;;
    "restart")
        restart_services
        ;;
    "stop")
        stop_services
        ;;
    "start")
        start_services
        ;;
    "health")
        check_health
        ;;
    "ssh")
        ssh_connect
        ;;
    "update")
        update_app
        ;;
    "backup")
        backup_database
        ;;
    "clean")
        clean_docker
        ;;
    "help"|*)
        show_help
        ;;
esac 