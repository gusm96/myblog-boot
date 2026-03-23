# 배포 자동화 및 무중단 배포 전략

> 작성일: 2026-03-10
> 대상: myblog-boot 모노레포 (backend: Spring Boot 3.0.4 / frontend: React + Nginx)

---

## 1. 현재 상태 분석

### 프로젝트 구조

```
myblog-boot/
├── backend/          # Spring Boot (Gradle, JDK 17)
│   ├── Dockerfile
│   ├── .env.dev
│   └── .env.prod.example
├── frontend/         # React (Node 18, Nginx 서빙)
│   ├── Dockerfile
│   └── nginx.conf
├── docker-compose.yaml       # 운영 (backend + frontend + db + redis)
└── docker-compose.dev.yaml   # 개발 (db + redis만)
```

### 현재 배포 방식

- `docker-compose.yaml`로 전체 스택을 단일 서버에서 실행
- 수동 빌드 → 수동 배포 (CI/CD 없음, GitHub Actions 미설정)
- 배포 시 `docker compose down` → `up` → **서비스 중단 발생**
- SSL/TLS 미적용 (frontend가 80 포트 직접 노출)

### 개선이 필요한 부분

| 항목 | 현재 | 목표 |
|------|------|------|
| 빌드/배포 | 수동 | GitHub Actions 자동화 |
| 다운타임 | compose down/up (수십 초 중단) | 무중단 (Blue-Green 또는 Rolling) |
| 리버스 프록시 | 없음 (frontend Nginx가 80 직접 노출) | Nginx 리버스 프록시 + SSL |
| 테스트 자동화 | 로컬에서만 실행 | PR 생성 시 자동 실행 |
| 이미지 관리 | 로컬 빌드 | Docker Hub 또는 GitHub Container Registry |

---

## 2. 목표 아키텍처

```
[GitHub] --push--> [GitHub Actions] --build/test/push-->  [Container Registry]
                                                                  |
                                                            docker pull
                                                                  |
                                                          [운영 서버 (VPS)]
                                                                  |
                                    ┌─────────────────────────────┼──────────────────────────┐
                                    │                             │                          │
                               [Nginx Proxy]              [MariaDB]                    [Redis]
                              (80/443 + SSL)
                                    │
                         ┌──────────┴──────────┐
                    [Backend A]           [Backend B]        ← Blue-Green
                    (8081)                (8082)
```

---

## 3. 단계별 구현 로드맵

### Phase 1: CI 파이프라인 (테스트 자동화)

GitHub Actions로 PR/push 시 테스트를 자동 실행한다.

**파일:** `.github/workflows/ci.yml`

```yaml
name: CI

on:
  pull_request:
    branches: [master, develop]
  push:
    branches: [develop]

jobs:
  backend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - name: Grant execute permission for gradlew
        run: chmod +x backend/gradlew

      - name: Run tests
        working-directory: backend
        run: ./gradlew test

      - name: Upload test report
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: test-report
          path: backend/build/reports/tests/test/
```

**포인트:**
- GitHub Actions의 ubuntu 러너에는 Docker가 기본 설치되어 있으므로 Testcontainers가 바로 동작한다
- PR 생성 시 테스트가 통과해야 머지 가능하도록 Branch Protection Rule 설정 권장

---

### Phase 2: CD 파이프라인 (이미지 빌드 + 레지스트리 푸시)

master 브랜치에 머지되면 Docker 이미지를 빌드하여 레지스트리에 푸시한다.

**레지스트리 선택지:**

| 옵션 | 장점 | 단점 |
|------|------|------|
| GitHub Container Registry (ghcr.io) | GitHub 통합, 무료 | GitHub 계정 필요 |
| Docker Hub | 보편적, 무료 티어 | pull rate limit |
| AWS ECR | AWS 연동 | AWS 비용 |

**권장: GitHub Container Registry** (GitHub Actions와 자연스러운 통합)

**파일:** `.github/workflows/cd.yml`

```yaml
name: CD

on:
  push:
    branches: [master]

env:
  REGISTRY: ghcr.io
  BACKEND_IMAGE: ghcr.io/${{ github.repository }}/backend
  FRONTEND_IMAGE: ghcr.io/${{ github.repository }}/frontend

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      # Backend: 테스트 → JAR 빌드 → Docker 이미지
      - name: Build backend JAR
        working-directory: backend
        run: chmod +x gradlew && ./gradlew build

      - name: Log in to Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push backend image
        uses: docker/build-push-action@v5
        with:
          context: ./backend
          push: true
          tags: |
            ${{ env.BACKEND_IMAGE }}:latest
            ${{ env.BACKEND_IMAGE }}:${{ github.sha }}

      - name: Build and push frontend image
        uses: docker/build-push-action@v5
        with:
          context: ./frontend
          push: true
          build-args: |
            REACT_APP_API_URL=${{ secrets.REACT_APP_API_URL }}
          tags: |
            ${{ env.FRONTEND_IMAGE }}:latest
            ${{ env.FRONTEND_IMAGE }}:${{ github.sha }}

      # 운영 서버에 배포 트리거
      - name: Deploy to server
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SERVER_SSH_KEY }}
          script: |
            cd /opt/myblog-boot
            ./deploy.sh ${{ github.sha }}
```

**필요한 GitHub Secrets:**

| Secret | 설명 |
|--------|------|
| `SERVER_HOST` | 운영 서버 IP |
| `SERVER_USER` | SSH 접속 사용자 |
| `SERVER_SSH_KEY` | SSH 개인키 |
| `REACT_APP_API_URL` | 프론트엔드 API 엔드포인트 |

---

### Phase 3: 무중단 배포 (Blue-Green)

Docker Compose + Nginx 리버스 프록시로 Blue-Green 배포를 구현한다.

#### 3-1. 왜 Blue-Green인가?

| 전략 | 장점 | 단점 | 적합한 규모 |
|------|------|------|------------|
| **Blue-Green** | 구현 간단, 즉시 롤백 가능 | 서버 리소스 2배 필요 | 소~중규모 |
| Rolling | 리소스 효율적 | 롤백 복잡, 구버전/신버전 혼재 | 중~대규모 (K8s) |
| Canary | 점진적 배포, 위험 최소 | 구현 복잡, 모니터링 필수 | 대규모 |

**이 프로젝트는 단일 서버에서 운영되는 개인 블로그이므로 Blue-Green이 가장 적합하다.**

#### 3-2. Nginx 리버스 프록시 구성

**파일:** `infra/nginx/nginx.conf`

```nginx
upstream backend {
    # deploy.sh가 이 파일을 동적으로 교체
    server 127.0.0.1:8081;  # blue 또는 8082 (green)
}

server {
    listen 80;
    server_name your-domain.com;

    # HTTPS 리다이렉트 (SSL 적용 후 활성화)
    # return 301 https://$host$request_uri;

    # API 프록시
    location /api/ {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 프론트엔드 정적 파일
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
}
```

#### 3-3. Docker Compose 운영 구성

**파일:** `docker-compose.prod.yaml`

```yaml
version: '3.8'

services:
  db:
    image: mariadb
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_DATABASE: myblog
      MYSQL_USER: ${DB_USERNAME}
      MYSQL_PASSWORD: ${DB_PASSWORD}
    volumes:
      - dbdata:/var/lib/mysql
    networks:
      - myblog-net

  redis:
    image: redis:7-alpine
    restart: always
    networks:
      - myblog-net

  # Blue 인스턴스
  backend-blue:
    image: ghcr.io/gusm96/myblog-boot/backend:latest
    env_file: .env.prod
    environment:
      SPRING_PROFILES_ACTIVE: prod
      REDIS_HOST: redis
      DB_URL: jdbc:mariadb://db:3306/myblog
    ports:
      - "8081:8080"
    depends_on:
      - db
      - redis
    networks:
      - myblog-net

  # Green 인스턴스
  backend-green:
    image: ghcr.io/gusm96/myblog-boot/backend:latest
    env_file: .env.prod
    environment:
      SPRING_PROFILES_ACTIVE: prod
      REDIS_HOST: redis
      DB_URL: jdbc:mariadb://db:3306/myblog
    ports:
      - "8082:8080"
    depends_on:
      - db
      - redis
    networks:
      - myblog-net

  nginx:
    image: nginx:alpine
    restart: always
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./infra/nginx/nginx.conf:/etc/nginx/conf.d/default.conf
      - ./infra/nginx/frontend:/usr/share/nginx/html
      # SSL 인증서 (Phase 4에서 추가)
      # - ./infra/certbot/conf:/etc/letsencrypt
    depends_on:
      - backend-blue
      - backend-green
    networks:
      - myblog-net

networks:
  myblog-net:

volumes:
  dbdata:
```

#### 3-4. 배포 스크립트

**파일:** `infra/deploy.sh`

```bash
#!/bin/bash
set -e

IMAGE_TAG=${1:-latest}
REGISTRY="ghcr.io/gusm96/myblog-boot"
COMPOSE_FILE="docker-compose.prod.yaml"
NGINX_CONF="infra/nginx/nginx.conf"

# 현재 활성 인스턴스 확인 (blue: 8081, green: 8082)
if grep -q "8081" "$NGINX_CONF"; then
    CURRENT="blue"
    NEXT="green"
    NEXT_PORT="8082"
else
    CURRENT="green"
    NEXT="blue"
    NEXT_PORT="8081"
fi

echo "=== 현재 활성: $CURRENT → 다음 배포: $NEXT ==="

# 1. 새 이미지 Pull
echo "[1/5] 이미지 Pull..."
docker pull "$REGISTRY/backend:$IMAGE_TAG"
docker pull "$REGISTRY/frontend:$IMAGE_TAG"

# 2. 프론트엔드 정적 파일 추출
echo "[2/5] 프론트엔드 정적 파일 업데이트..."
TEMP_CONTAINER=$(docker create "$REGISTRY/frontend:$IMAGE_TAG")
docker cp "$TEMP_CONTAINER:/usr/share/nginx/html" infra/nginx/frontend
docker rm "$TEMP_CONTAINER"

# 3. 대기 인스턴스 교체
echo "[3/5] $NEXT 인스턴스 시작..."
docker compose -f "$COMPOSE_FILE" up -d "backend-$NEXT"

# 4. 헬스체크 대기
echo "[4/5] 헬스체크 대기..."
for i in $(seq 1 30); do
    if curl -sf "http://127.0.0.1:$NEXT_PORT/actuator/health" > /dev/null 2>&1; then
        echo "  $NEXT 인스턴스 정상 기동 확인"
        break
    fi
    if [ "$i" -eq 30 ]; then
        echo "  헬스체크 실패! 배포 중단."
        docker compose -f "$COMPOSE_FILE" stop "backend-$NEXT"
        exit 1
    fi
    sleep 2
done

# 5. Nginx 트래픽 전환
echo "[5/5] 트래픽 전환: $CURRENT → $NEXT"
sed -i "s/127.0.0.1:[0-9]*/127.0.0.1:$NEXT_PORT/" "$NGINX_CONF"
docker compose -f "$COMPOSE_FILE" exec nginx nginx -s reload

# 이전 인스턴스 정지
echo "=== 이전 인스턴스($CURRENT) 정지 ==="
docker compose -f "$COMPOSE_FILE" stop "backend-$CURRENT"

echo "=== 배포 완료: $NEXT (port $NEXT_PORT) ==="
```

**배포 흐름:**

```
1. 새 이미지 Pull
2. 프론트엔드 정적 파일 추출 → Nginx html 디렉토리 교체
3. 대기 인스턴스(Green) 시작
4. 헬스체크 통과 대기 (최대 60초)
5. Nginx reload로 트래픽 전환 (다운타임 0)
6. 이전 인스턴스(Blue) 정지
```

**롤백:**

```bash
# 즉시 롤백: Nginx 설정만 되돌리면 됨
./deploy.sh  # 다시 실행하면 이전 인스턴스로 전환
```

---

### Phase 4: SSL/TLS (HTTPS)

Let's Encrypt + Certbot으로 무료 SSL을 적용한다.

```bash
# 초기 인증서 발급
docker run --rm \
  -v ./infra/certbot/conf:/etc/letsencrypt \
  -v ./infra/nginx/frontend:/var/www/certbot \
  certbot/certbot certonly \
  --webroot -w /var/www/certbot \
  -d your-domain.com \
  --email your-email@example.com \
  --agree-tos
```

Nginx SSL 설정 추가:

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    # ... 기존 location 블록
}

server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;
}
```

인증서 자동 갱신 (crontab):

```bash
0 3 1 */2 * docker run --rm -v /opt/myblog-boot/infra/certbot/conf:/etc/letsencrypt certbot/certbot renew && docker compose exec nginx nginx -s reload
```

---

## 4. Backend Dockerfile 개선

현재 Dockerfile은 `openjdk:17` (full JDK, ~400MB+)을 사용한다. 멀티스테이지 빌드 + 경량 이미지로 개선한다.

**현재:**
```dockerfile
FROM openjdk:17
WORKDIR /myblog-boot
VOLUME /tmp
ARG JAR_FILE=./build/libs/*.jar
ADD ${JAR_FILE} myblog-boot.jar
ENTRYPOINT ["java", "-jar", "myblog-boot.jar"]
```

**개선안:**
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar

# 보안: root가 아닌 별도 사용자로 실행
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
```

**개선 포인트:**

| 항목 | 현재 | 개선 |
|------|------|------|
| 베이스 이미지 | `openjdk:17` (~400MB) | `eclipse-temurin:17-jre-alpine` (~80MB) |
| 실행 사용자 | root | 전용 사용자 (appuser) |
| 헬스체크 | 없음 | `/actuator/health` 체크 |
| 메모리 설정 | 없음 | 컨테이너 메모리의 75% 사용 |

**참고:** 헬스체크를 사용하려면 Spring Boot Actuator 의존성이 필요하다.

```groovy
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

---

## 5. 환경별 구성 요약

| 환경 | 프로필 | DB | Redis | 실행 방법 |
|------|--------|-----|-------|----------|
| 로컬 개발 | dev | docker-compose.dev.yaml (3307) | docker-compose.dev.yaml (6379) | IDE 또는 `./gradlew bootRun` |
| 테스트 | test | H2 인메모리 | Testcontainers (자동) | `./gradlew test` |
| 운영 | prod | docker-compose.prod.yaml (내부) | docker-compose.prod.yaml (내부) | GitHub Actions → 자동 배포 |

---

## 6. 필요한 GitHub Secrets 목록

| Secret | 용도 | 예시 |
|--------|------|------|
| `SERVER_HOST` | 운영 서버 IP/도메인 | `123.456.789.0` |
| `SERVER_USER` | SSH 사용자 | `deploy` |
| `SERVER_SSH_KEY` | SSH 개인키 (Ed25519 권장) | `-----BEGIN OPENSSH PRIVATE KEY-----...` |
| `REACT_APP_API_URL` | 프론트엔드 API URL | `https://your-domain.com` |

---

## 7. 운영 서버 초기 설정 체크리스트

```
[ ] VPS/클라우드 서버 준비 (Ubuntu 22.04+ 권장)
[ ] Docker + Docker Compose 설치
[ ] 배포 전용 사용자 생성 (deploy)
[ ] SSH 키 기반 인증 설정
[ ] /opt/myblog-boot 디렉토리 생성
[ ] .env.prod 파일 배치 (서버에서 직접 생성, Git에 포함하지 않음)
[ ] docker-compose.prod.yaml 배치
[ ] infra/ 디렉토리 배치 (nginx.conf, deploy.sh)
[ ] deploy.sh 실행 권한 부여 (chmod +x)
[ ] 방화벽: 80, 443 포트 개방
[ ] 도메인 DNS A 레코드 설정
[ ] SSL 인증서 발급 (Let's Encrypt)
[ ] GitHub Secrets 등록
```

---

## 8. 구현 우선순위

| 순서 | Phase | 내용 | 난이도 | 효과 |
|:---:|:-----:|------|:-----:|------|
| 1 | Phase 1 | CI: GitHub Actions 테스트 자동화 | 낮음 | PR 품질 보장 |
| 2 | Phase 2 | CD: 이미지 빌드 + 레지스트리 푸시 | 중간 | 수동 빌드 제거 |
| 3 | — | Backend Dockerfile 개선 | 낮음 | 이미지 경량화, 보안 향상 |
| 4 | Phase 3 | 무중단 배포 (Blue-Green + Nginx) | 중간 | 다운타임 제거 |
| 5 | Phase 4 | SSL/TLS (Let's Encrypt) | 낮음 | HTTPS 보안 |
