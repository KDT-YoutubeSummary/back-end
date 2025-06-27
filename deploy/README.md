# YouSum AWS 배포 가이드

YouSum 프로젝트를 AWS에 배포하는 전체 과정을 안내합니다.

## 📋 사전 준비사항

### 1. 필수 도구 설치
- **AWS CLI**: AWS 리소스 관리
- **Docker**: 컨테이너화된 애플리케이션 실행
- **SSH 클라이언트**: EC2 접속

### 2. AWS 계정 설정
- AWS 계정 생성 및 결제 정보 등록
- IAM 사용자 생성 (EC2, RDS, S3 권한 필요)
- AWS CLI 자격 증명 설정

### 3. API 키 준비
- **OpenAI API Key**: GPT 및 Whisper 사용
- **Google OAuth 자격 증명**: 소셜 로그인
- **YouTube Data API Key**: 영상 메타데이터 조회

## 🚀 배포 단계별 가이드

### 1단계: AWS 인프라 설정
```bash
# 스크립트 실행 권한 부여
chmod +x deploy/*.sh

# AWS 인프라 생성 (VPC, 서브넷, 보안 그룹, S3)
./deploy/aws-setup.sh
```

**생성되는 리소스:**
- VPC 및 서브넷 (Public, Private, DB)
- 인터넷 게이트웨이 및 라우팅 테이블
- 보안 그룹 (EC2, RDS)
- S3 버킷 (파일 저장소)

### 2단계: EC2 인스턴스 생성
```bash
# EC2 인스턴스 생성 및 Docker 설치
./deploy/create-ec2.sh
```

**EC2 설정:**
- 인스턴스 타입: t3.medium (4GB RAM, 2 vCPU)
- 운영체제: Ubuntu 22.04 LTS
- 자동 설치: Docker, Docker Compose, Git, AWS CLI

### 3단계: RDS 데이터베이스 생성
```bash
# MySQL RDS 인스턴스 생성
./deploy/create-rds.sh
```

**RDS 설정:**
- 엔진: MySQL 8.0.35
- 인스턴스 클래스: db.t3.micro (프리티어)
- 스토리지: 20GB (암호화 활성화)

### 4단계: 환경변수 설정
```bash
# .env.production 파일 편집
vi .env.production
```

**필수 환경변수 추가:**
```env
# OpenAI
OPENAI_API_KEY=sk-your-openai-api-key

# Google OAuth
GOOGLE_OAUTH_CLIENT_ID=your-client-id.googleusercontent.com
GOOGLE_OAUTH_CLIENT_SECRET=your-client-secret

# YouTube API
YOUTUBE_API_KEY=your-youtube-api-key

# AWS 자격 증명
AWS_ACCESS_KEY_ID=your-access-key-id
AWS_SECRET_ACCESS_KEY=your-secret-access-key

# Email (Gmail SMTP)
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
```

### 5단계: 애플리케이션 배포
```bash
# 애플리케이션 빌드 및 배포
./deploy/deploy-app.sh
```

**배포 과정:**
1. 프로젝트 파일을 EC2로 전송
2. Docker 이미지 빌드
3. 컨테이너 실행
4. 헬스 체크 수행

## 🔧 운영 관리

### 모니터링 도구 사용
```bash
# 서비스 상태 확인
./deploy/monitoring.sh status

# 실시간 로그 확인
./deploy/monitoring.sh logs

# 서비스 재시작
./deploy/monitoring.sh restart

# 헬스 체크
./deploy/monitoring.sh health

# SSH 접속
./deploy/monitoring.sh ssh
```

### 주요 엔드포인트
- **Spring Boot API**: `http://EC2_IP:8080`
- **API 문서**: `http://EC2_IP:8080/swagger-ui.html`
- **Health Check**: `http://EC2_IP:8080/actuator/health`
- **Python Whisper**: `http://EC2_IP:8000`

## 📁 파일 구조

```
back-end/
├── deploy/                    # 배포 스크립트
│   ├── aws-setup.sh          # AWS 인프라 설정
│   ├── create-ec2.sh         # EC2 인스턴스 생성
│   ├── create-rds.sh         # RDS 데이터베이스 생성
│   ├── deploy-app.sh         # 애플리케이션 배포
│   ├── monitoring.sh         # 운영 모니터링
│   └── README.md             # 배포 가이드
├── Dockerfile                # Docker 이미지 정의
├── docker-compose.yml        # 로컬 개발용
├── start.sh                  # 컨테이너 시작 스크립트
├── init.sql                  # 데이터베이스 초기화
├── .env.example              # 환경변수 템플릿
└── .env.production           # 운영 환경변수 (생성됨)
```

## 🔒 보안 설정

### 네트워크 보안
- VPC 내 프라이빗 서브넷에 RDS 배치
- 보안 그룹으로 포트 접근 제한
- HTTPS 적용 권장 (Let's Encrypt 등)

### 데이터베이스 보안
- RDS 암호화 활성화
- 복잡한 마스터 패스워드 사용
- 정기적인 자동 백업

### 애플리케이션 보안
- JWT 토큰 기반 인증
- 환경변수로 민감 정보 관리
- API 키 순환 (정기적 갱신)

## 📊 모니터링 및 로깅

### CloudWatch 설정
```bash
# CloudWatch 에이전트 설치 (선택사항)
ssh -i key.pem ubuntu@EC2_IP
sudo apt install amazon-cloudwatch-agent
```

### 로그 확인
```bash
# 애플리케이션 로그
docker-compose logs -f yousum-backend

# 시스템 로그
sudo journalctl -f
```

## 🔄 업데이트 및 배포

### 코드 업데이트
```bash
# 최신 코드 배포
./deploy/monitoring.sh update
```

### 데이터베이스 백업
```bash
# 백업 생성
./deploy/monitoring.sh backup
```

### 롤백 절차
1. 이전 Docker 이미지로 롤백
2. 데이터베이스 백업에서 복원
3. 설정 파일 복원

## 🆘 문제 해결

### 자주 발생하는 문제

#### 1. 애플리케이션 시작 실패
```bash
# 로그 확인
./deploy/monitoring.sh logs

# 컨테이너 상태 확인
docker ps -a

# 환경변수 확인
docker exec yousum-backend env | grep -E '(DB|API)'
```

#### 2. 데이터베이스 연결 실패
- RDS 보안 그룹 설정 확인
- 데이터베이스 엔드포인트 및 자격 증명 확인
- VPC 내 네트워크 연결 상태 확인

#### 3. API 키 오류
- 환경변수 설정 확인
- API 키 유효성 및 권한 확인
- 사용량 제한 확인

### 긴급 복구
```bash
# 서비스 완전 재시작
./deploy/monitoring.sh stop
./deploy/monitoring.sh start

# Docker 이미지 재빌드
./deploy/monitoring.sh clean
./deploy/deploy-app.sh
```

## 💰 비용 최적화

### AWS 프리티어 활용
- EC2 t3.micro (월 750시간)
- RDS db.t3.micro (월 750시간)
- S3 5GB 저장소

### 비용 절약 팁
- 개발 환경은 필요시에만 실행
- CloudWatch 로그 보존 기간 설정
- 사용하지 않는 리소스 정리

## 📞 지원 및 문의

배포 과정에서 문제가 발생하면:

1. **로그 확인**: `./deploy/monitoring.sh logs`
2. **상태 점검**: `./deploy/monitoring.sh status`
3. **이슈 리포트**: GitHub Issues에 상세 정보 포함하여 보고

---

> **주의**: 운영 환경에서는 정기적인 백업과 모니터링이 필수입니다.
> 중요한 API 키는 절대 코드에 하드코딩하지 마세요. 