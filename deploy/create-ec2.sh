#!/bin/bash

# YouSum EC2 인스턴스 생성 스크립트
set -e

echo "🖥️ YouSum EC2 인스턴스 생성을 시작합니다..."

# 변수 설정
REGION="ap-northeast-2"
PROJECT_NAME="yousum"
INSTANCE_TYPE="t3.medium"  # 메모리 4GB, vCPU 2개
KEY_NAME="${PROJECT_NAME}-key"
AMI_ID="ami-0c9c942bd7bf113a2"  # Ubuntu 22.04 LTS (ap-northeast-2)

# aws-resources.txt에서 리소스 ID 로드
if [ ! -f "aws-resources.txt" ]; then
    echo "❌ aws-resources.txt 파일이 없습니다. 먼저 aws-setup.sh를 실행하세요."
    exit 1
fi

source aws-resources.txt

# 1. Key Pair 생성
echo "🔑 EC2 Key Pair 생성 중..."
if ! aws ec2 describe-key-pairs --key-names $KEY_NAME --region $REGION &>/dev/null; then
    aws ec2 create-key-pair \
        --key-name $KEY_NAME \
        --region $REGION \
        --output text --query 'KeyMaterial' > ${KEY_NAME}.pem
    
    chmod 400 ${KEY_NAME}.pem
    echo "Key Pair 생성됨: ${KEY_NAME}.pem"
else
    echo "Key Pair가 이미 존재합니다: $KEY_NAME"
fi

# 2. EC2 사용자 데이터 스크립트 생성
cat > user-data.sh << 'EOF'
#!/bin/bash

# 시스템 업데이트
apt-get update
apt-get upgrade -y

# Docker 설치
apt-get install -y apt-transport-https ca-certificates curl gnupg lsb-release
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io

# Docker Compose 설치
curl -L "https://github.com/docker/compose/releases/download/v2.20.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Ubuntu 사용자를 docker 그룹에 추가
usermod -aG docker ubuntu

# Git 설치
apt-get install -y git

# 방화벽 설정
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 8080/tcp
ufw allow 8000/tcp
ufw --force enable

# 필요한 디렉토리 생성
mkdir -p /home/ubuntu/yousum
mkdir -p /home/ubuntu/yousum/logs
chown -R ubuntu:ubuntu /home/ubuntu/yousum

# Docker 서비스 시작
systemctl enable docker
systemctl start docker

# AWS CLI 설치
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
./aws/install

# 시스템 정보 로그
echo "EC2 초기화 완료 - $(date)" >> /home/ubuntu/setup.log
docker --version >> /home/ubuntu/setup.log
docker-compose --version >> /home/ubuntu/setup.log
aws --version >> /home/ubuntu/setup.log
EOF

# 3. EC2 인스턴스 생성
echo "🚀 EC2 인스턴스 생성 중..."
INSTANCE_ID=$(aws ec2 run-instances \
    --image-id $AMI_ID \
    --count 1 \
    --instance-type $INSTANCE_TYPE \
    --key-name $KEY_NAME \
    --security-group-ids $EC2_SG_ID \
    --subnet-id $PUBLIC_SUBNET_ID \
    --associate-public-ip-address \
    --user-data file://user-data.sh \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=${PROJECT_NAME}-server}]" \
    --region $REGION \
    --output text --query 'Instances[0].InstanceId')

echo "EC2 인스턴스 생성 중: $INSTANCE_ID"

# 4. 인스턴스 상태 확인
echo "⏳ 인스턴스가 시작되기를 기다리는 중..."
aws ec2 wait instance-running --instance-ids $INSTANCE_ID --region $REGION

# 5. 퍼블릭 IP 주소 조회
PUBLIC_IP=$(aws ec2 describe-instances \
    --instance-ids $INSTANCE_ID \
    --region $REGION \
    --output text --query 'Reservations[0].Instances[0].PublicIpAddress')

echo "✅ EC2 인스턴스가 성공적으로 생성되었습니다!"
echo ""
echo "📋 인스턴스 정보:"
echo "Instance ID: $INSTANCE_ID"
echo "Public IP: $PUBLIC_IP"
echo "Key File: ${KEY_NAME}.pem"
echo ""
echo "🔗 SSH 연결 명령어:"
echo "ssh -i ${KEY_NAME}.pem ubuntu@$PUBLIC_IP"
echo ""

# 인스턴스 정보를 파일에 저장
cat >> aws-resources.txt << EOF

# EC2 정보
INSTANCE_ID=$INSTANCE_ID
PUBLIC_IP=$PUBLIC_IP
KEY_NAME=$KEY_NAME
EOF

echo "📄 인스턴스 정보가 aws-resources.txt 파일에 추가되었습니다."
echo ""
echo "다음 단계:"
echo "1. 약 2-3분 후 SSH로 접속하여 Docker 설치 완료 확인"
echo "2. RDS 데이터베이스 생성: ./deploy/create-rds.sh"
echo "3. 애플리케이션 배포: ./deploy/deploy-app.sh" 