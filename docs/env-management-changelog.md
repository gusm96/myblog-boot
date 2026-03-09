# 환경변수 관리 체계 정비 작업 내역

> 작업일: 2026-03-06
> 관련 계획서: [docs/env-management-plan.md](./env-management-plan.md)

---

## 1. 작업 배경

| 구분 | 내용 |
|------|------|
| 동기 | AWS S3 버킷 삭제 후 기동 오류 + 환경변수가 파일 없이 IDE에만 등록된 상태 |
| 목표 | Dev / Prod 환경변수를 파일로 체계화하고, 민감 정보를 git에서 분리 |

---

## 2. 변경 전 문제점 요약

- `docker-compose.yaml`에 DB 비밀번호(`moyada1343`, `1234`)가 평문 하드코딩 → **git 노출**
- `docker-compose.yaml`에 `JWT_SECRET_KEY` 주입 누락 → **운영 환경에서 JWT 서명 불가**
- `application.yaml`의 AWS 자격증명 환경변수에 기본값 없음 → **S3 버킷 삭제 후 기동 실패**
- Dev(`DB_USERNAME`) / Prod(`SPRING_DATASOURCE_USERNAME`) **변수명 불일치**
- 환경변수 파일 부재 → 개발 환경 재현 어려움

---

## 3. 변경 파일 상세

### 3-1. `.gitignore` — 환경변수 파일 제외 추가

```diff
+ # 환경변수 파일 (실제 값 포함, 절대 커밋 금지)
+ .env.dev
+ .env.prod
```

### 3-2. `application.yaml` — AWS 자격증명 기본값 추가 및 Stack 감지 비활성화

S3 버킷이 삭제된 상태에서 `AwsS3Config` 빈이 자격증명 환경변수를 요구해 기동이 실패하던
문제를 해소한다. S3 코드 정리(`docs/s3-dev-environment-plan.md`) 완료 후 해당 블록 전체 제거 예정.

```diff
- cloud:
-   aws:
-     credentials:
-       access-key: ${AWS_CREDENTIALS_ACCESS_KEY}
-       secret-key: ${AWS_CREDENTIALS_SECRET_KEY}
-     s3:
-       bucketName: myblog-boot-bucket
-     region:
-       static: ap-northeast-2
-       auto: false

+ # S3 (버킷 삭제됨 — S3 코드 정리 전까지 임시 더미값 유지, 이후 블록 전체 제거 예정)
+ cloud:
+   aws:
+     credentials:
+       access-key: ${AWS_CREDENTIALS_ACCESS_KEY:dummy-access-key}
+       secret-key: ${AWS_CREDENTIALS_SECRET_KEY:dummy-secret-key}
+     s3:
+       bucketName: myblog-boot-bucket
+     region:
+       static: ap-northeast-2
+       auto: false
+     stack:
+       auto: false
```

### 3-3. `application-prod.yaml` — DB 환경변수명 통일

Dev 와 Prod 가 동일한 변수명을 공유하도록 변경한다.

```diff
- url: ${SPRING_DATASOURCE_URL}
- username: ${SPRING_DATASOURCE_USERNAME}
- password: ${SPRING_DATASOURCE_PASSWORD}

+ url: ${DB_URL}
+ username: ${DB_USERNAME}
+ password: ${DB_PASSWORD}
```

### 3-4. `docker-compose.yaml` — 하드코딩 제거 및 env_file 적용

기존 파일의 문제점을 전면 수정한다.

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| DB 비밀번호 | 평문 하드코딩 | `.env.prod` 참조 |
| JWT 키 | 미주입 | `env_file: .env.prod` 로 주입 |
| `spring-testcontainers-app` 서비스 | 존재 (미사용) | 제거 |
| app 포트 | `8081:8080` | `8080:8080` |
| DB 외부 포트 노출 | 있음 (`3307:3307`) | 제거 (내부 네트워크만 사용) |

```yaml
# 운영 환경용
# 실행 방법: docker compose --env-file .env.prod up -d

services:
  db:
    image: mariadb
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}  # compose 변수 치환 (--env-file)
      MYSQL_DATABASE: myblog
      MYSQL_USER: ${DB_USERNAME}
      MYSQL_PASSWORD: ${DB_PASSWORD}
    # 외부 포트 미노출 (spring-net 내부 통신만 사용)

  app:
    env_file:
      - .env.prod   # Spring Boot 환경변수 전체 주입
    environment:
      SPRING_PROFILES_ACTIVE: prod
```

### 3-5. `docker-compose.dev.yaml` — 신규 생성

로컬 개발 시 DB와 Redis만 컨테이너로 실행하는 파일. 앱은 IDE에서 실행한다.

- MariaDB 포트: `3307:3306` (기존 파일의 잘못된 `3307:3307` 수정)
- 앱 서비스 없음 — IDE 실행과 분리

```bash
docker compose -f docker-compose.dev.yaml --env-file .env.dev up -d
```

---

## 4. 신규 생성 파일

| 파일 | git 추적 | 설명 |
|------|---------|------|
| `.env.dev` | **X** | 로컬 개발 실제 값. gitignore 적용 확인 |
| `.env.dev.example` | **O** | 키 목록 + 기본값만 포함. 개발자 온보딩용 |
| `.env.prod.example` | **O** | 키 목록 + 운영 서버 기본값만 포함. 서버 설정 가이드 |
| `docker-compose.dev.yaml` | **O** | 로컬 인프라 전용 Compose 파일 |

---

## 5. 환경변수 매핑 최종 현황

### Dev (`.env.dev` → IDE 주입)

| 변수 | 값 (예시) | 참조 위치 |
|------|----------|---------|
| `DB_URL` | `jdbc:mariadb://localhost:3307/myblog` | `application-dev.yaml` |
| `DB_USERNAME` | `moyada` | `application-dev.yaml` |
| `DB_PASSWORD` | `****` | `application-dev.yaml` |
| `DB_ROOT_PASSWORD` | `****` | `docker-compose.dev.yaml` |
| `JWT_SECRET_KEY` | `****` | `application.yaml` |
| `REDIS_HOST` | `localhost` | `application.yaml` (기본값 동일) |
| `REDIS_PORT` | `6379` | `application.yaml` (기본값 동일) |

### Prod (`.env.prod` → docker compose 주입)

| 변수 | 값 (예시) | 참조 위치 |
|------|----------|---------|
| `DB_URL` | `jdbc:mariadb://db:3306/myblog` | `application-prod.yaml` |
| `DB_USERNAME` | `****` | `application-prod.yaml`, `docker-compose.yaml` |
| `DB_PASSWORD` | `****` | `application-prod.yaml`, `docker-compose.yaml` |
| `DB_ROOT_PASSWORD` | `****` | `docker-compose.yaml` |
| `JWT_SECRET_KEY` | `****` | `application.yaml` |
| `REDIS_HOST` | `rediscache` | `application.yaml` |
| `REDIS_PORT` | `6379` | `application.yaml` |

---

## 6. 실행 방법

```bash
# 개발 환경 — 인프라 컨테이너 실행
docker compose -f docker-compose.dev.yaml --env-file .env.dev up -d

# 앱 실행 (IDE)
# VM options: -Dspring.profiles.active=dev
# EnvFile: .env.dev

# 운영 환경 — 전체 실행 (서버에서)
docker compose --env-file .env.prod up -d
```

---

## 7. 후속 작업

| 작업 | 관련 문서 | 상태 |
|------|---------|------|
| S3 코드 정리 (`AwsS3Config`, `FileUploadServiceImpl` 프로파일 분리) | `docs/s3-dev-environment-plan.md` | 대기 중 |
| `application.yaml` AWS 블록 제거 | S3 코드 정리 완료 후 | 대기 중 |
