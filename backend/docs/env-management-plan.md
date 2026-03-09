# 환경변수 관리 체계 정비 계획

## 1. 현재 상태 및 문제점

### 1-1. 현재 환경변수 참조 현황

| 파일 | 참조하는 환경변수 | 기본값 |
|------|-----------------|--------|
| `application.yaml` | `JWT_SECRET_KEY` | 없음 |
| `application.yaml` | `AWS_CREDENTIALS_ACCESS_KEY` | 없음 |
| `application.yaml` | `AWS_CREDENTIALS_SECRET_KEY` | 없음 |
| `application-dev.yaml` | `DB_USERNAME` | 없음 |
| `application-dev.yaml` | `DB_PASSWORD` | 없음 |
| `application-prod.yaml` | `SPRING_DATASOURCE_URL` | 없음 |
| `application-prod.yaml` | `SPRING_DATASOURCE_USERNAME` | 없음 |
| `application-prod.yaml` | `SPRING_DATASOURCE_PASSWORD` | 없음 |
| `application.yaml` (base) | `REDIS_HOST`, `REDIS_PORT` | localhost, 6379 |

### 1-2. 확인된 문제점

**[보안]**
- `docker-compose.yaml`에 DB 비밀번호(`moyada1343`, `1234`)가 평문 하드코딩 → git에 노출
- `docker-compose.yaml`에 `JWT_SECRET_KEY` 주입 없음 → prod 환경에서 JWT 서명 불가능 상태
- `.gitignore`에 백엔드 `.env` 파일 항목 없음

**[일관성]**
- Dev와 Prod의 DB 환경변수 이름이 다름 (`DB_USERNAME` vs `SPRING_DATASOURCE_USERNAME`)
- `application-dev.yaml`에서 DB URL이 하드코딩 (`jdbc:mariadb://localhost:3307/myblog`), 환경변수 미사용
- Dev 환경변수가 IDE 실행 설정에만 등록되어 있고 파일로 관리되지 않음 → 팀원 간 공유 불가, 재현 어려움

**[불필요한 설정]**
- S3 버킷 삭제 이후에도 `application.yaml`에 AWS 자격증명 환경변수가 필수값으로 남아있음
- Dev 환경변수에 `DB_URL`이 있으나 yaml에서 사용하지 않음

---

## 2. 목표 구조

```
myblog-boot/
├── .env.dev.example          # git 추적 O — 키 이름과 설명만 포함 (값 없음)
├── .env.prod.example         # git 추적 O — 키 이름과 설명만 포함 (값 없음)
├── .env.dev                  # git 추적 X — 로컬 개발 실제 값
├── .env.prod                 # git 추적 X — 운영 서버에만 존재
├── docker-compose.yaml       # prod용 — env_file로 .env.prod 참조
├── docker-compose.dev.yaml   # dev용 — 로컬 DB/Redis 컨테이너만 실행
└── src/main/resources/
    ├── application.yaml      # 공통 + 기본값 설정
    ├── application-dev.yaml  # dev 프로파일 — .env.dev 값 사용
    └── application-prod.yaml # prod 프로파일 — .env.prod 값 사용
```

---

## 3. 환경변수 목록 정의

### 3-1. Dev 환경 (`.env.dev`)

| 환경변수 | 예시 값 | 설명 |
|----------|---------|------|
| `DB_URL` | `jdbc:mariadb://localhost:3307/myblog` | 로컬 MariaDB 접속 URL |
| `DB_USERNAME` | `moyada` | DB 사용자명 |
| `DB_PASSWORD` | `moyada1343` | DB 비밀번호 |
| `JWT_SECRET_KEY` | `moya.myblog.secret.key` | JWT 서명 키 |
| `REDIS_HOST` | `localhost` | Redis 호스트 (기본값과 동일하면 생략 가능) |
| `REDIS_PORT` | `6379` | Redis 포트 (기본값과 동일하면 생략 가능) |

> AWS 관련 키는 S3 버킷 삭제로 불필요 → **제거 대상**

### 3-2. Prod 환경 (`.env.prod`)

| 환경변수 | 예시 값 | 설명 |
|----------|---------|------|
| `DB_URL` | `jdbc:mariadb://db:3306/myblog` | Docker 네트워크 내 DB URL |
| `DB_USERNAME` | `moyada` | DB 사용자명 |
| `DB_PASSWORD` | `(강력한 비밀번호)` | DB 비밀번호 |
| `DB_ROOT_PASSWORD` | `(강력한 비밀번호)` | MariaDB root 비밀번호 |
| `JWT_SECRET_KEY` | `(256비트 이상 랜덤 문자열)` | JWT 서명 키 |
| `REDIS_HOST` | `rediscache` | Docker 서비스명 |
| `REDIS_PORT` | `6379` | Redis 포트 |

> Dev와 Prod 모두 동일한 환경변수 이름을 사용하도록 통일

---

## 4. 변경 대상 파일 상세

### 4-1. `.gitignore` 수정

백엔드 `.env` 파일을 git 추적에서 제외한다.

```gitignore
# 추가할 항목
.env.dev
.env.prod
```

### 4-2. `application.yaml` 수정 (공통 base 설정)

- AWS 관련 설정 블록 **전체 제거** (S3 삭제로 불필요)
- `JWT_SECRET_KEY` 기본값 없음 유지 (필수값이므로 두 환경 모두 반드시 주입)

```yaml
# 제거 대상 블록
# cloud:
#   aws:
#     credentials:
#       access-key: ${AWS_CREDENTIALS_ACCESS_KEY}
#       secret-key: ${AWS_CREDENTIALS_SECRET_KEY}
#     s3:
#       bucketName: myblog-boot-bucket
#     region:
#       static: ap-northeast-2
#       auto: false
```

### 4-3. `application-dev.yaml` 수정

DB URL을 환경변수로 분리하고, 변수명을 Prod와 통일한다.

```yaml
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    driver-class-name: org.mariadb.jdbc.Driver
    url: ${DB_URL}              # 기존: 하드코딩 → 환경변수로 변경
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  # ... 나머지 동일
```

### 4-4. `application-prod.yaml` 수정

환경변수 이름을 Dev와 통일한다.

```yaml
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    driver-class-name: org.mariadb.jdbc.Driver
    url: ${DB_URL}                    # 기존: SPRING_DATASOURCE_URL → 통일
    username: ${DB_USERNAME}          # 기존: SPRING_DATASOURCE_USERNAME → 통일
    password: ${DB_PASSWORD}          # 기존: SPRING_DATASOURCE_PASSWORD → 통일
```

### 4-5. `docker-compose.yaml` 수정 (Prod용)

하드코딩 값을 제거하고 `env_file`로 `.env.prod`를 참조한다.

```yaml
version: '3'
services:
  db:
    image: mariadb
    restart: always
    env_file:
      - .env.prod
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_DATABASE: myblog
      MYSQL_USER: ${DB_USERNAME}
      MYSQL_PASSWORD: ${DB_PASSWORD}
    ports:
      - 3306:3306
    volumes:
      - dbdata:/var/lib/mysql

  redis:
    image: redis
    restart: always
    container_name: rediscache
    ports:
      - 6379:6379
    networks:
      - spring-net

  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - db
      - redis
    env_file:
      - .env.prod
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mariadb://db:3306/myblog
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET_KEY: ${JWT_SECRET_KEY}
      REDIS_HOST: rediscache
      REDIS_PORT: 6379
    networks:
      - spring-net

networks:
  spring-net:
volumes:
  dbdata:
```

### 4-6. `docker-compose.dev.yaml` 신규 생성 (Dev용)

로컬 개발에서 DB와 Redis 컨테이너만 실행하기 위한 파일. 앱 자체는 IDE에서 실행한다.

```yaml
version: '3'
services:
  db:
    image: mariadb
    restart: always
    env_file:
      - .env.dev
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_DATABASE: myblog
      MYSQL_USER: ${DB_USERNAME}
      MYSQL_PASSWORD: ${DB_PASSWORD}
    ports:
      - 3307:3306
    volumes:
      - dbdata-dev:/var/lib/mysql

  redis:
    image: redis
    restart: always
    container_name: rediscache-dev
    ports:
      - 6379:6379

volumes:
  dbdata-dev:
```

**사용 방법:**
```bash
docker compose -f docker-compose.dev.yaml up -d
```

### 4-7. `.env.dev.example` 신규 생성 (git 추적)

```dotenv
# 개발 환경 환경변수 예시 파일
# 이 파일을 복사하여 .env.dev 를 만들고 실제 값을 채워넣으세요.
# .env.dev 는 절대 git에 커밋하지 마세요.

DB_URL=jdbc:mariadb://localhost:3307/myblog
DB_USERNAME=
DB_PASSWORD=
DB_ROOT_PASSWORD=
JWT_SECRET_KEY=
REDIS_HOST=localhost
REDIS_PORT=6379
```

### 4-8. `.env.prod.example` 신규 생성 (git 추적)

```dotenv
# 운영 환경 환경변수 예시 파일
# 운영 서버에서 이 파일을 복사하여 .env.prod 를 만들고 실제 값을 채워넣으세요.
# .env.prod 는 절대 git에 커밋하지 마세요.

DB_URL=jdbc:mariadb://db:3306/myblog
DB_USERNAME=
DB_PASSWORD=
DB_ROOT_PASSWORD=
JWT_SECRET_KEY=
REDIS_HOST=rediscache
REDIS_PORT=6379
```

---

## 5. IDE 실행 설정 (개발자 로컬)

IntelliJ IDEA 기준: `Run/Debug Configurations` → `EnvFile` 플러그인을 사용하거나,
`Environment variables` 항목에 `.env.dev` 파일 경로를 지정한다.

또는 Gradle 실행 시 직접 env 파일을 로드하는 방식도 가능하다:
```bash
# Linux/macOS
export $(cat .env.dev | xargs) && ./gradlew bootRun --args='--spring.profiles.active=dev'

# Windows (PowerShell)
Get-Content .env.dev | ForEach-Object { $k,$v = $_ -split '=',2; [System.Environment]::SetEnvironmentVariable($k,$v) }
./gradlew bootRun --args='--spring.profiles.active=dev'
```

---

## 6. 적용 후 환경별 흐름 요약

```
[Dev 환경]
  .env.dev (로컬에만 존재, git 제외)
    ↓
  IDE 실행 설정 또는 export 명령어로 환경변수 주입
    ↓
  application.yaml + application-dev.yaml 로드
    ↓
  H2(test) / MariaDB(dev) + 로컬 Redis 사용, S3 없음

[Prod 환경]
  .env.prod (운영 서버에만 존재, git 제외)
    ↓
  docker compose --env-file .env.prod up
    ↓
  application.yaml + application-prod.yaml 로드
    ↓
  Docker 내부 MariaDB + Redis 사용, S3 없음 (추후 재도입 시 추가)
```

---

## 7. 작업 체크리스트

### Phase 1 — 기반 정비
- [ ] `.gitignore`에 `.env.dev`, `.env.prod` 추가
- [ ] `.env.dev.example` 생성 (git 추적)
- [ ] `.env.prod.example` 생성 (git 추적)
- [ ] `.env.dev` 로컬 생성 (git 제외, 실제 값 입력)

### Phase 2 — 설정 파일 수정
- [ ] `application.yaml` — AWS 설정 블록 제거
- [ ] `application-dev.yaml` — DB URL 환경변수화 (`${DB_URL}`)
- [ ] `application-prod.yaml` — 환경변수명 통일 (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)

### Phase 3 — Docker Compose 수정
- [ ] `docker-compose.yaml` — 하드코딩 값 제거, `env_file: .env.prod` 적용, `JWT_SECRET_KEY` 주입 추가
- [ ] `docker-compose.dev.yaml` 신규 생성 — 로컬 DB/Redis 전용

### Phase 4 — S3 코드 정리 (별도 계획서 참조)
- [ ] `docs/s3-dev-environment-plan.md` 에 따라 S3 코드 처리

### Phase 5 — 검증
- [ ] dev 프로파일로 앱 기동 확인
- [ ] prod 환경에서 `docker compose up` 후 정상 동작 확인
- [ ] `.env.dev`, `.env.prod` 파일이 git status에 나타나지 않는지 확인

---

## 8. 참고: 변경 전/후 환경변수 매핑

| 현재 (변경 전) | 변경 후 | 비고 |
|---------------|---------|------|
| `DB_USERNAME` (dev only) | `DB_USERNAME` (공통) | 통일 |
| `DB_PASSWORD` (dev only) | `DB_PASSWORD` (공통) | 통일 |
| `SPRING_DATASOURCE_URL` (prod only) | `DB_URL` (공통) | 통일 |
| `SPRING_DATASOURCE_USERNAME` (prod only) | `DB_USERNAME` (공통) | 통일 |
| `SPRING_DATASOURCE_PASSWORD` (prod only) | `DB_PASSWORD` (공통) | 통일 |
| `AWS_CREDENTIALS_ACCESS_KEY` | **제거** | S3 삭제 |
| `AWS_CREDENTIALS_SECRET_KEY` | **제거** | S3 삭제 |
| `JWT_SECRET_KEY` | `JWT_SECRET_KEY` | 유지, prod docker-compose에 주입 추가 |
| `DB_URL` (dev, 미사용) | `DB_URL` (dev, 사용) | yaml에서 사용하도록 변경 |
