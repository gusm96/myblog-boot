# AWS 비용 최적화 배포 전략

## 목표

개인 블로그 수준의 트래픽에서 AWS 비용을 최소화하면서  
HTTPS, 안정적인 운영, 향후 확장 가능성을 확보한다.

---

## 아키텍처 요약

```
사용자
 │
 ├─── 정적 파일 (React) ──► CloudFront ──► S3
 │
 └─── API 요청 ──► EC2 (t3.small)
                    ├── Nginx (리버스 프록시 + SSL)
                    ├── Spring Boot (Docker)
                    ├── MariaDB (Docker)
                    └── Redis (Docker)
```

**핵심 결정**: 프론트엔드를 S3 + CloudFront로 분리  
→ EC2 부하 감소, 정적 파일 CDN 캐싱, EC2 비용 절감 가능

---

## 서비스별 구성 및 비용

| 서비스 | 용도 | 예상 비용/월 |
|---|---|---|
| EC2 t3.small | Backend + MariaDB + Redis | ~$17 |
| S3 | React 빌드 파일 정적 호스팅 | ~$0.03 |
| CloudFront | S3 앞단 CDN + HTTPS | ~$0 (무료 티어 충분) |
| Elastic IP | EC2 고정 IP | $0 (실행 중 무료) |
| ACM | SSL 인증서 | $0 (무료) |
| Route 53 | 도메인 DNS | ~$0.50 |
| **합계** | | **~$18/월** |

> **ALB 미사용**: 월 ~$16 절감. Nginx가 리버스 프록시 역할 대체.  
> **RDS 미사용**: 월 ~$15~30 절감. MariaDB를 EC2 내 Docker로 운영.

---

## 1단계: EC2 설정

### 인스턴스 스펙

- **타입**: t3.small (2 vCPU, 2GB RAM)
  - Spring Boot + MariaDB + Redis 동시 실행 시 최소 1.5GB 소요
  - t3.micro(1GB)는 메모리 부족 가능성 있음
- **OS**: Amazon Linux 2023 또는 Ubuntu 22.04 LTS
- **스토리지**: 20GB gp3 (기본값)
- **보안 그룹 인바운드**:

| 포트 | 프로토콜 | 허용 대상 | 용도 |
|---|---|---|---|
| 22 | TCP | 내 IP만 | SSH |
| 80 | TCP | 0.0.0.0/0 | HTTP (HTTPS 리다이렉트용) |
| 443 | TCP | 0.0.0.0/0 | HTTPS |

> 8080 포트는 외부에 열지 않는다. Nginx가 443 → 8080으로 프록시.

### Docker + Docker Compose 설치

```bash
# Amazon Linux 2023
sudo yum update -y
sudo yum install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user

# Docker Compose Plugin
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
```

### Nginx 설치 (호스트에 직접 설치 — SSL 처리)

```bash
sudo yum install -y nginx
sudo systemctl enable --now nginx
```

> Nginx를 Docker 컨테이너로 띄우지 않고 호스트에 설치하는 이유:  
> Let's Encrypt certbot이 호스트 파일 시스템에 접근해야 인증서를 발급·갱신하기 때문.

### SSL 인증서 발급 (Let's Encrypt)

```bash
sudo yum install -y certbot python3-certbot-nginx
sudo certbot --nginx -d yourdomain.com
# 자동 갱신 확인
sudo systemctl status certbot-renew.timer
```

### Nginx 설정

`/etc/nginx/conf.d/myblog.conf`:

```nginx
# HTTP → HTTPS 리다이렉트
server {
    listen 80;
    server_name yourdomain.com;
    return 301 https://$host$request_uri;
}

# HTTPS
server {
    listen 443 ssl;
    server_name yourdomain.com;

    ssl_certificate     /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

    # API 요청 → Spring Boot
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 2단계: 백엔드 배포 (Docker Compose)

### 환경변수 파일 생성

EC2에 `backend/.env.prod` 생성 (git에는 절대 커밋하지 않음):

```env
PROFILE=prod
DB_ROOT_PASSWORD=your_root_password
DB_USERNAME=myblog_user
DB_PASSWORD=your_db_password
REDIS_HOST=redis
REDIS_PORT=6379
JWT_SECRET_KEY=your_jwt_secret_256bit
VISITOR_HMAC_SECRET=your_hmac_secret
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

### 배포 명령

```bash
# 코드 클론
git clone https://github.com/your-repo/myblog-boot.git
cd myblog-boot

# 백엔드만 실행 (프론트엔드는 S3로 분리)
docker compose up -d db redis backend
```

### docker-compose.yaml 프론트엔드 서비스 제거

S3로 프론트엔드를 분리하면 `frontend` 서비스는 불필요하므로 제거한다.

---

## 3단계: 프론트엔드 배포 (S3 + CloudFront)

### S3 버킷 설정

```bash
# 버킷 생성 (퍼블릭 액세스 차단 유지 — CloudFront OAC 사용)
aws s3 mb s3://myblog-frontend-prod

# 빌드 후 업로드
cd frontend
VITE_API_URL=https://yourdomain.com npm run build
aws s3 sync dist/ s3://myblog-frontend-prod --delete
```

### CloudFront 설정

| 항목 | 값 |
|---|---|
| Origin | S3 버킷 (OAC 방식) |
| Viewer Protocol Policy | Redirect HTTP to HTTPS |
| Default Root Object | index.html |
| Error Pages | 403/404 → /index.html (SPA 라우팅) |
| Cache Policy | CachingOptimized (정적 파일) |

> **SPA 라우팅 주의**: CloudFront Error Pages에서 403, 404를 `/index.html`로  
> 응답 코드 200으로 리다이렉트 설정해야 React Router가 동작한다.

### 배포 후 캐시 무효화

```bash
aws cloudfront create-invalidation \
  --distribution-id YOUR_DISTRIBUTION_ID \
  --paths "/*"
```

---

## 4단계: DB 백업

RDS 미사용으로 인해 백업을 직접 구성해야 한다.

```bash
# /home/ec2-user/backup.sh
#!/bin/bash
BACKUP_DIR="/home/ec2-user/backups"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

docker exec db mysqldump \
  -u root -p"${DB_ROOT_PASSWORD}" myblog \
  > "$BACKUP_DIR/myblog_$DATE.sql"

# 7일 이상 된 백업 삭제
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete
```

```bash
# crontab -e 에 추가 (매일 새벽 3시)
0 3 * * * /home/ec2-user/backup.sh
```

> S3에 백업 파일을 업로드하면 더 안전하다:  
> `aws s3 cp "$BACKUP_DIR/myblog_$DATE.sql" s3://myblog-backup-bucket/`

---

## 배포 흐름 요약

```
코드 변경
    │
    ├── 백엔드 변경 시
    │     git pull → docker compose build backend → docker compose up -d backend
    │
    └── 프론트엔드 변경 시
          npm run build → aws s3 sync → aws cloudfront create-invalidation
```

---

## 비용 절감 추가 팁

- **EC2 예약 인스턴스 (1년)**: t3.small 기준 온디맨드 대비 약 40% 절감 (~$10/월)
- **EC2 절전**: 개발/테스트 중 사용하지 않을 때 인스턴스 중지 (스토리지 비용만 발생)
- **CloudWatch 알림**: 예상치 못한 비용 급증 감지용 Budget Alert 설정 권장
