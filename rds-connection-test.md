# AWS RDS MySQL 연결 테스트 가이드

## 🔧 RDS 생성 후 할 일

### 1️⃣ RDS 엔드포인트 확인 ✅
실제 생성된 RDS 정보:
```
엔드포인트: yousum-mysql-db.cvaqg68u4xoh.ap-northeast-2.rds.amazonaws.com
사용자명: admin
비밀번호: 12345678
포트: 3306
데이터베이스: yousum
```

### 2️⃣ 환경변수 설정

**Windows (PowerShell):**
```powershell
$env:RDS_ENDPOINT="yousum-mysql-db.cvaqg68u4xoh.ap-northeast-2.rds.amazonaws.com"
$env:RDS_USERNAME="admin"
$env:RDS_PASSWORD="12345678"
```

**Linux/Mac:**
```bash
export RDS_ENDPOINT="yousum-mysql-db.cvaqg68u4xoh.ap-northeast-2.rds.amazonaws.com"
export RDS_USERNAME="admin"
export RDS_PASSWORD="12345678"
```

### 3️⃣ MySQL 클라이언트로 직접 연결 테스트

```bash
mysql -h yousum-mysql-db.cvaqg68u4xoh.ap-northeast-2.rds.amazonaws.com -P 3306 -u admin -p
# 비밀번호 입력: 12345678
```

성공하면:
```sql
SHOW DATABASES;
USE yousum;
SHOW TABLES;
```

### 4️⃣ Spring Boot 애플리케이션 실행

```bash
# Windows
./gradlew bootRun

# Linux/Mac  
./gradlew bootRun
```

### 5️⃣ 연결 확인 로그

성공적인 연결 시 로그:
```
HikariCP - Start completed.
Spring Data JPA initialized.
o.hibernate.jpa.internal.util.LogHelper: HHH000204: Processing PersistenceUnitInfo
```

### 🚨 문제 해결

**연결 오류 시 체크리스트:**
- [ ] RDS 보안 그룹에 3306 포트 허용 설정
- [ ] RDS 퍼블릭 액세스 활성화
- [ ] 올바른 엔드포인트 URL 사용
- [ ] 사용자명/비밀번호 정확성
- [ ] VPC 설정 확인

**일반적인 오류:**
- `Communications link failure`: 보안 그룹 설정 확인
- `Access denied`: 사용자명/비밀번호 확인
- `Unknown database 'yousum'`: 초기 DB 생성 시 yousum 이름 확인 