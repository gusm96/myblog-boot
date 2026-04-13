# ── Stage 1: 빌드 ─────────────────────────────────────────────
FROM node:18-alpine AS builder
WORKDIR /app

# 의존성 설치 (캐시 레이어 분리)
COPY package*.json ./
RUN npm ci

# 소스 복사
COPY . .

# 빌드 타임 환경변수 주입 (NEXT_PUBLIC_* 은 빌드 시 번들에 포함됨)
ARG NEXT_PUBLIC_API_URL=http://localhost:8080
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL

RUN npm run build

# ── Stage 2: 실행 ──────────────────────────────────────────────
FROM node:18-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production

# standalone 출력물만 복사 (최소 이미지)
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
COPY --from=builder /app/public ./public

EXPOSE 3000

ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

CMD ["node", "server.js"]
