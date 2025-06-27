#!/bin/bash

# YouSum AWS 인프라 설정 스크립트
set -e

echo "🚀 YouSum AWS 인프라 설정을 시작합니다..."

# 변수 설정
REGION="ap-northeast-2"
PROJECT_NAME="yousum"
VPC_CIDR="10.0.0.0/16"
PUBLIC_SUBNET_CIDR="10.0.1.0/24"
PRIVATE_SUBNET_CIDR="10.0.2.0/24"
DB_SUBNET_CIDR="10.0.3.0/24"

# AWS CLI 설치 확인
if ! command -v aws &> /dev/null; then
    echo "❌ AWS CLI가 설치되어 있지 않습니다. 설치해주세요."
    echo "설치 명령: curl 'https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip' -o 'awscliv2.zip' && unzip awscliv2.zip && sudo ./aws/install"
    exit 1
fi

echo "✅ AWS CLI 설치 확인됨"

# AWS 계정 정보 확인
echo "📋 AWS 계정 정보 확인 중..."
aws sts get-caller-identity

# 1. VPC 생성
echo "🌐 VPC 생성 중..."
VPC_ID=$(aws ec2 create-vpc \
    --cidr-block $VPC_CIDR \
    --tag-specifications "ResourceType=vpc,Tags=[{Key=Name,Value=${PROJECT_NAME}-vpc}]" \
    --region $REGION \
    --output text --query 'Vpc.VpcId')

echo "VPC 생성됨: $VPC_ID"

# 2. 인터넷 게이트웨이 생성 및 연결
echo "🌍 인터넷 게이트웨이 생성 중..."
IGW_ID=$(aws ec2 create-internet-gateway \
    --tag-specifications "ResourceType=internet-gateway,Tags=[{Key=Name,Value=${PROJECT_NAME}-igw}]" \
    --region $REGION \
    --output text --query 'InternetGateway.InternetGatewayId')

aws ec2 attach-internet-gateway \
    --internet-gateway-id $IGW_ID \
    --vpc-id $VPC_ID \
    --region $REGION

echo "인터넷 게이트웨이 생성 및 연결됨: $IGW_ID"

# 3. 퍼블릭 서브넷 생성
echo "🏗️ 퍼블릭 서브넷 생성 중..."
PUBLIC_SUBNET_ID=$(aws ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block $PUBLIC_SUBNET_CIDR \
    --availability-zone "${REGION}a" \
    --tag-specifications "ResourceType=subnet,Tags=[{Key=Name,Value=${PROJECT_NAME}-public-subnet}]" \
    --region $REGION \
    --output text --query 'Subnet.SubnetId')

echo "퍼블릭 서브넷 생성됨: $PUBLIC_SUBNET_ID"

# 4. 프라이빗 서브넷 생성 (RDS용)
echo "🔒 프라이빗 서브넷 생성 중..."
PRIVATE_SUBNET_ID=$(aws ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block $PRIVATE_SUBNET_CIDR \
    --availability-zone "${REGION}b" \
    --tag-specifications "ResourceType=subnet,Tags=[{Key=Name,Value=${PROJECT_NAME}-private-subnet}]" \
    --region $REGION \
    --output text --query 'Subnet.SubnetId')

echo "프라이빗 서브넷 생성됨: $PRIVATE_SUBNET_ID"

# 5. DB 서브넷 생성
echo "💾 DB 서브넷 생성 중..."
DB_SUBNET_ID=$(aws ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block $DB_SUBNET_CIDR \
    --availability-zone "${REGION}c" \
    --tag-specifications "ResourceType=subnet,Tags=[{Key=Name,Value=${PROJECT_NAME}-db-subnet}]" \
    --region $REGION \
    --output text --query 'Subnet.SubnetId')

echo "DB 서브넷 생성됨: $DB_SUBNET_ID"

# 6. 라우팅 테이블 생성 및 설정
echo "🛣️ 라우팅 테이블 설정 중..."
ROUTE_TABLE_ID=$(aws ec2 create-route-table \
    --vpc-id $VPC_ID \
    --tag-specifications "ResourceType=route-table,Tags=[{Key=Name,Value=${PROJECT_NAME}-public-rt}]" \
    --region $REGION \
    --output text --query 'RouteTable.RouteTableId')

aws ec2 create-route \
    --route-table-id $ROUTE_TABLE_ID \
    --destination-cidr-block 0.0.0.0/0 \
    --gateway-id $IGW_ID \
    --region $REGION

aws ec2 associate-route-table \
    --subnet-id $PUBLIC_SUBNET_ID \
    --route-table-id $ROUTE_TABLE_ID \
    --region $REGION

echo "라우팅 테이블 설정 완료: $ROUTE_TABLE_ID"

# 7. 보안 그룹 생성
echo "🔐 보안 그룹 생성 중..."

# EC2용 보안 그룹
EC2_SG_ID=$(aws ec2 create-security-group \
    --group-name "${PROJECT_NAME}-ec2-sg" \
    --description "YouSum EC2 Security Group" \
    --vpc-id $VPC_ID \
    --region $REGION \
    --output text --query 'GroupId')

# EC2 보안 그룹 규칙 추가
aws ec2 authorize-security-group-ingress \
    --group-id $EC2_SG_ID \
    --protocol tcp --port 22 --cidr 0.0.0.0/0 \
    --region $REGION

aws ec2 authorize-security-group-ingress \
    --group-id $EC2_SG_ID \
    --protocol tcp --port 80 --cidr 0.0.0.0/0 \
    --region $REGION

aws ec2 authorize-security-group-ingress \
    --group-id $EC2_SG_ID \
    --protocol tcp --port 443 --cidr 0.0.0.0/0 \
    --region $REGION

aws ec2 authorize-security-group-ingress \
    --group-id $EC2_SG_ID \
    --protocol tcp --port 8080 --cidr 0.0.0.0/0 \
    --region $REGION

echo "EC2 보안 그룹 생성됨: $EC2_SG_ID"

# RDS용 보안 그룹
RDS_SG_ID=$(aws ec2 create-security-group \
    --group-name "${PROJECT_NAME}-rds-sg" \
    --description "YouSum RDS Security Group" \
    --vpc-id $VPC_ID \
    --region $REGION \
    --output text --query 'GroupId')

aws ec2 authorize-security-group-ingress \
    --group-id $RDS_SG_ID \
    --protocol tcp --port 3306 \
    --source-group $EC2_SG_ID \
    --region $REGION

echo "RDS 보안 그룹 생성됨: $RDS_SG_ID"

# 8. S3 버킷 생성
echo "📦 S3 버킷 생성 중..."
S3_BUCKET_NAME="${PROJECT_NAME}-s3-$(date +%s)"

aws s3api create-bucket \
    --bucket $S3_BUCKET_NAME \
    --region $REGION \
    --create-bucket-configuration LocationConstraint=$REGION

# S3 버킷 정책 설정
cat > s3-policy.json << EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "YouSumAppAccess",
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::$(aws sts get-caller-identity --output text --query Account):root"
            },
            "Action": [
                "s3:GetObject",
                "s3:PutObject",
                "s3:DeleteObject"
            ],
            "Resource": "arn:aws:s3:::$S3_BUCKET_NAME/*"
        }
    ]
}
EOF

aws s3api put-bucket-policy \
    --bucket $S3_BUCKET_NAME \
    --policy file://s3-policy.json

echo "S3 버킷 생성됨: $S3_BUCKET_NAME"

# 9. RDS 서브넷 그룹 생성
echo "🗄️ RDS 서브넷 그룹 생성 중..."
aws rds create-db-subnet-group \
    --db-subnet-group-name "${PROJECT_NAME}-db-subnet-group" \
    --db-subnet-group-description "YouSum DB Subnet Group" \
    --subnet-ids $PRIVATE_SUBNET_ID $DB_SUBNET_ID \
    --region $REGION

echo "RDS 서브넷 그룹 생성됨: ${PROJECT_NAME}-db-subnet-group"

# 설정 정보 저장
cat > aws-resources.txt << EOF
# YouSum AWS 리소스 정보
# 이 정보를 .env 파일에 복사하여 사용하세요

VPC_ID=$VPC_ID
PUBLIC_SUBNET_ID=$PUBLIC_SUBNET_ID
PRIVATE_SUBNET_ID=$PRIVATE_SUBNET_ID
DB_SUBNET_ID=$DB_SUBNET_ID
EC2_SG_ID=$EC2_SG_ID
RDS_SG_ID=$RDS_SG_ID
S3_BUCKET_NAME=$S3_BUCKET_NAME
REGION=$REGION

# 환경변수 설정용
export AWS_S3_BUCKET_NAME=$S3_BUCKET_NAME
export AWS_REGION=$REGION
EOF

echo "✅ AWS 인프라 설정 완료!"
echo "📄 리소스 정보가 aws-resources.txt 파일에 저장되었습니다."
echo ""
echo "다음 단계:"
echo "1. EC2 인스턴스 생성: ./deploy/create-ec2.sh"
echo "2. RDS 데이터베이스 생성: ./deploy/create-rds.sh"
echo "3. 애플리케이션 배포: ./deploy/deploy-app.sh" 