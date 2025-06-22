# YouSum (유썸) - AI 기반 유튜브 영상 요약 및 학습 지원 플랫폼

![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-005C84?style=for-the-badge&logo=mysql&logoColor=white)
![OpenAI](https://img.shields.io/badge/OpenAI-412991?style=for-the-badge&logo=openai&logoColor=white)
![AWS](https://img.shields.io/badge/Amazon_AWS-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2CA5E0?style=for-the-badge&logo=docker&logoColor=white)

## 📌 프로젝트 소개 (Overview)

**YouSum**은 능동적 학습자를 위한 AI 기반 유튜브 영상 요약 및 학습 지원 플랫폼입니다. 
OpenAI의 Whisper와 GPT API를 활용하여 유튜브 영상을 자동으로 요약하고, 퀴즈 생성, 리마인드 기능, 개인화된 학습 지원을 제공합니다.

### 🎯 핵심 목적
- **효율적 학습**: 긴 유튜브 영상을 빠르게 요약하여 핵심 내용 파악
- **능동적 학습**: AI 생성 퀴즈를 통한 학습 내용 점검 및 복습
- **개인화 서비스**: 사용자 맞춤형 영상 추천 및 학습 관리
- **지속적 학습**: 리마인드 알림을 통한 체계적인 복습 지원

## ✨ 핵심 기능 (Key Features)

### 🎬 영상 처리 및 요약
- **Whisper STT**: 유튜브 영상 오디오를 텍스트로 변환
- **다양한 요약 형식**: 기본 요약, 3줄 요약, 키워드 요약, 타임라인 요약
- **목적별 맞춤 요약**: 사용자가 지정한 목적에 따른 개인화된 요약 생성
- **자동 해시태그 생성**: AI 기반 콘텐츠 분류 및 태그 자동 생성

### 📚 개인 학습 관리
- **사용자 라이브러리**: 요약된 콘텐츠 저장 및 개인 메모 추가
- **태그 기반 검색**: 해시태그를 활용한 효율적인 콘텐츠 검색
- **학습 통계 시각화**: 태그별 학습 현황 및 활동 로그 분석

### 🧠 AI 기반 학습 지원
- **자동 퀴즈 생성**: 요약 내용 기반 OX/객관식 문제 자동 출제
- **즉시 피드백**: 정답 확인 및 상세 해설 제공
- **개인화 추천**: 학습 이력 기반 관련 영상 추천

### 🔔 학습 지속성 지원
- **리마인드 알림**: 이메일 기반 복습 알림 (일간/주간/커스텀)
- **학습 기록 추적**: 사용자 활동 로그 및 학습 패턴 분석

### 🔐 사용자 인증 및 보안
- **JWT 기반 인증**: 안전한 토큰 기반 사용자 인증
- **OAuth2 구글 로그인**: 간편한 소셜 로그인 지원
- **비밀번호 암호화**: BCrypt를 활용한 안전한 비밀번호 저장

## 🛠 기술 스택 (Tech Stack)

### Backend
- **Framework**: Spring Boot 3.x
- **Security**: Spring Security (JWT + OAuth2)
- **Database**: MySQL 8.0, Redis
- **ORM**: JPA, QueryDSL
- **AI Services**: OpenAI Whisper API, GPT API
- **External API**: YouTube Data API v3

### Infrastructure
- **Cloud**: AWS (EC2, S3, RDS, Lambda)
- **Containerization**: Docker
- **Build Tool**: Gradle
- **Monitoring**: Spring Boot Actuator, AWS CloudWatch

### Development Tools
- **Testing**: JUnit 5, Mockito, TestContainers
- **Code Quality**: SonarQube
- **Documentation**: Swagger/OpenAPI

## 🏗 시스템 아키텍처 (Architecture)

### 전체 시스템 구조
```
┌─────────────────┐    API 호출    ┌──────────────────────┐
│   React Web    │ ──────────────► │  Spring Boot API     │
│   Frontend     │                 │      Server          │
└─────────────────┘                 └──────────────────────┘
                                              │
                    ┌─────────────────────────┼─────────────────────────┐
                    │                         │                         │
                    ▼                         ▼                         ▼
        ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
        │   OpenAI APIs    │    │   YouTube API    │    │   MySQL + Redis  │
        │ • Whisper (STT)  │    │ • 영상 메타데이터   │    │ • 사용자 데이터    │
        │ • GPT (요약/퀴즈) │    │ • 썸네일, 제목     │    │ • 요약/퀴즈 데이터 │
        └──────────────────┘    └──────────────────┘    └──────────────────┘
                    │
                    ▼
        ┌──────────────────────────────────────────────────────────────────┐
        │                      AWS Infrastructure                          │
        │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐           │
        │  │   EC2   │  │   S3    │  │   RDS   │  │ Lambda  │           │
        │  │서버호스팅 │  │파일저장  │  │ MySQL  │  │자동화   │           │
        │  └─────────┘  └─────────┘  └─────────┘  └─────────┘           │
        └──────────────────────────────────────────────────────────────────┘
```

### 주요 기능별 시퀀스 다이어그램

#### 1. 영상 요약 생성 플로우
```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend<br/>(React)
    participant A as API Server<br/>(Spring Boot)
    participant Y as YouTube API
    participant O as OpenAI API
    participant D as Database<br/>(MySQL)
    participant W as Whisper<br/>(Python)

    Note over U,W: 영상 요약 생성 플로우

    U->>F: 유튜브 URL 입력
    F->>A: POST /api/youtube/upload
    A->>Y: 영상 메타데이터 조회
    Y-->>A: 영상 정보 반환
    A->>D: 영상 정보 저장
    A->>W: 오디오 추출 및 STT 요청
    W->>O: Whisper API 호출
    O-->>W: 텍스트 변환 결과
    W-->>A: STT 텍스트 반환
    A->>D: 원본 텍스트 저장
    A->>A: 텍스트 정제 (TextCleaner)
    A->>O: GPT API 요약 요청
    O-->>A: 요약 결과 반환
    A->>D: 요약 데이터 저장
    A->>O: 해시태그 생성 요청
    O-->>A: 해시태그 반환
    A->>D: 해시태그 저장
    A-->>F: 요약 완료 응답
    F-->>U: 요약 결과 표시
```

#### 2. 퀴즈 생성 및 풀이 플로우
```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend<br/>(React)
    participant A as API Server<br/>(Spring Boot)
    participant O as OpenAI API
    participant D as Database<br/>(MySQL)

    Note over U,D: 퀴즈 생성 및 풀이 플로우

    U->>F: 퀴즈 생성 요청
    F->>A: POST /api/quizzes
    A->>D: 요약 데이터 조회
    D-->>A: 요약 텍스트 반환
    A->>O: GPT API 퀴즈 생성 요청
    Note right of O: 요약 기반<br/>OX/객관식 문제 생성
    O-->>A: 퀴즈 문제 반환
    A->>D: 퀴즈 데이터 저장
    A-->>F: 퀴즈 문제 응답
    F-->>U: 퀴즈 문제 표시
    
    U->>F: 답안 제출
    F->>A: POST /api/quizzes/{id}/submit
    A->>D: 정답 확인
    A->>O: 해설 생성 요청 (선택적)
    O-->>A: 상세 해설 반환
    A->>D: 사용자 답안 기록 저장
    A-->>F: 채점 결과 및 해설
    F-->>U: 결과 및 피드백 표시
```

#### 3. 사용자 인증 플로우
```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend<br/>(React)
    participant A as API Server<br/>(Spring Boot)
    participant G as Google OAuth
    participant D as Database<br/>(MySQL)
    participant J as JWT Provider

    Note over U,J: 사용자 인증 플로우

    rect rgb(240, 248, 255)
        Note over U,J: 구글 소셜 로그인
        U->>F: 구글 로그인 클릭
        F->>G: OAuth2 인증 요청
        G-->>U: 구글 로그인 페이지
        U->>G: 구글 계정 인증
        G-->>F: Authorization Code
        F->>A: POST /api/auth/google-login
        A->>G: Access Token 요청
        G-->>A: 사용자 정보 반환
        A->>D: 사용자 정보 저장/조회
        A->>J: JWT 토큰 생성
        J-->>A: JWT 토큰 반환
        A-->>F: 로그인 성공 + JWT
        F-->>U: 메인 페이지 이동
    end
    
    rect rgb(255, 248, 240)
        Note over U,J: 일반 회원가입/로그인
        U->>F: 회원가입 정보 입력
        F->>A: POST /api/auth/register
        A->>A: 입력값 검증
        A->>D: 중복 사용자 확인
        A->>A: 비밀번호 암호화 (BCrypt)
        A->>D: 사용자 정보 저장
        A-->>F: 회원가입 완료
        F-->>U: 로그인 페이지 이동
    end
```

#### 4. 라이브러리 관리 및 리마인드 플로우
```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend<br/>(React)
    participant A as API Server<br/>(Spring Boot)
    participant O as OpenAI API
    participant D as Database<br/>(MySQL)
    participant E as Email Service<br/>(SMTP)

    Note over U,E: 라이브러리 관리 및 리마인드 플로우

    rect rgb(240, 255, 240)
        Note over U,D: 라이브러리 저장
        U->>F: 요약 저장 + 개인 메모
        F->>A: POST /api/libraries
        A->>D: 사용자 라이브러리 저장
        A->>D: 태그 기반 자동 분류
        A-->>F: 저장 완료 응답
        F-->>U: 저장 성공 알림
    end

    rect rgb(255, 240, 255)
        Note over U,E: 리마인드 설정 및 알림
        U->>F: 리마인드 설정 (일간/주간)
        F->>A: POST /api/reminders
        A->>D: 리마인드 스케줄 저장
        A-->>F: 설정 완료 응답
        
        Note over A,E: 스케줄러에 의한 자동 실행
        A->>D: 리마인드 대상 조회
        A->>E: 이메일 발송 요청
        E-->>U: 복습 알림 이메일
    end

    rect rgb(240, 240, 255)
        Note over U,O: AI 기반 영상 추천
        U->>F: 추천 영상 요청
        F->>A: GET /api/recommendations/users/{id}
        A->>D: 사용자 학습 이력 조회
        A->>O: GPT API 추천 요청
        Note right of O: 해시태그 기반<br/>유사 콘텐츠 추천
        O-->>A: 추천 영상 목록
        A->>D: 추천 결과 저장
        A-->>F: 추천 영상 응답
        F-->>U: 개인화된 추천 목록
    end
```

## 📁 폴더 구조 (Project Structure)

```
src/main/java/com/kdt/yts/YouSumback/
├── 📁 config/                    # 설정 클래스
│   ├── AppConfig.java
│   ├── OpenAIConfig.java
│   └── WebConfig.java
├── 📁 controller/                # REST API 컨트롤러
│   ├── AuthController.java       # 인증 관련
│   ├── SummaryController.java    # 요약 기능
│   ├── QuizController.java       # 퀴즈 기능
│   ├── UserLibraryController.java # 사용자 라이브러리
│   ├── ReminderController.java   # 리마인드 기능
│   └── VideoRecommendationController.java # 추천 기능
├── 📁 model/
│   ├── 📁 entity/               # JPA 엔티티
│   │   ├── User.java
│   │   ├── Video.java
│   │   ├── Summary.java
│   │   ├── Quiz.java
│   │   └── UserLibrary.java
│   └── 📁 dto/                  # 데이터 전송 객체
│       ├── 📁 request/
│       └── 📁 response/
├── 📁 service/                  # 비즈니스 로직
│   ├── AuthService.java
│   ├── SummaryServiceImpl.java
│   ├── QuizServiceImpl.java
│   ├── UserLibraryService.java
│   └── 📁 client/              # 외부 API 클라이언트
│       ├── OpenAIClient.java
│       └── YouTubeClient.java
├── 📁 repository/              # 데이터 액세스 계층
├── 📁 security/               # 보안 설정
│   ├── SecurityConfig.java
│   ├── JwtProvider.java
│   └── OAuth2LoginSuccessHandler.java
├── 📁 exception/              # 예외 처리
│   ├── GlobalExceptionHandler.java
│   └── UserAlreadyExistsException.java
└── 📁 Util/                   # 유틸리티 클래스
    ├── TextCleaner.java
    └── WhisperRunner.java
```

## 🚀 설치 및 실행 방법 (Getting Started)

### 사전 요구사항
- Java 17 이상
- MySQL 8.0
- Redis
- Docker (Whisper 처리용)

### 1. 프로젝트 클론
```bash
git clone https://github.com/your-repo/yousum-backend.git
cd yousum-backend
```

### 2. 환경 변수 설정
```bash
# Windows PowerShell에서 실행
./set-env.ps1
```

또는 직접 환경 변수 설정:
```bash
export OPENAI_API_KEY=your_openai_api_key
export YOUTUBE_API_KEY=your_youtube_api_key
export GOOGLE_OAUTH_CLIENT_ID=your_google_client_id
export GOOGLE_OAUTH_CLIENT_SECRET=your_google_client_secret
```

### 3. 데이터베이스 설정
```sql
CREATE DATABASE yousum;
```

### 4. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 5. API 문서 확인
서버 실행 후 다음 URL에서 API 문서를 확인할 수 있습니다:
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## 🎮 요약 대기 미니게임 (Summary Typing Game)

AI가 영상을 요약하는 동안 사용자의 대기 시간을 줄이기 위한 **타이핑 게임**을 제공합니다.

### 게임 특징
- **교육적 콘텐츠**: 학습 관련 단어 및 문장으로 구성
- **실시간 피드백**: 타이핑 속도 및 정확도 측정
- **레벨 시스템**: 사용자 실력에 따른 난이도 조절
- **점수 시스템**: 게임 결과를 통한 성취감 제공

이 기능은 단순한 대기 시간을 유익한 학습 시간으로 전환하여 사용자 경험을 향상시킵니다.

## 👥 팀 소개 (Team YouSum)

| 역할 | 이름 | 담당 영역 | GitHub |
|------|------|-----------|---------|
| **팀 리더** | 석예은 | 프로젝트 총괄, Full-Stack 개발 | [@yeeun-suk](https://github.com/yeeun-suk) |
| **프론트엔드** | 김지원 | React UI/UX, AI 연동 | [@jiwon-kim](https://github.com/jiwon-kim) |
| **프론트엔드** | 정준호 | React 컴포넌트, API 연동 | [@joonho-jung](https://github.com/joonho-jung) |
| **백엔드** | 윤정수 | Spring Boot API, Whisper STT | [@jungsu-yoon](https://github.com/jungsu-yoon) |
| **백엔드** | 최도영 | 인프라 구축, AWS 배포 | [@doyoung-choi](https://github.com/doyoung-choi) |

### 🏆 팀 역량
- **풀스택 개발**: React + Spring Boot 기반 완전한 웹 애플리케이션 구축
- **AI 통합**: OpenAI API를 활용한 실용적인 AI 서비스 개발
- **클라우드 인프라**: AWS 기반 확장 가능한 서비스 아키텍처 구성
- **사용자 중심 설계**: 실제 학습자의 니즈를 반영한 기능 개발

## 📚 문서 링크 (Docs)

- **[소프트웨어 요구사항 명세서 (SRS)](./docs/SRS-YTS-001.md)**: 전체 시스템 요구사항 정의
- **[기술 스택 가이드](./docs/tech-stack.md)**: 사용된 기술 스택 상세 설명
- **[개발 가이드](./docs/development-guide.md)**: 개발 컨벤션 및 아키텍처 가이드
- **[API 문서](./docs/api-docs.md)**: REST API 엔드포인트 상세 명세
- **[배포 가이드](./docs/deployment.md)**: AWS 기반 배포 절차

## 🛣 향후 계획 (Roadmap)

### Phase 1: 핵심 기능 완성 ✅
- [x] 유튜브 영상 요약 기능
- [x] 사용자 인증 및 라이브러리
- [x] AI 기반 퀴즈 생성
- [x] 리마인드 알림 시스템

### Phase 2: 사용자 경험 개선 🚧
- [ ] 요약 대기 중 미니게임 추가
- [ ] 모바일 반응형 UI 최적화
- [ ] 실시간 알림 시스템 (WebSocket)
- [ ] 다국어 지원 (영어, 일본어)

### Phase 3: 고도화 기능 📋
- [ ] 영상 북마크 및 구간별 메모
- [ ] 협업 학습 기능 (그룹 스터디)
- [ ] 학습 진도 관리 시스템
- [ ] AI 기반 개인 맞춤 학습 경로 추천

### Phase 4: 확장성 강화 🔮
- [ ] 마이크로서비스 아키텍처 전환
- [ ] Kubernetes 기반 컨테이너 오케스트레이션
- [ ] 실시간 데이터 처리 (Apache Kafka)
- [ ] 머신러닝 모델 자체 구축

---

# 🔐 YouSum 백엔드-프론트엔드 보안 구조 분석

## 📊 전체 보안 아키텍처

### 1. 보안 계층 구조
이 다이어그램은 YouSum 플랫폼의 전체 보안 아키텍처를 5개 계층으로 나누어 보여줍니다. 프론트엔드에서 시작된 요청이 네트워크 계층을 거쳐 백엔드 보안 필터들을 통과하고, 인증/인가 시스템을 거쳐 최종적으로 데이터 계층에 도달하는 전체 흐름을 시각화합니다.

```mermaid
graph TB
    subgraph "프론트엔드 (YouSumFront)"
        F1[React 애플리케이션]
        F2[Axios HTTP 클라이언트]
        F3[로컬 스토리지<br/>JWT 토큰]
        F4[인증 컨텍스트<br/>상태 관리]
    end

    subgraph "네트워크 계층"
        N1[HTTPS/HTTP 요청]
        N2[CORS 정책<br/>localhost:5173]
        N3[Authorization 헤더<br/>Bearer 토큰]
    end

    subgraph "백엔드 보안 계층"
        B1[Spring Security 필터 체인]
        B2[JWT 인증 필터]
        B3[JWT 로그인 필터]
        B4[OAuth2 로그인 필터]
        B5[CORS 설정]
    end

    subgraph "인증 및 인가"
        A1[JWT 제공자<br/>토큰 생성/검증]
        A2[사용자 상세 서비스<br/>사용자 로딩]
        A3[BCrypt 비밀번호 암호화]
        A4[구글 OAuth2 서비스]
        A5[보안 컨텍스트<br/>인증 정보 저장]
    end

    subgraph "데이터 계층"
        D1[MySQL 데이터베이스<br/>사용자 인증정보]
        D2[JWT 비밀키<br/>환경 변수]
        D3[구글 OAuth2<br/>클라이언트 인증정보]
    end

    F1 --> F2
    F2 --> F3
    F3 --> F4
    F2 --> N1
    N1 --> N2
    N2 --> N3
    N3 --> B1
    B1 --> B2
    B1 --> B3
    B1 --> B4
    B1 --> B5
    B2 --> A1
    B3 --> A1
    B4 --> A4
    A1 --> A2
    A2 --> A3
    A1 --> A5
    A2 --> D1
    A1 --> D2
    A4 --> D3

    classDef frontend fill:#e1f5fe
    classDef network fill:#f3e5f5
    classDef backend fill:#e8f5e8
    classDef auth fill:#fff3e0
    classDef data fill:#fce4ec

    class F1,F2,F3,F4 frontend
    class N1,N2,N3 network
    class B1,B2,B3,B4,B5 backend
    class A1,A2,A3,A4,A5 auth
    class D1,D2,D3 data
```

**계층별 주요 역할:**
- **프론트엔드 계층**: 사용자 인터페이스, HTTP 클라이언트, 토큰 저장 및 상태 관리
- **네트워크 계층**: HTTPS 통신, CORS 정책 적용, 인증 헤더 전송
- **백엔드 보안 계층**: Spring Security 필터 체인을 통한 요청 검증 및 인증 처리
- **인증/인가 계층**: JWT 토큰 관리, 사용자 정보 로딩, 비밀번호 암호화, OAuth2 처리
- **데이터 계층**: 사용자 인증 정보 저장, 보안 키 관리

## 🔄 JWT 기반 API 인증 플로우

### 2. 보호된 API 접근 시퀀스
이 시퀀스 다이어그램은 사용자가 보호된 API 엔드포인트에 접근할 때의 전체 인증 과정을 보여줍니다. JWT 토큰의 검증부터 사용자 정보 로딩, 보안 컨텍스트 설정까지의 상세한 흐름을 시각화합니다.

```mermaid
sequenceDiagram
    participant F as 프론트엔드<br/>(YouSumFront)
    participant C as CORS 필터
    participant SF as 보안 필터 체인
    participant JF as JWT 인증 필터
    participant JP as JWT 제공자
    participant UDS as 사용자상세서비스
    participant SC as 보안 컨텍스트
    participant API as 보호된 API 엔드포인트
    participant DB as MySQL 데이터베이스

    Note over F,DB: JWT 기반 API 인증 흐름

    F->>+C: Authorization 헤더와 함께 HTTP 요청
    Note right of F: Authorization: Bearer eyJ0eXAiOiJKV1Q...
    
    C->>C: CORS 정책 검사<br/>(localhost:5173 허용)
    C->>+SF: 요청 전달
    
    SF->>+JF: JWT 인증 처리
    
    alt JWT 토큰이 존재하는 경우
        JF->>JF: 헤더에서 토큰 추출<br/>Bearer eyJ0eXAiOiJKV1Q...
        JF->>+JP: 토큰 유효성 검증
        JP->>JP: 서명 및 만료시간 확인
        JP-->>-JF: 토큰 유효함
        
        JF->>+JP: 사용자 ID 추출
        JP-->>-JF: userId: 123
        
        JF->>+UDS: 사용자 ID로 사용자 정보 로딩
        UDS->>+DB: SELECT * FROM users WHERE id = 123
        DB-->>-UDS: 사용자 엔티티
        UDS-->>-JF: 사용자 상세정보
        
        JF->>+SC: 인증 정보 설정
        SC-->>-JF: 인증 설정 완료
        JF-->>-SF: 인증 성공
        
        SF->>+API: 보호된 엔드포인트로 전달
        API->>API: 비즈니스 로직 처리
        API-->>-SF: API 응답
        
    else JWT 토큰이 유효하지 않거나 없는 경우
        JF->>JF: 토큰 검증 실패
        JF-->>SF: 401 인증 실패
        SF-->>C: 오류 응답
    end
    
    SF-->>-C: 최종 응답
    C-->>-F: HTTP 응답
```

**주요 처리 단계:**
1. **CORS 검증**: 프론트엔드 도메인(localhost:5173) 허용 여부 확인
2. **토큰 추출**: Authorization 헤더에서 Bearer 토큰 추출
3. **토큰 검증**: JWT 서명 및 만료시간 확인
4. **사용자 로딩**: 토큰에서 추출한 사용자 ID로 데이터베이스 조회
5. **컨텍스트 설정**: Spring Security Context에 인증 정보 저장

## 🔑 사용자 인증 플로우

### 3. 로그인 인증 시퀀스 (일반 로그인 + OAuth2)
이 다이어그램은 YouSum 플랫폼에서 지원하는 두 가지 로그인 방식을 보여줍니다. 일반적인 아이디/비밀번호 로그인과 구글 OAuth2 소셜 로그인의 전체 과정을 비교하여 시각화합니다.

```mermaid
sequenceDiagram
    participant F as 프론트엔드<br/>(YouSumFront)
    participant LF as 로그인 필터
    participant AM as 인증 관리자
    participant UDS as 사용자상세서비스
    participant PE as 비밀번호 암호화
    participant JP as JWT 제공자
    participant DB as MySQL 데이터베이스
    participant LS as 로컬 스토리지

    Note over F,LS: 로그인 인증 흐름

    rect rgb(240, 248, 255)
        Note over F,LS: 일반 로그인 (아이디/비밀번호)
        F->>+LF: POST /api/auth/login<br/>{username, password}
        
        LF->>+AM: 인증 시도
        AM->>+UDS: 사용자명으로 사용자 조회
        UDS->>+DB: SELECT * FROM users WHERE username = ?
        DB-->>-UDS: 사용자 엔티티
        UDS-->>-AM: 사용자 상세정보
        
        AM->>+PE: 비밀번호 일치 확인
        PE-->>-AM: 비밀번호 유효
        AM-->>-LF: 인증 성공
        
        LF->>+JP: JWT 토큰 생성
        JP-->>-LF: JWT 토큰
        
        LF-->>F: {accessToken, username, userId}
        F->>+LS: JWT 토큰 저장
        LS-->>-F: 토큰 저장 완료
    end

    rect rgb(240, 255, 240)
        Note over F,LS: OAuth2 구글 로그인
        F->>F: 구글 로그인 버튼 클릭
        F->>LF: /oauth2/authorization/google로 리다이렉트
        
        LF->>LF: OAuth2 인증 요청
        Note right of LF: 구글 OAuth2로 리다이렉트
        
        LF-->>F: 구글 로그인 페이지
        F->>F: 구글 계정으로 로그인
        F->>LF: 인증 코드와 함께 OAuth2 콜백
        
        LF->>+JP: JWT 토큰 생성
        JP-->>-LF: JWT 토큰
        
        LF-->>F: 토큰과 함께 리다이렉트
        F->>+LS: JWT 토큰 저장
        LS-->>-F: 토큰 저장 완료
    end
```

**인증 방식별 특징:**
- **일반 로그인**: BCrypt를 사용한 비밀번호 해시 검증, 데이터베이스 기반 사용자 확인
- **OAuth2 로그인**: 구글 계정 연동, 별도 비밀번호 불필요, 자동 회원가입 지원

## ⚖️ 보안 책임 분담

### 4. 프론트엔드 vs 백엔드 보안 역할
이 다이어그램은 YouSum 플랫폼에서 프론트엔드(YouSumFront)와 백엔드가 각각 담당하는 보안 책임을 명확히 구분하여 보여줍니다. 또한 양쪽이 공통으로 고려해야 할 보안 관심사도 함께 표시합니다.

```mermaid
graph LR
    subgraph "프론트엔드 보안 책임"
        F1[토큰 저장<br/>localStorage/sessionStorage]
        F2[자동 토큰 첨부<br/>Axios 인터셉터]
        F3[라우트 보호<br/>Private Routes]
        F4[토큰 갱신 로직<br/>401 응답 처리]
        F5[로그아웃 토큰 정리<br/>스토리지 초기화]
    end

    subgraph "백엔드 보안 책임"
        B1[JWT 토큰 검증<br/>서명 및 만료시간]
        B2[사용자 인증<br/>비밀번호 확인]
        B3[권한 검사<br/>역할 기반 접근제어]
        B4[CORS 설정<br/>교차 출처 요청]
        B5[보안 헤더<br/>XSS, CSRF 방지]
        B6[요청 제한<br/>API 남용 방지]
    end

    subgraph "공통 보안 관심사"
        S1[HTTPS 통신<br/>전송 중 데이터 암호화]
        S2[토큰 만료<br/>시간 기반 보안]
        S3[오류 처리<br/>정보 노출 방지]
        S4[입력값 검증<br/>데이터 검증 및 정제]
    end

    F1 --> S1
    F2 --> S1
    F3 --> S2
    F4 --> S2
    F5 --> S3
    
    B1 --> S1
    B2 --> S4
    B3 --> S2
    B4 --> S1
    B5 --> S3
    B6 --> S4

    classDef frontend fill:#e3f2fd
    classDef backend fill:#e8f5e8
    classDef shared fill:#fff3e0

    class F1,F2,F3,F4,F5 frontend
    class B1,B2,B3,B4,B5,B6 backend
    class S1,S2,S3,S4 shared
```

**역할 분담 상세:**

**프론트엔드 보안 책임:**
- **토큰 관리**: JWT 토큰의 안전한 저장 및 자동 첨부
- **라우트 보호**: 인증되지 않은 사용자의 접근 차단
- **세션 관리**: 로그아웃 시 토큰 정리 및 401 오류 대응

**백엔드 보안 책임:**
- **토큰 검증**: JWT 서명 및 만료시간 확인
- **인증/인가**: 사용자 신원 확인 및 권한 검사
- **보안 정책**: CORS, 보안 헤더, Rate Limiting 적용

**공통 보안 관심사:**
- **통신 보안**: HTTPS를 통한 데이터 암호화
- **시간 기반 보안**: 토큰 만료를 통한 세션 관리
- **입력값 검증**: 클라이언트와 서버 양쪽에서의 데이터 검증

---

## 🛡️ 프론트엔드 보안 구현 가이드 (YouSumFront)

### Axios 인터셉터 설정 예시
```javascript
// 요청 인터셉터 - 자동 토큰 첨부
axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  }
);

// 응답 인터셉터 - 401 에러 처리
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('accessToken');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

### Protected Route 컴포넌트 예시
```javascript
const ProtectedRoute = ({ children }) => {
  const token = localStorage.getItem('accessToken');
  
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  
  return children;
};
```

---

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참고하세요.

## 🤝 기여하기

프로젝트에 기여하고 싶으시다면:

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

<div align="center">

**YouSum Team** | 능동적 학습자를 위한 AI 기반 학습 플랫폼

[🌐 Website](https://yousum.com) • [📧 Contact](mailto:team@yousum.com) • [📱 Demo](https://demo.yousum.com)

</div> 
