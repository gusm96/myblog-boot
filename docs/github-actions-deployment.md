# GitHub Actions 배포 자동화 전략

> 작성일: 2026-03-19
> 대상: myblog-boot 모노레포
> 목표: Jenkins → GitHub Actions 전환 / Frontend(S3) + Backend(EC2) 독립 배포

---

## 1. 개요

### 1-1. 배포 아키텍처

```
[GitHub monorepo]
  ├── frontend/   ─── push → GitHub Actions ──► npm build ──► aws s3 sync  ──► S3 (정적 호스팅)
  └── backend/    ─── push → GitHub Actions ──► gradle build → Docker build → EC2 SSH 배포
```

### 1-2. 핵심 원칙: 경로 기반 트리거(Path Filter)

모노레포에서 `frontend/`와 `backend/`가 완전히 독립적으로 배포되도록 **`paths` 필터**를 사용한다.
`paths` 필터는 해당 경로 하위 파일이 변경된 커밋이 push될 때만 워크플로를 실행한다.

```yaml
on:
  push:
    branches: [master]
    paths:
      - 'frontend/**'   # frontend 디렉토리 변경 시에만 실행
```

| 변경된 파일 | 실행되는 워크플로 |
|---|---|
| `frontend/src/**` | frontend-deploy만 실행 |
| `backend/src/**` | backend-deploy만 실행 |
| `frontend/**` + `backend/**` 동시 변경 | 두 워크플로 병렬 실행 |
| `README.md`, 루트 파일만 변경 | 둘 다 실행 안 됨 |

---

## 2. 워크플로 구성 파일 목록

```
.github/
└── workflows/
    ├── ci.yml                # PR 시 테스트 자동 실행 (frontend + backend 공통)
    ├── frontend-deploy.yml   # master push + frontend 변경 → S3 배포
    └── backend-deploy.yml    # master push + backend 변경 → EC2 배포
```

---

## 3. CI 워크플로 — 테스트 자동화

**파일:** `.github/workflows/ci.yml`

PR을 열거나 `develop` 브랜치에 push할 때 변경된 쪽(frontend/backend)만 테스트를 실행한다.
Branch Protection Rule과 연동하여 CI 통과 없이 master 머지를 막을 수 있다.

```yaml
name: CI

on:
  pull_request:
    branches: [master, develop]
  push:
    branches: [develop]

jobs:
  # ── 변경 경로 감지 ───────────────────────────────────────────────
  # dorny/paths-filter로 어떤 디렉토리가 변경됐는지 먼저 판별한다.
  # 이 결과를 이후 job의 if 조건에서 참조한다.
  changes:
    name: Detect Changes
    runs-on: ubuntu-latest
    outputs:
      backend: ${{ steps.filter.outputs.backend }}
      frontend: ${{ steps.filter.outputs.frontend }}
    steps:
      - uses: actions/checkout@v4
      - uses: dorny/paths-filter@v3
        id: filter
        with:
          filters: |
            backend:
              - 'backend/**'
            frontend:
              - 'frontend/**'

  # ── Backend 테스트 ──────────────────────────────────────────────
  backend-test:
    name: Backend Test
    runs-on: ubuntu-latest
    needs: changes
    # backend 디렉토리 변경이 있을 때만 실행
    if: needs.changes.outputs.backend == 'true'
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      # Gradle 캐시: ~/.gradle/caches + ~/.gradle/wrapper
      # key는 OS + *.gradle 파일 해시 기반 → 의존성 변경 시 자동 갱신
      - uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('backend/**/*.gradle*', 'backend/**/gradle-wrapper.properties') }}
          restore-keys: |
            ${{ runner.os }}-gradle-

      - name: Grant execute permission for gradlew
        run: chmod +x backend/gradlew

      - name: Run backend tests
        working-directory: backend
        run: ./gradlew test
        # GitHub Actions ubuntu runner에는 Docker가 기본 설치되어 있어
        # Testcontainers(Redis)가 별도 설정 없이 동작한다

      - name: Upload test report on failure
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: backend-test-report
          path: backend/build/reports/tests/test/

  # ── Frontend 테스트 / 빌드 검증 ────────────────────────────────
  frontend-test:
    name: Frontend Build Check
    runs-on: ubuntu-latest
    needs: changes
    # frontend 디렉토리 변경이 있을 때만 실행
    if: needs.changes.outputs.frontend == 'true'
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        run: npm ci --prefix frontend

      - name: Build check
        run: npm run build --prefix frontend
        env:
          REACT_APP_API_URL: ${{ vars.REACT_APP_API_URL }}
          # CI=false: CRA의 warning-as-error 비활성화
          CI: 'false'
```

> **참고 — `paths` 필터와 PR 필수 체크 주의사항**
> 워크플로 트리거에 `paths` 필터를 사용하면 해당 경로에 변경이 없을 때 워크플로 자체가 실행되지 않는다.
> Branch Protection의 "Required status checks"에 등록한 job인데 실행이 건너뛰어지면 PR이 영구 차단될 수 있다.
> CI 워크플로는 이를 방지하기 위해 **트리거 수준 `paths` 필터 대신 `dorny/paths-filter`로 내부에서 조건 분기**한다.
> CD 워크플로(`frontend-deploy`, `backend-deploy`)는 Required checks에 등록하지 않으므로 트리거 `paths` 필터를 그대로 사용한다.

---

## 4. Frontend 배포 워크플로 — S3

**파일:** `.github/workflows/frontend-deploy.yml`

`master` 브랜치에서 `frontend/` 하위 파일 변경 시 실행된다.

```yaml
name: Frontend Deploy (S3)

on:
  push:
    branches: [master]
    paths:
      - 'frontend/**'

# production 환경 보호 규칙(필요 시 수동 승인)이 적용된다
# Secrets는 환경(environment) 단위로 격리 관리된다
jobs:
  deploy:
    name: Build & Deploy to S3
    runs-on: ubuntu-latest
    environment: production   # GitHub Environment 사용 → 배포 보호 규칙 + Secrets 격리

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        run: npm ci --prefix frontend

      - name: Build
        run: npm run build --prefix frontend
        env:
          # 빌드 시 환경변수로 백엔드 API 주소를 주입
          REACT_APP_API_URL: ${{ vars.REACT_APP_API_URL }}
          CI: 'false'

      # AWS 자격증명 설정
      # 옵션 A: IAM User (Access Key) ← 초기 설정이 간단
      # 옵션 B: OIDC (권장, 장기 키 불필요 → 4절 참고)
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ vars.AWS_REGION }}

      # frontend/build/ 를 S3 버킷에 동기화
      # --delete: S3에만 있고 로컬에 없는 파일 자동 삭제
      - name: Deploy to S3
        run: |
          aws s3 sync frontend/build/ s3://${{ vars.S3_BUCKET_NAME }} \
            --delete \
            --cache-control "public, max-age=31536000, immutable"

      # CloudFront 사용 시 캐시 무효화 (없으면 이 step 제거)
      - name: Invalidate CloudFront cache
        if: ${{ vars.CLOUDFRONT_DISTRIBUTION_ID != '' }}
        run: |
          aws cloudfront create-invalidation \
            --distribution-id ${{ vars.CLOUDFRONT_DISTRIBUTION_ID }} \
            --paths "/*"
```

### S3 정적 웹 호스팅 설정 요약

```
버킷 정책: Public 읽기 허용 (또는 CloudFront OAC 사용)
정적 웹 호스팅: 활성화 (인덱스: index.html, 오류: index.html)
SPA 라우팅: 오류 문서도 index.html로 설정해야 React Router가 정상 동작
```

---

## 5. Backend 배포 워크플로 — EC2

**파일:** `.github/workflows/backend-deploy.yml`

`master` 브랜치에서 `backend/` 하위 파일 변경 시 실행된다.
빌드 → Docker 이미지 → GHCR 푸시 → EC2 SSH 접속 → 컨테이너 교체 순서로 진행한다.

```yaml
name: Backend Deploy (EC2)

on:
  push:
    branches: [master]
    paths:
      - 'backend/**'

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}/backend   # e.g. gusm96/myblog-boot/backend

jobs:
  # ── 1단계: 빌드 + 이미지 푸시 ──────────────────────────────────
  build:
    name: Build & Push Image
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write   # GHCR 푸시 권한

    outputs:
      image_tag: ${{ steps.meta.outputs.tags }}

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('backend/**/*.gradle*', 'backend/**/gradle-wrapper.properties') }}
          restore-keys: |
            ${{ runner.os }}-gradle-

      - name: Grant execute permission for gradlew
        run: chmod +x backend/gradlew

      # 테스트를 포함하여 JAR 빌드
      # Testcontainers: ubuntu runner에 Docker 기본 설치 → 그대로 동작
      - name: Build JAR
        working-directory: backend
        run: ./gradlew build

      - name: Log in to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}   # 별도 Secret 불필요

      # 이미지 태그: latest + 커밋 SHA (롤백 시 특정 버전 지정 가능)
      - name: Extract Docker metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=raw,value=latest
            type=sha,format=short

      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: ./backend
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          # 레이어 캐시: 변경 없는 레이어 재사용 → 빌드 시간 단축
          cache-from: type=gha
          cache-to: type=gha,mode=max

  # ── 2단계: EC2 배포 ─────────────────────────────────────────────
  deploy:
    name: Deploy to EC2
    runs-on: ubuntu-latest
    needs: build   # build job 성공 후에만 실행
    environment: production

    steps:
      - name: Deploy via SSH
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USERNAME }}
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            IMAGE="${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest"

            # GHCR 로그인 (EC2에서 private 패키지 pull 시 필요)
            echo "${{ secrets.GHCR_TOKEN }}" | docker login ghcr.io \
              -u ${{ github.actor }} --password-stdin

            # 최신 이미지 Pull
            docker pull $IMAGE

            # 기존 컨테이너 중지 및 제거
            docker stop myblog-backend || true
            docker rm   myblog-backend || true

            # 새 컨테이너 실행
            # --env-file: 서버에 직접 관리하는 환경변수 파일 (DB 비밀번호 등 민감 정보)
            docker run -d \
              --name myblog-backend \
              --restart unless-stopped \
              -p 8080:8080 \
              --env-file /home/${{ secrets.EC2_USERNAME }}/.env.prod \
              $IMAGE

            # 헬스체크 (최대 60초 대기)
            echo "Waiting for health check..."
            for i in $(seq 1 30); do
              if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
                echo "✅ Health check passed"
                exit 0
              fi
              sleep 2
            done
            echo "❌ Health check failed"
            exit 1
```

> **GHCR vs Docker Hub**
> GHCR(GitHub Container Registry)은 `GITHUB_TOKEN`만으로 푸시가 가능해 별도 외부 계정이 불필요하다.
> 단, EC2에서 Pull 시 Personal Access Token(패키지 read 권한)을 `GHCR_TOKEN` Secret에 등록해야 한다.
> (패키지가 public이면 토큰 불필요)

---

## 6. GitHub Secrets & Variables 설정

GitHub Repository → **Settings → Secrets and variables → Actions** 에서 등록한다.

### 6-1. Repository Variables (비민감 설정값)

`vars.*` 로 참조. 공개 설정값은 Variables에 저장한다.

| Variable | 값 예시 | 용도 |
|---|---|---|
| `AWS_REGION` | `ap-northeast-2` | AWS 리전 |
| `S3_BUCKET_NAME` | `myblog-frontend-prod` | S3 버킷 이름 |
| `CLOUDFRONT_DISTRIBUTION_ID` | `EXXXXXXXXXXXXX` | CloudFront 배포 ID (없으면 공란) |
| `REACT_APP_API_URL` | `https://api.your-domain.com` | 프론트 API URL |

### 6-2. Repository Secrets (민감 정보)

`secrets.*` 로 참조. 값은 등록 후 재조회 불가.

| Secret | 설명 |
|---|---|
| `AWS_ACCESS_KEY_ID` | IAM 사용자 Access Key |
| `AWS_SECRET_ACCESS_KEY` | IAM 사용자 Secret Key |
| `EC2_HOST` | EC2 퍼블릭 IP 또는 도메인 |
| `EC2_USERNAME` | EC2 SSH 접속 사용자 (예: `ec2-user`) |
| `EC2_SSH_KEY` | EC2 SSH 개인키 전체 내용 (PEM) |
| `GHCR_TOKEN` | GitHub PAT (패키지 read 권한, EC2 Pull용) |

### 6-3. GitHub Environments 활용 (권장)

**Settings → Environments → New environment → `production`** 으로 환경을 만들면:

- **배포 보호 규칙**: 특정 reviewer 승인 후에만 배포 진행
- **Secrets 격리**: environment 단위 Secrets → Repository Secrets와 분리
- **배포 이력**: GitHub UI에서 각 environment 별 배포 기록 추적 가능
- **브랜치 제한**: `master` 브랜치에서만 production 환경 배포 허용

```yaml
jobs:
  deploy:
    environment: production   # 이 한 줄로 보호 규칙 + Secrets 격리 적용
```

---

## 7. AWS IAM 권한 설정

### 7-1. Frontend 배포용 IAM 정책 (S3)

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::myblog-frontend-prod",
        "arn:aws:s3:::myblog-frontend-prod/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "cloudfront:CreateInvalidation"
      ],
      "Resource": "arn:aws:cloudfront::ACCOUNT_ID:distribution/DIST_ID"
    }
  ]
}
```

### 7-2. (고급) OIDC 연동으로 장기 키 제거

IAM User의 Access Key 대신 **GitHub OIDC Provider**를 사용하면
임시 자격증명(STS AssumeRoleWithWebIdentity)을 발급받아 **장기 키를 저장하지 않아도** 된다.

```yaml
# 워크플로에서 사용할 권한 선언
permissions:
  id-token: write   # JWT 요청 필수
  contents: read

- name: Configure AWS credentials (OIDC)
  uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: arn:aws:iam::ACCOUNT_ID:role/GitHubActionsRole
    aws-region: ap-northeast-2
```

**AWS 콘솔에서 설정:**
1. IAM → Identity providers → GitHub OIDC Provider 추가
   (`token.actions.githubusercontent.com`)
2. IAM Role 생성 → Trust policy에 Repository + Environment 조건 설정

```json
"Condition": {
  "StringEquals": {
    "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
    "token.actions.githubusercontent.com:sub":
      "repo:gusm96/myblog-boot:environment:production"
  }
}
```

> 초기에는 IAM User Key 방식으로 시작하고, 이후 OIDC로 마이그레이션하는 것을 권장한다.

---

## 8. EC2 사전 설정

배포 전 EC2 인스턴스에 아래 항목을 준비한다.

```bash
# Docker 설치 (Amazon Linux 2023 기준)
sudo yum install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user   # 재로그인 필요

# 환경변수 파일 생성 (서버에서 직접 관리, Git에 포함하지 않음)
cat > ~/.env.prod << 'EOF'
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mariadb://RDS_ENDPOINT:3306/myblog
DB_USERNAME=...
DB_PASSWORD=...
REDIS_HOST=ELASTICACHE_ENDPOINT
VISITOR_HMAC_SECRET=...
EOF
chmod 600 ~/.env.prod
```

### EC2 보안 그룹 규칙

| 유형 | 포트 | 소스 | 용도 |
|---|---|---|---|
| SSH | 22 | GitHub Actions IP 대역 (또는 내 IP) | 배포용 SSH |
| HTTP | 8080 | 0.0.0.0/0 (또는 ALB SG) | Spring Boot API |

> GitHub Actions runner IP 범위: `https://api.github.com/meta` 의 `actions` 필드 참고
> 보안을 위해 SSH는 GitHub Actions IP만 허용하거나, **AWS Systems Manager Session Manager**를 사용해 SSH 포트 자체를 닫는 것을 고려한다.

---

## 9. Jenkins에서 GitHub Actions로 마이그레이션 체크리스트

```
[ ] .github/workflows/ 디렉토리 생성
[ ] ci.yml 작성 및 push → PR에서 테스트 실행 확인
[ ] frontend-deploy.yml 작성
[ ] backend-deploy.yml 작성
[ ] GitHub Repository Secrets 등록
    [ ] AWS_ACCESS_KEY_ID
    [ ] AWS_SECRET_ACCESS_KEY
    [ ] EC2_HOST
    [ ] EC2_USERNAME
    [ ] EC2_SSH_KEY
    [ ] GHCR_TOKEN
[ ] GitHub Repository Variables 등록
    [ ] AWS_REGION
    [ ] S3_BUCKET_NAME
    [ ] REACT_APP_API_URL
[ ] GitHub Environment (production) 생성 및 보호 규칙 설정
[ ] AWS IAM 사용자 생성 + S3/CloudFront 정책 연결
[ ] S3 버킷 생성 + 정적 웹 호스팅 활성화
[ ] EC2 보안 그룹 SSH 규칙 확인
[ ] EC2 ~/.env.prod 파일 생성
[ ] EC2에서 GHCR 로그인 테스트 (docker pull ghcr.io/...)
[ ] master에 더미 커밋 push → Actions 탭에서 워크플로 실행 확인
[ ] 기존 Jenkinsfile 제거 (마이그레이션 완료 후)
[ ] Branch Protection Rule: CI 통과 필수로 설정
```

---

## 10. 전체 흐름 요약

```
개발자 push to master
        │
        ├─ frontend/** 변경 있음? ──► frontend-deploy.yml
        │                               │
        │                         npm ci + build
        │                               │
        │                         aws s3 sync → S3 버킷
        │                               │
        │                         CloudFront 캐시 무효화
        │
        └─ backend/** 변경 있음? ──► backend-deploy.yml
                                        │
                                  gradle build (테스트 포함)
                                        │
                                  docker build + push → GHCR
                                        │
                                  EC2 SSH 접속
                                        │
                                  docker pull + run
                                        │
                                  /actuator/health 확인
```
