# App Router 리뷰 1회차 — 점검 영역 2 (2-1 ~ 2-4) 계획서 · 렌더링 전략

- **작성일**: 2026-04-20
- **참조 리뷰**: `.claude/reviews/frontend-review-1-app-router.md` (2026-04-19)
- **연계 계획서**:
  - `frontend/docs/260420_app-router-top3-implementation-plan.md` (Top 1: 2-2/2-3 frontend + Top 3: 2-4 loading.tsx) — **이미 구현됨** (`5fe9069`)
  - `backend/docs/260420_revalidate-webhook-plan.md` (2-3 backend webhook 호출자) — **미구현, 내일 작업 예정**
  - `frontend/docs/260420_app-router-review-sections-1-4-plan.md` (점검 영역 1 계획서)
- **범위**: 리뷰 "점검 영역 2" 전 구간 (2-1 페이지별 렌더링 / 2-2 fetch 옵션 / 2-3 on-demand revalidation / 2-4 Streaming + Suspense)
- **성격**: **신규 대규모 구현이 아니라**, 완료 작업의 공식 검증 + 백엔드 webhook 연동 **이후** 수행할 프론트 측 후속 단계 + 관측성 보강

---

## 1. Problem (문제 정의)

### 1.1 리뷰 1회차가 지적한 것 (요약)

| 항목 | 우선순위 | 지적 내용 |
| --- | --- | --- |
| 2-1 `/posts/[slug]` on-demand 미연결 | High | 관리자 CUD 시 TTL 60s~3600s 동안 stale → SEO 지연 (2-3 재기)|
| 2-1 `/management/**` 초기 스피너 | Medium | 서버 prefetch 가능한 구조인데 CSR 전용 |
| 2-1 `sitemap.ts` 의도 주석 부재 | Low | `export const revalidate = 3600` 명시 권장 |
| 2-2 `next.tags` 전무 | High | 전역 무효화 불가 — 구조적 공백 |
| 2-2 `revalidate` 값 근거 미문서화 | Medium | 60/300/3600 혼재, 정책 주석 없음 |
| 2-3 `revalidateTag` / `revalidatePath` 호출 0건 | High | 반쪽 구현 — Top 1 의 핵심 |
| 2-3 Route Handler + secret 검증 | High | 신규 엔드포인트 |
| 2-3 백엔드 `@TransactionalEventListener(AFTER_COMMIT)` | High | **-b 작업** |
| 2-3 `revalidate` TTL 60s → 600s 완화 | Medium | on-demand 도입 후 백엔드 호출 감소 |
| 2-4 `loading.tsx` 부재 | High | 라우트 Streaming 기회 상실 (1-3 에서도 재기) |
| 2-4 Server Component 내부 Suspense | Low | 현재 불필요 — 과한 쪼갬 지양 |

### 1.2 왜 지금 다시 정리하는가

- 위 High/Medium 중 **프론트 측 항목은 모두 구현 완료** (`5fe9069` + 후속 커밋).
- 그러나 **(a) 리뷰 원문 → 구현 파일 매핑이 문서화되지 않음**, **(b) TTL 완화(2-3 Medium)는 backend webhook 완료 후 단계별 롤아웃이 필요**, **(c) Route Handler 의 End-to-End 검증(독립 실행 + backend 연동)이 아직 공식 수행되지 않음**.
- 이 계획서 목적:
  1. 완료 상태를 공식 매핑 → 2회차 리뷰에서 중복 지적 차단.
  2. TTL 완화의 안전한 롤아웃 절차 확정 (backend 연동 완료 후 즉시 적용 가능한 수준까지 설계).
  3. Route Handler 의 **단독 검증 루틴** (backend 미구현 구간에서도 수행 가능) 확립.
  4. 관측성 보강 — `console.info` 수준을 넘는 운영 가시성 확보 여부 결정.

### 1.3 방치 시 리스크

- TTL 60s 유지 시 백엔드 호출 불필요하게 높음 — on-demand 도입의 원래 목적(백엔드 호출 90% 감소)이 무력화.
- Route Handler 단독 검증이 없으면 backend 연동 단계에서 문제 원인이 프론트/백엔드 중 어디인지 구분 불가 → 디버깅 시간 증가.
- 운영 관측 부재 시 revalidate 실패가 "조용한 실패" 로 누적 → stale 컨텐츠 장기 방치 위험.

---

## 2. Analyze (분석 및 선택지 검토)

### 2.1 현재 구현 상태 — 교차 검증

| 리뷰 항목 | 리뷰 권고 | 현재 상태 | 근거 파일 |
| --- | --- | --- | --- |
| 2-1 ISR 전략 (`/`, `/category/[name]`, `/posts/[slug]`) | SSG + ISR 60s | ✅ 유지 | `app/(public)/page.tsx:6`, `app/(public)/category/[name]/page.tsx:8`, `app/(public)/posts/[slug]/page.tsx:10` |
| 2-1 `/boards/[id]` shim | redirects 로 대체 | ✅ 완료 | `next.config.ts:31-41` |
| 2-1 `sitemap.ts` revalidate 명시 | `export const revalidate = 3600` + 주석 | ✅ 완료 | `app/sitemap.ts:4-5` |
| 2-2 모든 fetch 에 `next.tags` | posts/slugs/categories 라벨 | ✅ 완료 (6건 전부) | `lib/api.ts:43,54,66,73,84,91` |
| 2-2 revalidate 정책 주석 | 파일 상단 주석 | ✅ 완료 | `lib/api.ts:6-10` |
| 2-3 Route Handler | secret + `revalidateTag` + `revalidatePath` | ✅ 완료 | `app/api/revalidate/route.ts` |
| 2-3 Next.js 16.x `revalidateTag(tag, "max")` | 2인수 시그니처 | ✅ 완료 | `app/api/revalidate/route.ts:43` |
| 2-3 백엔드 webhook 호출자 | `@TransactionalEventListener(AFTER_COMMIT)` | ❌ **미구현** (`-b scope`) | `backend/docs/260420_revalidate-webhook-plan.md` 존재 |
| 2-3 TTL 완화 (60s → 600s) | posts/slug 기본값 조정 | ❌ **미적용** (backend 완료 후 진행) | `lib/api.ts:41,51,62` 현 60s |
| 2-4 `(public)/loading.tsx` | Streaming fallback | ✅ 완료 | `app/(public)/loading.tsx` |
| 2-4 `posts/[slug]/loading.tsx` | 스켈레톤 | ✅ 완료 | `app/(public)/posts/[slug]/loading.tsx` |
| 2-4 `Suspense` for `useSearchParams` | login/search | ✅ 유지 | `app/login/page.tsx:5-8`, `app/search/page.tsx` |

### 2.2 남은 gap 분석

#### 2.2.1 gap-A: TTL 완화 (2-3 Medium) — **백엔드 webhook 의존**

**현 상태**:
- `getPostList`, `getPostBySlug`, `getCategoryPostList` 기본 revalidate = 60s.
- on-demand webhook 이 **아직 호출되지 않음** → TTL 완화 시 stale 구간이 60s 에서 600s 로 10배 증가.

**완화 조건**:
- 백엔드 계획서 `260420_revalidate-webhook-plan.md` 의 E2E 검증 (§4-3) **통과 후**.
- 양측 로그 페어링이 관찰되고, secret 불일치/네트워크 실패 시나리오가 문서화된 상태.

**결론**: **본 계획서에서 코드 변경은 하지 않고, "조건부 롤아웃 절차" 만 명문화**. 백엔드 완료 시점에 단일 PR 로 일괄 적용.

#### 2.2.2 gap-B: Route Handler 단독 검증 (실행 미수행)

**현 상태**:
- `app/api/revalidate/route.ts` 는 커밋되었으나, 수동 curl/Postman 호출로 **정상 응답 / 에러 응답 / secret 검증 / 빈 body 거부** 등의 독립 검증이 문서화되지 않음.

**조치**:
- backend 없이도 frontend dev 서버만으로 수행 가능한 **검증 스크립트 / 체크리스트** 를 본 계획서에 명시.
- 추후 backend 연동 시 "프론트 단독은 통과했음" 을 전제로 장애 지점을 분리 가능.

#### 2.2.3 gap-C: Route Handler 관측성 (2-3 보강 제안)

**현 상태**:
- `console.info("[revalidate] tags=%o paths=%o", ...)` 로 성공만 기록.
- **실패 경로** (401 / 400 / 500) 는 응답은 정확하나 로그 레벨 구분 없음.
- 구조화 로그 (JSON) 가 아니라 grep 이나 장기 분석에 불리.

**선택지 비교**:

| 선택지 | 설명 | 판단 |
| --- | --- | --- |
| A. 현 상태 유지 | 최소 로그만 | ⚠️ 운영 가시성 부족 — 실패가 "조용한 실패"로 누적될 수 있음 |
| B. 레벨 분리 + 구조화 JSON | `log.warn` / `log.info` 구분 + JSON 한 줄 | ✅ 최소 비용으로 grep/집계 용이 |
| C. 외부 로깅 서비스 연동 (Datadog 등) | 중앙 집중화 | ✗ 운영 인프라 미도입 상태. YAGNI |

**채택: B** — `console.info` / `console.warn` 분리 + 객체 포맷.

#### 2.2.4 gap-D: `/management/**` 서버 prefetch (2-1 Medium)

- 리뷰 원문은 "**2회차 TanStack Query 와 묶어 처리**" 로 명시적 이월.
- 본 계획서 범위 외 — 2회차에서 재논의.

#### 2.2.5 gap-E: `/search` 페이지의 렌더링 적합성 재확인

- 리뷰에서는 "로그인/동적 질의 특성상 CSR 로 격리" 를 합리적으로 평가.
- 현재 `app/search/page.tsx` 검증 결과: `<Suspense>` 로 감싼 뒤 Client content — **리뷰 권고와 일치**.
- 변경 불필요. 본 계획서는 이 판단을 재확인만.

### 2.3 핵심 트레이드오프

| 트레이드오프 | 선택 | 근거 |
| --- | --- | --- |
| TTL 완화 시점 | backend E2E 검증 완료 후 | stale 폭을 10배로 늘리는 변경 → 반드시 선행 조건 충족 |
| 완화 폭 (600s vs 3600s) | **600s** 먼저 | 600s 는 "최악의 경우 10분" — 운영 관측 확보 후 3600s 단계 판단 |
| 구조화 로그 도입 수준 | 최소한으로 (B안) | 외부 서비스 없이 console 레벨 분리만 |
| Route Handler 에 rate-limit 추가 | 불필요 | secret 기반 인증 + Docker 내부 네트워크 전제. 공개 엔드포인트 아님 |

---

## 3. Action (구현 계획 및 설계)

### 3.1 목표 및 범위

- **In scope (본 계획서에서 즉시 수행)**:
  1. **gap-B**: Route Handler 단독 검증 절차 실행 + 결과 기록.
  2. **gap-C**: `app/api/revalidate/route.ts` 로깅 레벨 분리 + 구조화 JSON.
- **Conditional (backend webhook 완료 후 수행)**:
  3. **gap-A**: TTL 완화 단계별 롤아웃 — 본 계획서에서 절차만 정의, 실행은 후속.
- **Out of scope**:
  - `/management/**` 서버 prefetch (2회차 이월).
  - `getPostList` 이하 함수의 시그니처 변경 (호출 측 일괄 수정 필요 → 별도 작업).
  - Route Handler rate limiting / 인증 체계 확장.

### 3.2 구현 접근

#### 3.2.1 gap-C — Route Handler 로깅 보강

**변경 대상**: `frontend/app/api/revalidate/route.ts`

**현재 로그**:
```ts
console.info("[revalidate] tags=%o paths=%o", tags, paths);
```

**변경안**:
```ts
// 성공 경로
console.info(
  JSON.stringify({
    event: "revalidate.ok",
    tags,
    paths,
    at: new Date().toISOString(),
  }),
);

// 실패 경로 (각 분기에 추가)
console.warn(
  JSON.stringify({
    event: "revalidate.unauthorized",
    reason: "invalid secret",
    at: new Date().toISOString(),
  }),
);

console.warn(
  JSON.stringify({
    event: "revalidate.bad_request",
    reason: "tags or paths required" | "invalid json body",
    at: new Date().toISOString(),
  }),
);

console.error(
  JSON.stringify({
    event: "revalidate.misconfig",
    reason: "REVALIDATE_SECRET env missing",
    at: new Date().toISOString(),
  }),
);
```

**원칙**:
- secret 값 자체는 절대 로그에 포함하지 않음 (access log 방어).
- `event` 필드로 grep/집계 가능.
- 라인당 1 JSON — Docker 로그 드라이버 친화.

#### 3.2.2 gap-B — 단독 검증 체크리스트 (스크립트 포함)

**전제**: `npm run dev` 로 프론트만 기동 (백엔드 미기동 OK). `.env.local` 에 `REVALIDATE_SECRET=test-secret` 설정.

| # | 시나리오 | curl 명령 | 기대 결과 |
| --- | --- | --- | --- |
| V1 | 올바른 tags 요청 | `curl -i -X POST http://localhost:3000/api/revalidate -H "x-revalidate-secret: test-secret" -H "Content-Type: application/json" -d '{"tags":["posts"]}'` | 200 + `{ok:true, tags:["posts"], paths:[]}` |
| V2 | 올바른 paths 요청 | `-d '{"paths":["/sitemap.xml"]}'` | 200 + `{ok:true, tags:[], paths:["/sitemap.xml"]}` |
| V3 | tags + paths 동시 | `-d '{"tags":["posts","slugs"],"paths":["/sitemap.xml"]}'` | 200 + 전부 포함 |
| V4 | secret 불일치 | `-H "x-revalidate-secret: wrong"` | 401 + `{ok:false, error:"invalid secret"}` |
| V5 | secret 헤더 없음 | 헤더 제거 | 401 |
| V6 | body 빈 객체 | `-d '{}'` | 400 + `{ok:false, error:"tags or paths required"}` |
| V7 | 잘못된 JSON | `-d 'not-json'` | 400 + `{ok:false, error:"invalid json body"}` |
| V8 | GET 메서드 | `-X GET` | 405 (Next.js 기본) |
| V9 | `REVALIDATE_SECRET` 미설정 (`.env.local` 주석) | 재기동 후 정상 요청 | 500 + `{ok:false, error:"server misconfiguration"}` |

**기록**: 각 시나리오의 HTTP status + response body + 로그 라인을 본 계획서 §5 에 기록.

#### 3.2.3 gap-A — TTL 완화 롤아웃 절차 (backend 완료 후)

**전제 조건 체크리스트**:
- [ ] 백엔드 `RevalidateWebhookListenerTest` 통과
- [ ] 백엔드 수동 E2E (backend plan §4-3) — 글 발행 시 프론트 로그 `revalidate.ok` 확인
- [ ] secret 불일치 / 네트워크 실패 시 백엔드 `[revalidate] failed` 로그 관찰

**단계 1 — 기본값만 변경 (conservative)**:
```ts
// lib/api.ts
export function getPostList(page = 1, revalidate = 600 /* was 60 */) { ... }
export function getPostBySlug(slug: string, revalidate = 600 /* was 60 */) { ... }
export function getCategoryPostList(categoryName: string, page = 1, revalidate = 600 /* was 60 */) { ... }
// getAllSlugs (3600) 유지 — on-demand 가 sitemap 즉시 무효화
// getCategoriesV2 / getCategories (300) 유지 — 현 값 적정
```

**단계 2 — 호출 지점 정리**:
- `app/(public)/page.tsx:11` — `getPostList(1, 60)` → `getPostList(1)` (기본값 사용) 또는 `getPostList(1, 600)` 명시.
- `app/(public)/category/[name]/page.tsx:43` — 동일.
- 각 페이지의 `export const revalidate = 60` 도 600 으로 변경 (ISR 은 페이지 레벨 값과 fetch revalidate 중 더 작은 값이 우선).

**단계 3 — 관측 (48시간)**:
- 프론트 로그에서 `revalidate.ok` 빈도 확인 — 쓰기 액션 수와 일치해야 함.
- 백엔드 access 로그에서 게시글 조회 API 호출 빈도 감소 확인.

**단계 4 — 필요 시 추가 완화**:
- 48시간 관측 후 문제가 없으면 `posts` TTL 을 1800s 까지 확대 검토.
- 본 계획 범위 밖 — 별도 Ops 회차.

**롤백 절차**:
- 문제 발생 시 `lib/api.ts` 기본값을 60s 로 되돌리는 단일 PR.
- 호출 지점은 명시적으로 TTL 을 넘기지 않으므로 기본값 롤백만으로 즉시 복구.

### 3.3 변경 대상 파일 (즉시 수행 분)

| 파일 | 변경 유형 | 설명 |
| --- | --- | --- |
| `frontend/app/api/revalidate/route.ts` | 수정 | 로깅 구조화 + 레벨 분리 |
| `frontend/docs/260420_rendering-strategy-review-sections-2-1-4-plan.md` | 본 파일 | — |

### 3.4 주요 트레이드오프

- **구조화 로그 도입**: `JSON.stringify` 호출 비용은 무시 가능 (요청 빈도 저, per-CUD 1회). 가독성 약간 저하되지만 grep/집계 이득.
- **TTL 완화 지연**: backend 완료까지 60s 유지 → 관리자 UX 는 기존과 동일. 조기 완화보다 안전.
- **로깅 라이브러리 미도입 (pino/winston 등)**: console 레벨 분리만으로 충분. 라이브러리 추가는 별도 과제.

### 3.5 예상 이슈 및 대응

| 이슈 | 대응 |
| --- | --- |
| `JSON.stringify` 가 `undefined` 필드를 누락해 로그 필드 유실 | 모든 필드에 명시적 값 할당 (선택 필드는 `null` 또는 `[]`) |
| `console.error` 가 Next.js 에서 표준 에러 스트림으로 나가 docker log 에 색상 코드 삽입 | `next dev` 는 ok, 프로덕션 Docker 는 standalone → 색상 없음. 추가 조치 불필요 |
| `.env.local` 변경 후 Next 재기동 누락 | V9 시나리오 체크리스트에 "재기동 후" 명기 |
| Windows bash 에서 curl 작동 확인 | PowerShell/WSL 모두 지원. 본 가이드는 `/Users/gusm9/Documents/...` WSL 스타일 curl 사용 |

---

## 4. Result (검증 계획)

### 4.1 성공 기준 (즉시 수행 분)

- [ ] `app/api/revalidate/route.ts` 4개 경로 (ok / unauthorized / bad_request / misconfig) 모두 JSON 구조화 로그 출력.
- [ ] 단독 검증 V1 ~ V9 시나리오 **전부 pass**.
- [ ] secret 값이 어떤 로그 경로에도 노출되지 않음 (grep 확인).
- [ ] `npm run build` 성공, 타입 에러 0건.

### 4.2 검증 절차

```bash
cd frontend

# 1. .env.local 에 REVALIDATE_SECRET=test-secret 설정 확인

# 2. dev 서버 기동
npm run dev

# 3. 별도 터미널에서 §3.2.2 의 V1 ~ V9 순차 실행
#    각 요청의 status + body + 콘솔 로그 라인 기록

# 4. .env.local 의 REVALIDATE_SECRET 라인 주석 처리 후 재기동 → V9 실행

# 5. 빌드 확인
npm run build
```

### 4.3 성공 기준 (조건부 — backend 연동 후)

- [ ] backend 계획서 E2E(§4-3) 통과 — frontend 로그 `revalidate.ok` + backend 로그 페어링.
- [ ] TTL 완화 후 48시간 동안 stale 불만 제보 0건.
- [ ] 백엔드 게시글 조회 API 호출 빈도가 최소 50% 이상 감소 (log 기반 집계).

### 4.4 회귀 체크

- `lib/api.ts` 의 함수 시그니처 변경 없음 → 호출 측 빌드 영향 없음.
- Route Handler 의 응답 JSON 스키마 불변 (`{ok, tags, paths, revalidatedAt}`) → backend 호출자 계약 유지.
- `revalidateTag(tag, "max")` Next.js 16.x 2-인수 시그니처 유지 — 하위 호환 문제 없음.

### 4.5 관측 지표 (선택)

- **Route Handler 처리량**: 성공(`revalidate.ok`) 건수 / 시간.
- **실패율**: `revalidate.unauthorized` + `revalidate.bad_request` / 전체.
- **latency**: `revalidatedAt - requestStart` (추후 측정 도입 시).

---

## 5. 검증 결과 (실행 후 채울 것)

<!-- V1 ~ V9 실행 결과 기록
| 시나리오 | HTTP | Response Body | 로그 event | 결과 |
| --- | --- | --- | --- | --- |
| V1 |  |  |  | ⏳ |
...
-->

---

## 6. 차기 회차 이월

| 항목 | 이월 회차 | 사유 |
| --- | --- | --- |
| `/management/**` 서버 prefetch + HydrationBoundary | 2회차 TanStack Query | 리뷰 원문에서 명시 이월 |
| `useSuspenseQuery` 도입 | 2회차 TanStack Query | 특수 파일 기반 확보 후 검토 |
| `generateMetadata` / Open Graph / sitemap 세부 | 2회차 SEO | 별도 영역 |
| Route Handler 에 pino/winston 등 로깅 라이브러리 | Ops 회차 | 운영 관측 인프라 도입 시 |
| TTL 1800s / 3600s 확대 | Ops 회차 | 600s 에서 48h 관측 후 |
| Webhook 응답 latency 측정 | Ops 회차 | 관측 인프라 |

---

## 7. 체크리스트 (구현 세션용)

### 즉시 수행
- [ ] `app/api/revalidate/route.ts` 4개 분기 구조화 로그 적용
- [ ] V1 ~ V9 단독 검증 실행 및 결과 §5 에 기록
- [ ] `npm run build` 통과 확인
- [ ] secret 노출 재검토 (로그 경로 전수 확인)
- [ ] 사용자에게 변경 요약 + 커밋 확인 요청 (자동 커밋 금지)

### 조건부 — backend webhook 완료 후
- [ ] backend E2E 선행 통과 확인 (backend plan §4-3)
- [ ] `lib/api.ts` 기본 revalidate 값 60 → 600 (3개 함수)
- [ ] 페이지 레벨 `export const revalidate` 값 60 → 600 (home, category, post)
- [ ] 48시간 관측 후 성공 기준 §4.3 체크
