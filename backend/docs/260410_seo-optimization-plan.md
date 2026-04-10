# SEO 최적화 계획서

> 작성일: 2026-04-10  
> 대상: myblog-boot 개인 블로그 플랫폼 (Spring Boot 3.0.4 + React SPA)  
> 목표: Google / Naver 검색엔진 인덱싱 최적화

---

## 1. 개요

현재 myblog-boot은 **순수 SPA(React) + REST API** 구조로, 검색엔진 크롤러가 콘텐츠를 인덱싱하기 극히 어려운 상태이다. 블로그의 핵심 가치는 **작성한 글이 검색에 노출되는 것**인데, 현재 아키텍처에는 SEO를 위한 장치가 전무하다.

이 계획서는 백엔드 관점에서 SEO-Ready 시스템을 구축하기 위해 필요한 변경 사항을 단계별로 정리한다.

---

## 2. 현재 문제 분석

### 2.1 치명적 (Critical)

| # | 문제 | 상세 |
|---|------|------|
| C-1 | **SPA 렌더링 — 크롤러 접근 불가** | React CSR 구조로, `index.html`의 `<div id="root"></div>`만 존재. Googlebot은 JS 실행을 시도하지만 렌더링 예산(crawl budget)이 제한적이고, **Naver 크롤러는 JavaScript를 실행하지 않는다**. 결과적으로 모든 게시글 콘텐츠가 인덱싱 불가. |
| C-2 | **Slug 없는 URL 구조** | 게시글 URL이 `/boards/42`(숫자 ID)로 구성됨. 검색엔진은 URL 자체를 콘텐츠 시그널로 활용하는데, 숫자 ID는 **의미 없는 URL**로 평가됨. `/posts/spring-boot-redis-cache-strategy` 같은 slug 기반 URL 대비 CTR과 랭킹에서 불리. |
| C-3 | **sitemap.xml 없음** | 검색엔진에게 크롤링할 페이지 목록을 알려주는 sitemap이 존재하지 않음. 신규 게시글 발행 시 검색엔진이 발견하기까지 불필요한 시간 소요. |
| C-4 | **Meta 태그 부재** | `index.html`의 description이 `"Web site created using create-react-app"`으로 기본값 그대로. Open Graph / Twitter Card 메타 태그 없음. 페이지별 동적 title/description 불가. |

### 2.2 중요 (Important)

| # | 문제 | 상세 |
|---|------|------|
| I-1 | **robots.txt 없음** | 크롤러 접근 정책과 sitemap 위치를 명시하는 `robots.txt`가 없음. |
| I-2 | **RSS/Atom 피드 없음** | 블로그 구독 및 콘텐츠 신디케이션을 위한 피드가 없음. Naver 블로그 검색 등록 시 RSS 피드가 활용됨. |
| I-3 | **canonical URL 없음** | 동일 콘텐츠가 여러 URL로 접근 가능할 경우(쿼리 파라미터 등) 중복 콘텐츠로 판단될 수 있음. |
| I-4 | **구조화된 데이터(Structured Data) 없음** | `JSON-LD` 형식의 `BlogPosting` 스키마가 없어 Rich Snippet(작성일, 작성자 등) 표시 불가. |
| I-5 | **Board 엔티티에 SEO 필드 부재** | `slug`, `metaDescription`, `metaKeywords`, `thumbnailUrl` 등 SEO에 필요한 필드가 Entity/DTO 어디에도 없음. |
| I-6 | **API 응답에 카테고리명 없음** | `BoardDetailResDto`에 카테고리 정보가 포함되지 않아, 프론트엔드에서 breadcrumb 네비게이션이나 카테고리 기반 구조화된 데이터 생성이 불가. |

### 2.3 경미 (Minor)

| # | 문제 | 상세 |
|---|------|------|
| M-1 | **HTTP 캐시 헤더 미설정** | 게시글 API에 `Cache-Control`, `ETag`, `Last-Modified` 헤더가 없음. 크롤러 재방문 시 불필요한 전체 응답 반환. |
| M-2 | **`<html lang="en">`** | `index.html`의 lang 속성이 `en`으로 설정되어 있으나, 한국어 블로그이므로 `ko`가 적절. |
| M-3 | **페이지네이션 SEO 미지원** | 게시글 목록의 페이지네이션에서 `rel="prev"` / `rel="next"` 또는 이에 상응하는 API 응답이 없음. |
| M-4 | **이미지 alt 텍스트 관리 부재** | `ImageFile` 엔티티에 `alt` 텍스트 필드가 없어, 이미지 검색 최적화 불가. |

---

## 3. 제안 변경 사항

### 3.1 URL 구조 개편

**현재:**
```
GET /api/v1/boards              → 목록
GET /api/v8/boards/{boardId}    → 상세 (숫자 ID)
```

**제안:**
```
GET /api/v1/posts               → 목록
GET /api/v1/posts/{slug}        → 상세 (slug 기반)
GET /api/v1/posts/{slug}        → fallback: slug 없는 기존 글은 ID로도 접근 가능
```

- 프론트엔드 URL도 `/boards/42` → `/posts/spring-boot-seo-guide` 형태로 변경
- 기존 ID 기반 URL은 301 리다이렉트로 slug URL로 전환 (SEO 점수 이전)

### 3.2 데이터 모델 개선

**Board 엔티티 추가 필드:**

| 필드 | 타입 | 용도 |
|------|------|------|
| `slug` | `VARCHAR(255) UNIQUE` | URL 식별자 (타이틀 기반 자동 생성 + 수동 편집 가능) |
| `metaDescription` | `VARCHAR(160)` | 검색 결과 미리보기 텍스트 |
| `metaKeywords` | `VARCHAR(255)` | 메타 키워드 (Naver에서 여전히 활용) |
| `thumbnailUrl` | `VARCHAR(500)` | OG 이미지 / 대표 이미지 URL |

**BoardReqDto 추가 필드:**
- `slug` (optional — 미입력 시 title에서 자동 생성)
- `metaDescription` (optional — 미입력 시 content 앞 160자 자동 추출)
- `thumbnailUrl` (optional)

**BoardDetailResDto 추가 필드:**
- `slug`, `metaDescription`, `thumbnailUrl`, `categoryName`

### 3.3 신규 SEO 엔드포인트

| 엔드포인트 | 설명 |
|------------|------|
| `GET /sitemap.xml` | 동적 사이트맵 생성 — 모든 `VIEW` 상태 게시글 URL + `lastmod` 포함 |
| `GET /robots.txt` | 크롤러 정책 + sitemap 위치 명시 |
| `GET /rss.xml` 또는 `GET /feed` | RSS 2.0 / Atom 피드 — 최근 게시글 20~50개 |

### 3.4 서버사이드 렌더링 전략 (백엔드 관점)

SPA의 근본적인 SEO 한계를 해결하기 위한 **두 가지 선택지:**

**Option A: 크롤러 전용 사전 렌더링 (Prerendering)**
- 백엔드에서 User-Agent 감지 → Googlebot/Yeti(Naver) 등 크롤러 요청 시 사전 렌더된 HTML 반환
- Spring Boot에서 Prerender.io 같은 서비스 연동 또는 자체 렌더링 미들웨어 구축
- 장점: 프론트엔드 변경 최소화
- 단점: User-Agent 감지는 cloaking 리스크 존재 (Google 가이드라인 주의 필요)

**Option B: SSR/SSG 프레임워크 전환 (권장)**
- React → Next.js 전환 (SSR/ISR 지원)
- 게시글 상세 페이지를 ISR(Incremental Static Regeneration)로 정적 생성
- 장점: 근본적 해결, 성능 + SEO 모두 개선
- 단점: 프론트엔드 대규모 리팩토링 필요

**Option C: 백엔드 HTML 렌더링 엔드포인트 추가 (실용적 절충안)**
- 크롤러가 접근하는 게시글 URL에 대해 백엔드가 최소한의 HTML(meta 태그 + 본문 텍스트)을 직접 렌더링
- `GET /posts/{slug}` → Accept 헤더 또는 별도 경로로 HTML 응답
- Thymeleaf 등 서버 템플릿으로 메타 태그 + OG 태그 + 본문 미리보기 포함한 HTML 생성
- SPA는 이 HTML 위에 hydrate 하거나, 별도 API로 동작

---

## 4. 구현 단계

### Phase 0: 도메인 리네이밍 — `Board` → `Post` (선행 작업)

> **근거:** `Board`는 게시판/커뮤니티 도메인 용어이며, 블로그에서는 `Post`가 업계 표준이다.
> URL(`/posts/...`), JSON-LD(`BlogPosting`), API 설계 모두 `Post` 기반이므로 내부 모델도 일치시켜야 코드-스키마 간 인지 불일치를 방지할 수 있다.

**영향 범위 (분석 완료):**

| 영역 | 파일 수 | 참조 횟수 |
|------|---------|-----------|
| 백엔드 main (Java) | 31개 | ~230회 |
| 백엔드 test (Java) | 10개 | ~44회 |
| 프론트엔드 (JS/CSS) | 25개 | ~269회 |
| **합계** | **66개 파일** | **~543회** |

**작업 순서:**

1. **DB 테이블명 변경 마이그레이션**: `board` → `post`, FK 컬럼 `board_id` → `post_id`
   - 마이그레이션 스크립트로 `ALTER TABLE board RENAME TO post` + FK 컬럼 rename
   - `board_status` → `post_status`, 관련 인덱스명도 갱신
2. **엔티티 리네이밍 (백엔드 core)**:
   - `Board` → `Post`, `BoardStatus` → `PostStatus`, `BoardService` → `PostService` 등
   - 패키지 `domain.board` → `domain.post`, `dto.board` → `dto.post`
   - `BoardRepository` → `PostRepository`, `BoardRedisRepository` → `PostRedisRepository`
   - `BoardController` → `PostController`, `BoardLikeController` → `PostLikeController`
   - `BoardForRedis` → `PostForRedis`, `BoardCacheService` → `PostCacheService`
3. **테스트 코드 리네이밍**: 10개 테스트 파일 전체 갱신
4. **프론트엔드 리네이밍**:
   - 컴포넌트: `BoardList` → `PostList`, `BoardDetail` → `PostDetail` 등
   - API 함수: `boardApi.js` → `postApi.js`
   - CSS 클래스: `.board-*` → `.post-*`
   - 라우트: `/boards/:boardId` → `/posts/:postId` (Phase 2에서 slug로 최종 전환)
5. **QueryDSL Q클래스 재생성**: `./gradlew clean compileQuerydsl`
6. **전체 테스트 실행 — 99개 테스트 통과 확인**

**리스크 완화:**
- 한 번의 대형 커밋이 아닌, 레이어별 분리 커밋 (entity → repository → service → controller → test → frontend)
- `@Table(name = "post")` + `@Column(name = "post_id")`로 JPA 매핑 명시하여 DB 의존 명확화
- 리네이밍 전후 전체 테스트 통과를 게이트로 활용

---

### Phase 1: SEO 필드 + Slug 시스템 (기반 작업)

1. `Post` 엔티티에 `slug`, `metaDescription`, `metaKeywords`, `thumbnailUrl` 필드 추가
2. Flyway/Liquibase 마이그레이션 스크립트 작성 (기존 게시글에 slug 자동 생성)
3. `SlugUtil` 유틸리티 구현
   - 한글 제목 → 로마자 변환 또는 한글 slug 허용 (Naver는 한글 URL 지원)
   - 중복 slug 방지 로직 (`title-slug` → `title-slug-2`)
4. `PostReqDto`, `PostDetailResDto`, `PostResDto` DTO에 새 필드 반영
5. `PostService.write()` / `PostService.edit()`에서 slug 자동 생성 로직 추가
6. 기존 게시글 일괄 slug 생성 마이그레이션 실행

### Phase 2: API 엔드포인트 개편

1. `GET /api/v1/posts/{slug}` 엔드포인트 추가 (slug 기반 조회)
2. `PostRepository`에 `findBySlug(String slug)` 쿼리 추가
3. 기존 `GET /api/v8/boards/{boardId}` → `GET /api/v1/posts/{postId}` 전환, 기존 경로 301 리다이렉트
4. `PostDetailResDto`에 `categoryName` 필드 추가
5. 게시글 목록 API 응답에 `slug` 필드 포함

### Phase 3: sitemap.xml + robots.txt + RSS

1. `SitemapController` 구현
   - `GET /sitemap.xml` — `VIEW` 상태 게시글 전체 + 카테고리 페이지 포함
   - `lastmod`에 `updateDate` 활용
   - 게시글 1000개 이상일 경우 sitemap index 방식 적용
2. `RobotsController` 구현 (또는 static resource)
   - `Sitemap: https://yourdomain.com/sitemap.xml`
   - `/api/`, `/management/` 경로 크롤링 차단
3. `RssFeedController` 구현
   - RSS 2.0 포맷, 최근 50개 게시글
   - `<title>`, `<description>`, `<link>`, `<pubDate>` 포함
4. `WebSecurityConfig`에 `/sitemap.xml`, `/robots.txt`, `/rss.xml` permitAll 추가

### Phase 4: 서버사이드 메타 렌더링

1. Thymeleaf 의존성 추가 (메타 태그 렌더링 전용)
2. 크롤러/SNS 공유 요청에 대해 HTML 응답하는 컨트롤러 구현:
   - `<title>`, `<meta name="description">`, `<meta name="keywords">`
   - Open Graph: `og:title`, `og:description`, `og:image`, `og:url`, `og:type`
   - Twitter Card: `twitter:card`, `twitter:title`, `twitter:description`, `twitter:image`
   - `<link rel="canonical">`
   - JSON-LD `BlogPosting` 구조화된 데이터
3. React SPA는 기존대로 동작 — 크롤러만 서버 렌더링 HTML 수신

### Phase 5: HTTP 캐시 + 성능 최적화

1. 게시글 상세 API에 `Last-Modified` 헤더 추가 (`updateDate` 활용)
2. 게시글 목록 API에 `Cache-Control: public, max-age=300` 설정
3. sitemap.xml에 적절한 캐시 정책 적용 (1시간 등)
4. 이미지 alt 텍스트 필드 `ImageFile` 엔티티에 추가

### Phase 6: 프론트엔드 연동 (백엔드 완료 후)

1. `index.html` lang 속성 `en` → `ko` 변경
2. 프론트엔드 라우팅 `/boards/:boardId` → `/posts/:slug` 변경
3. 기존 `/boards/{id}` URL → `/posts/{slug}` 301 리다이렉트 처리 (백엔드)
4. `react-helmet-async` 등으로 페이지별 동적 title/description 적용
5. Naver Search Advisor / Google Search Console 등록

---

## 5. 영향 분석

### 5.1 예상 SEO 개선 효과

| 항목 | 현재 | 개선 후 |
|------|------|---------|
| **Google 인덱싱** | JS 렌더링 의존 — 불안정 | 크롤러 전용 HTML로 100% 인덱싱 보장 |
| **Naver 인덱싱** | 완전 불가 (JS 미실행) | HTML 메타 렌더링으로 인덱싱 가능 |
| **검색 결과 표시** | 기본 title + CRA 기본 description | 게시글별 고유 title + description |
| **URL 가독성** | `/boards/42` | `/posts/spring-boot-cache-guide` |
| **SNS 공유 미리보기** | 없음 | OG/Twitter Card로 제목+설명+이미지 표시 |
| **Rich Snippet** | 없음 | JSON-LD로 작성일/작성자 표시 |
| **신규 콘텐츠 발견 속도** | 크롤러 자연 발견 대기 | sitemap + RSS로 즉시 알림 |

### 5.2 리스크 및 트레이드오프

| 리스크 | 영향 | 완화 방안 |
|--------|------|-----------|
| **Board → Post 리네이밍** | 66개 파일, ~543회 참조 변경 — 대규모 리팩토링 | 레이어별 분리 커밋, 각 단계 테스트 통과 게이트, IDE 리팩토링 도구 활용 |
| **DB 테이블 리네이밍** | `board` → `post` 테이블명 변경 — 운영 중 실행 시 다운타임 | 마이그레이션 스크립트 사전 검증, 배포 윈도우에 실행 |
| **Slug 마이그레이션** | 기존 게시글 URL 변경 → 외부 링크 깨짐 | 301 리다이렉트로 SEO 점수 이전, 기존 ID 기반 접근 병행 유지 |
| **중복 엔드포인트 관리** | slug + ID 두 가지 접근 경로 → 복잡도 증가 | 전환 기간 후 ID 엔드포인트 deprecation |
| **Thymeleaf 도입** | 기술 스택 추가 | 메타 렌더링 전용으로 최소한만 사용, 향후 Next.js 전환 시 제거 |
| **한글 slug 처리** | URL 인코딩 이슈 가능 | 영문 slug 기본 + 한글 옵션 제공 |
| **DB 마이그레이션** | 운영 중단 리스크 | nullable 필드로 추가 → 배치로 기존 데이터 채움 → NOT NULL 전환 |

---

## 6. 선택적 추가 개선 사항

| 항목 | 설명 | 우선순위 |
|------|------|----------|
| **Next.js 전환** | 근본적인 SSR/ISR 지원으로 Phase 4 불필요 | 장기 |
| **AMP 페이지** | 모바일 검색 우선 노출 (Google Discover 등) | 낮음 |
| **다국어 지원 (hreflang)** | 영문 게시글 작성 시 언어별 URL 분리 | 낮음 |
| **게시글 읽기 시간 표시** | 구조화된 데이터에 `timeRequired` 추가 | 낮음 |
| **Naver 웹마스터 도구 연동** | `<meta name="naver-site-verification">` 추가 | 높음 |
| **Google Search Console 연동** | `google-site-verification` 메타 태그 | 높음 |
| **내부 링크 최적화** | 관련 게시글 추천 API → 크롤러가 링크 구조 파악 용이 | 중간 |
| **Breadcrumb 구조화 데이터** | 홈 > 카테고리 > 게시글 경로를 JSON-LD로 제공 | 중간 |

---

## 7. 요약 — 우선순위 로드맵

```
Phase 0 (선행)     → Board → Post 도메인 리네이밍    ← 전체 작업의 토대, 이후 Phase에서 네이밍 혼란 방지
Phase 1 (기반)     → Slug + SEO 필드 추가           ← 모든 후속 작업의 전제
Phase 2 (API)      → Slug 기반 엔드포인트            ← URL 구조 개선
Phase 3 (발견성)   → Sitemap + robots.txt + RSS      ← 검색엔진 크롤링 지원
Phase 4 (핵심)     → 서버사이드 메타 렌더링           ← Naver 인덱싱 해결
Phase 5 (성능)     → HTTP 캐시 최적화                ← 크롤링 효율 개선
Phase 6 (연동)     → 프론트엔드 URL 전환 + 등록       ← 최종 적용
```

> **핵심 메시지:** Phase 0(리네이밍)을 먼저 완료해야 이후 모든 Phase에서 `Post` 기반으로 일관성 있게 작업할 수 있다. 가장 시급한 SEO 문제는 **Naver 크롤러가 콘텐츠를 전혀 볼 수 없다는 것**이며, Phase 0~4 완료가 검색 노출의 기본 조건이다.
