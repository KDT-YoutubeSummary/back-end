#!/bin/bash

# YouSum RDS MySQL 데이터베이스 생성 스크립트
set -e

echo "🗄️ YouSum RDS MySQL 데이터베이스 생성을 시작합니다..."

# 변수 설정
REGION="ap-northeast-2"
PROJECT_NAME="yousum"
DB_INSTANCE_ID="${PROJECT_NAME}-db"
DB_NAME="yousum"
DB_USERNAME="admin"
DB_PASSWORD="YouSum123!SecurePassword"
DB_INSTANCE_CLASS="db.t3.micro"  # 프리티어
DB_ALLOCATED_STORAGE=20
DB_ENGINE="mysql"
DB_ENGINE_VERSION="8.0.35"

# aws-resources.txt에서 리소스 ID 로드
if [ ! -f "aws-resources.txt" ]; then
    echo "❌ aws-resources.txt 파일이 없습니다. 먼저 aws-setup.sh를 실행하세요."
    exit 1
fi

source aws-resources.txt

# 1. RDS 인스턴스 생성
echo "🔧 RDS MySQL 인스턴스 생성 중..."
aws rds create-db-instance \
    --db-instance-identifier $DB_INSTANCE_ID \
    --db-instance-class $DB_INSTANCE_CLASS \
    --engine $DB_ENGINE \
    --engine-version $DB_ENGINE_VERSION \
    --master-username $DB_USERNAME \
    --master-user-password $DB_PASSWORD \
    --allocated-storage $DB_ALLOCATED_STORAGE \
    --storage-type gp2 \
    --db-name $DB_NAME \
    --vpc-security-group-ids $RDS_SG_ID \
    --db-subnet-group-name "${PROJECT_NAME}-db-subnet-group" \
    --backup-retention-period 7 \
    --multi-az false \
    --storage-encrypted true \
    --auto-minor-version-upgrade true \
    --publicly-accessible false \
    --copy-tags-to-snapshot true \
    --deletion-protection false \
    --enable-performance-insights false \
    --tags Key=Name,Value="${PROJECT_NAME}-database" Key=Environment,Value=production \
    --region $REGION

echo "RDS 인스턴스 생성 요청 완료: $DB_INSTANCE_ID"

# 2. RDS 인스턴스 생성 완료 대기
echo "⏳ RDS 인스턴스가 사용 가능해질 때까지 기다리는 중... (약 5-10분 소요)"
aws rds wait db-instance-available --db-instance-identifier $DB_INSTANCE_ID --region $REGION

# 3. RDS 엔드포인트 조회
RDS_ENDPOINT=$(aws rds describe-db-instances \
    --db-instance-identifier $DB_INSTANCE_ID \
    --region $REGION \
    --output text --query 'DBInstances[0].Endpoint.Address')

RDS_PORT=$(aws rds describe-db-instances \
    --db-instance-identifier $DB_INSTANCE_ID \
    --region $REGION \
    --output text --query 'DBInstances[0].Endpoint.Port')

echo "✅ RDS MySQL 데이터베이스가 성공적으로 생성되었습니다!"
echo ""
echo "📋 데이터베이스 정보:"
echo "DB Instance ID: $DB_INSTANCE_ID"
echo "Endpoint: $RDS_ENDPOINT"
echo "Port: $RDS_PORT"
echo "Database: $DB_NAME"
echo "Username: $DB_USERNAME"
echo "Password: $DB_PASSWORD"
echo ""

# 4. 데이터베이스 정보를 파일에 저장
cat >> aws-resources.txt << EOF

# RDS 정보
DB_INSTANCE_ID=$DB_INSTANCE_ID
RDS_ENDPOINT=$RDS_ENDPOINT
RDS_PORT=$RDS_PORT
DB_NAME=$DB_NAME
DB_USERNAME=$DB_USERNAME
DB_PASSWORD=$DB_PASSWORD
EOF

# 5. 환경변수 파일 생성
cat > .env.production << EOF
# YouSum Production 환경변수
# AWS RDS Database
RDS_ENDPOINT=$RDS_ENDPOINT
RDS_USERNAME=$DB_USERNAME
RDS_PASSWORD=$DB_PASSWORD
RDS_DATABASE=$DB_NAME

# AWS S3
AWS_S3_BUCKET_NAME=$S3_BUCKET_NAME
AWS_REGION=$REGION

# 다른 환경변수들은 .env.example을 참고하여 추가하세요
EOF

echo "📄 데이터베이스 정보가 aws-resources.txt 파일에 추가되었습니다."
echo "📄 .env.production 파일이 생성되었습니다."
echo ""
echo "🔧 추가 설정 필요:"
echo "1. .env.production 파일에 다른 API 키들을 추가하세요:"
echo "   - OPENAI_API_KEY"
echo "   - GOOGLE_OAUTH_CLIENT_ID"
echo "   - GOOGLE_OAUTH_CLIENT_SECRET"
echo "   - YOUTUBE_API_KEY"
echo "   - AWS_ACCESS_KEY_ID"
echo "   - AWS_SECRET_ACCESS_KEY"
echo ""
echo "다음 단계:"
echo "1. 환경변수 설정 완료 후 애플리케이션 배포: ./deploy/deploy-app.sh"
echo "2. 데이터베이스 초기화: ./deploy/init-database.sh" 