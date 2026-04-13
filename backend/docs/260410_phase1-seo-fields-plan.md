# Phase 1: SEO 필드 + Slug 시스템 작업 계획서

> 작성일: 2026-04-10  
> 참조: `260410_seo-optimization-plan.md` Phase 1

---

## 1. 작업 목표

`Post` 엔티티에 SEO 필드를 추가하고 Slug 기반 식별 시스템을 구축한다.  
이 작업은 Phase 2(Slug 엔드포인트)와 Phase 3(sitemap)의 전제 조건이다.

---

## 2. 구현 범위

### 2.1 신규 파일

| 파일 | 역할 |
|------|------|
| `utils/SlugUtil.java` | 제목 → slug 변환, 한글 허용, 중복 suffix 로직 |

### 2.2 변경 파일

| 파일 | 변경 내용 |
|------|-----------|
| `domain/post/Post.java` | `slug`, `metaDescription`, `metaKeywords`, `thumbnailUrl` 필드 추가 |
| `dto/post/PostForRedis.java` | `slug`, `metaDescription`, `thumbnailUrl`, `categoryName` 추가 |
| `dto/post/PostDetailResDto.java` | `slug`, `metaDescription`, `thumbnailUrl`, `categoryName` 추가 |
| `dto/post/PostResDto.java` | `slug` 추가 (목록 API에서 slug 포함) |
| `dto/post/PostReqDto.java` | `slug`(optional), `metaDescription`(optional), `thumbnailUrl`(optional) 추가 |
| `repository/PostRepository.java` | `findBySlug`, `existsBySlug` 추가 |
| `service/PostService.java` | 메서드 시그니처 변경 없음 — 내부 slug 생성 로직 추가 |
| `service/implementation/PostServiceImpl.java` | `write()` / `edit()`에 slug 자동 생성 추가 |

---

## 3. 설계 결정

### 3.1 Slug 규칙

- **한글 허용**: Naver 크롤러가 한글 URL을 지원하며, 모던 브라우저에서 가독성 유지
- **변환 로직**:
  1. 소문자 변환 (영문만)
  2. 한글·영문·숫자·공백·하이픈 외 문자 제거
  3. 공백·연속 하이픈 → 단일 하이픈
  4. 앞뒤 하이픈 제거
  5. 100자 제한 (word boundary 기준 truncation)
- **중복 처리**: `slug` → `slug-2` → `slug-3` (최대 10회 시도 후 UUID suffix)
- **수동 지정**: `PostReqDto.slug`가 있으면 우선 사용

### 3.2 DB 마이그레이션 전략

`application-dev.yaml`에 `ddl-auto: create`가 설정되어 있어 개발 환경에서는 자동 처리.  
운영 환경을 위한 ALTER TABLE SQL을 이 문서 하단에 제공.

### 3.3 Redis 캐시 호환성

`PostForRedis`에 새 필드 추가 시 기존 캐시 JSON에는 해당 필드가 없다.  
Jackson 역직렬화에서 누락 필드는 `null`로 처리되므로 별도 조치 불필요.

### 3.4 categoryName 처리

`PostForRedis`에 `categoryName`을 포함 → `PostDetailResDto` 빌드 시 추가 DB 조회 없이 응답.  
`Post` 엔티티는 category를 fetch join으로 가져오므로 N+1 문제 없음.

---

## 4. 운영 DB 마이그레이션 SQL

```sql
ALTER TABLE post
  ADD COLUMN slug              VARCHAR(255) NULL UNIQUE AFTER post_status,
  ADD COLUMN meta_description  VARCHAR(160) NULL AFTER slug,
  ADD COLUMN meta_keywords     VARCHAR(255) NULL AFTER meta_description,
  ADD COLUMN thumbnail_url     VARCHAR(500) NULL AFTER meta_keywords;

-- 기존 게시글 slug 자동 생성 (title 기반, 한글 그대로 사용)
-- 실제 운영에서는 SlugUtil 로직과 동일하게 애플리케이션 레벨에서 backfill 권장
```

---

## 5. 체크리스트

- [ ] `SlugUtil.java` 구현
- [ ] `Post.java` SEO 필드 추가 + Builder 업데이트
- [ ] `PostRepository.java` slug 조회 메서드 추가
- [ ] `PostForRedis.java` 필드 추가
- [ ] `PostDetailResDto.java` 필드 추가
- [ ] `PostResDto.java` slug 추가
- [ ] `PostReqDto.java` optional SEO 필드 추가
- [ ] `PostServiceImpl.java` slug 생성 로직 추가
- [ ] 컴파일 확인
- [ ] 관련 테스트 실행
