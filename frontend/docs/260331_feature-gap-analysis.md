# 프론트엔드 기능 검토 및 Gap 분석

> 작성일: 2026-03-31
> 대상: `myblog-boot` 프론트엔드 (React 18 / Vite / TanStack Query v5)

---

## 1. 현재 구현 상태 요약

### 구현 완료 기능

| 기능 | 구현 상태 | 비고 |
|---|---|---|
| JWT 인증 / 토큰 갱신 | ✅ 완료 | Redux Persist, 앱 마운트 시 자동 갱신 |
| 역할 기반 접근 제어 | ✅ 완료 | ProtectedRoute (ROLE_ADMIN) |
| 게시글 목록 (무한 스크롤) | ✅ 완료 | IntersectionObserver, TanStack Query |
| 게시글 상세 조회 | ✅ 완료 | DOMPurify HTML 렌더링 |
| 게시글 작성/수정 | ✅ 완료 | TipTap StarterKit + Image |
| 게시글 소프트 삭제 / 복구 | ✅ 완료 | TemporaryStorage 화면 |
| 카테고리별 목록 | ✅ 완료 | 무한 스크롤 동일 방식 |
| 카테고리 CRUD (관리자) | ✅ 완료 | CategoryList / CategoryModal |
| 댓글 작성/수정/삭제 | ✅ 완료 | 로그인 필요 |
| 중첩 답글 (무한 depth) | ✅ 완료 | 지연 로딩 방식 |
| 좋아요 토글 | ✅ 완료 | BoardLike, 카운트 표시 |
| 검색 (제목/내용/작성자) | ✅ 완료 | SearchPage, URL 파라미터 |
| 방문자 수 표시 | ✅ 완료 | VisitorCount 컴포넌트 |
| 이미지 업로드 (에디터) | ✅ 완료 | S3 연동, 현재 더미 자격증명 |
| 마크다운 파일 임포트 | ✅ 완료 | .md → TipTap HTML 변환 |
| React Query 캐시 영속화 | ✅ 완료 | localStorage, 선택적 dehydrate |

---

## 2. 코드 레벨 버그 및 문제

> 실제 코드를 읽으며 발견한 사항. 기능 추가보다 먼저 수정이 필요한 것들.

### 2-1. 버그 (동작 오류)

#### `boardApi.js` — 페이지네이션 쿼리 파라미터 오류
```js
// 현재 (잘못됨)
getBoardLikes: (page) => axios.get(`${BASE_URL}/boards?${page}`)
getDeletedBoards: (page) => axios.get(`${BASE_URL}/boards/deleted?${page}`)

// 올바른 형태
getBoardLikes: (page) => axios.get(`${BASE_URL}/boards?p=${page}`)
```
`page` 값이 쿼리 파라미터의 키가 아닌 값만 붙는 형태 → 항상 1페이지만 조회됨.

#### `apiClient.js` — null 토큰 헤더 전송
```js
// 현재: 토큰이 null이어도 헤더 추가
config.headers.Authorization = `bearer ${_token}`;

// 수정: 토큰 존재 시에만
if (_token) config.headers.Authorization = `bearer ${_token}`;
```
미인증 요청에 `Authorization: bearer null` 헤더가 붙어 전송됨.

#### `boardApi.js` — `uploadImageFile` 응답 처리 불일치
```js
// 현재: res 전체를 반환 (다른 함수와 불일치)
export const uploadImageFile = (formData) =>
  apiClient.post(...).then((res) => res);

// 다른 함수들은 모두 res.data 반환
export const getBoard = (id) =>
  apiClient.get(...).then((res) => res.data);
```
에디터에서 이미지 URL 추출 시 `res.data.url` 대신 `res.url`을 참조할 가능성.

#### `VisitorCount.js` — `apiClient` 대신 raw `axios` 직접 사용
```js
// 현재: 인터셉터 우회
import axios from 'axios';
axios.get(...)

// 올바른 형태
import apiClient from '../services/apiClient';
apiClient.get(...)
```
중앙 집중식 에러 핸들링 및 인증 헤더 주입을 완전히 우회.

---

### 2-2. 품질 문제

#### 응답 인터셉터 없음 (`apiClient.js`)
- 요청 중 토큰 만료(401) 발생 시 자동 갱신 로직 없음
- 앱 마운트 시 1회 갱신은 있지만, 장시간 사용 중 만료되면 API가 그냥 실패함
- 결과: 사용자가 직접 페이지를 새로고침해야 하는 상황 발생 가능

#### `alert()` 사용 (BoardLike.js)
```js
alert('로그인이 필요합니다.');
```
브라우저 기본 alert — UX 일관성 파괴. Toast/Modal로 교체 필요.

#### 에러 무시 패턴 (BoardEditor.js 외 다수)
```js
.catch(() => {}) // 완전히 무시
```
이미지 업로드 실패, 게시글 저장 실패 등이 사용자에게 전혀 보이지 않음.

#### `useQueries.js` 범위 제한
- 훅이 `useBoardQuery`, `useCommentsQuery` 2개뿐
- 좋아요 상태, 카테고리 목록, 검색 결과 쿼리는 컴포넌트 내 인라인 정의 혼재
- 쿼리 관리 일관성 부재

#### 댓글 전체 로드 (페이지네이션 없음)
- `CommentList.js`에서 특정 게시글의 댓글 전체를 한 번에 로드
- 댓글이 많은 게시글에서 성능 문제 가능

#### 에디터 유효성 검사 없음 (BoardEditor.js)
- 제목/내용 비어있어도 제출 가능
- 페이지 이탈 시 작성 중 내용 경고 없음

---

## 3. 누락된 기능 분석

### 3-1. 높은 우선순위

#### SEO 메타 태그
- **현재**: `index.html`의 정적 `<title>` 1개만 존재
- **필요**: 게시글별 동적 `<title>`, `og:title`, `og:description`, `og:image`
- **이유**: 개인 블로그에서 SNS 공유, 검색 유입의 기본 조건
- **구현 방법**: `react-helmet-async` 또는 `react-router` v7의 `handle.meta` 패턴

#### 코드 블록 문법 강조 (Syntax Highlighting)
- **현재**: TipTap `StarterKit`의 기본 `CodeBlock`만 사용 — 언어 지정 없음, 색상 없음
- **필요**: `@tiptap/extension-code-block-lowlight` + `lowlight` + `highlight.js`
- **이유**: 기술 블로그라면 코드 가독성이 콘텐츠 품질의 핵심
- **추가 필요 익스텐션**:
  - 에디터 툴바에 언어 선택 드롭다운
  - 뷰어에서 "복사" 버튼

#### 게시글 수정/삭제 UI (일반 사용자 관점)
- **현재**: `BoardDetail.js`에 수정/삭제 버튼 없음
- **현재**: 수정은 `/management/boards/:boardId`로만 가능 (관리자 경로)
- **문제**: 게시글 상세에서 바로 수정으로 이동하는 UX가 없음
- **권고**: 작성자(또는 관리자) 확인 후 상세 페이지에 편집 버튼 노출

---

### 3-2. 중간 우선순위

#### 태그 시스템
- **현재**: 카테고리 1개만 지정 가능 (1:1 관계)
- **필요**: 다중 태그 (M:N 관계 — `board_tag`, `tag` 테이블)
- **이유**: 카테고리보다 세밀한 콘텐츠 분류, 관련 글 연결에 활용
- **백엔드 변경 필요**: `Tag` 엔티티, `BoardTag` 중간 테이블, 태그별 게시글 API

#### 읽기 예상 시간
- **현재**: 없음
- **구현**: 게시글 HTML의 텍스트 길이 ÷ 200(분당 평균 단어 수) → 분 단위 표시
- **프론트 단에서만 처리 가능**, 백엔드 변경 불필요

#### 목차 자동 생성 (Table of Contents)
- **현재**: 없음
- **구현**: 게시글 내 `h1~h3` 태그 추출 → 사이드바 목차 렌더링
- **긴 기술 글에서 탐색성 크게 향상**

#### 관련 게시글 추천
- **현재**: 없음
- **구현**: 동일 카테고리/태그 게시글 3~5개 표시
- **필요**: 백엔드 API 추가 (`/api/v1/boards/{boardId}/related`)

#### SNS 공유 버튼
- **현재**: 없음
- **구현**: 링크 복사, 카카오톡 SDK, Twitter Intent URL
- **SEO 메타 태그와 반드시 세트로 구현**해야 효과 있음

#### Toast 알림 시스템
- **현재**: `alert()` 또는 무소음 실패
- **필요**: `react-toastify` 또는 자체 Toast 컴포넌트
- **적용 범위**: 게시글 저장 성공/실패, 이미지 업로드 실패, 좋아요 로그인 요구 등

---

### 3-3. 낮은 우선순위

#### RSS 피드
- **구현 위치**: 백엔드 (`/rss.xml`, Spring `Rome` 라이브러리)
- **이유**: 기술 블로그 독자의 Feedly/RSS 구독 지원

#### About / 소개 페이지
- **현재**: 없음
- **구현**: 정적 컴포넌트, 라우트 `/about` 추가
- **내용**: 블로그 소개, GitHub/LinkedIn 링크, 기술 스택

#### 다크 모드
- **현재**: 없음
- **구현**: Bootstrap `data-bs-theme="dark"` + CSS 변수 오버라이드
- **Redux** 또는 `localStorage`에 테마 설정 저장

#### 읽기 진행바
- **현재**: 없음
- **구현**: `scroll` 이벤트 → 상단 고정 progress bar
- **순수 CSS/JS, 백엔드 변경 불필요**

#### 게시글 시리즈 묶음
- **현재**: 없음
- **필요**: 연재물 그룹핑 (`series` 테이블), 이전/다음 글 네비게이션
- **백엔드 변경 필요**: 난이도 높음, 마지막에 고려

#### 공지/고정 게시글
- **현재**: 없음
- **구현**: `Board.pinned: boolean` 필드 추가, 목록 상단 고정
- **백엔드 변경 소규모**

---

## 4. 기술 부채

| 항목 | 현재 상태 | 영향 |
|---|---|---|
| AWS S3 더미 자격증명 | 실제 업로드 불가 | 이미지 기능 전체 비활성 |
| 응답 인터셉터 없음 | 런타임 401 처리 불가 | 장시간 사용 시 인증 오류 |
| 댓글 무한 로드 | 페이지네이션 없음 | 댓글 많은 글에서 느림 |
| 에러 핸들링 불일치 | `catch(() => {})` 혼재 | 디버깅 어려움, 사용자 UX 저하 |
| `alert()` 사용 | UX 파괴 | 모달/토스트로 교체 필요 |
| `useQueries.js` 불완전 | 인라인 쿼리 혼재 | 유지보수 어려움 |
| 에디터 유효성 검사 없음 | 빈 게시글 가능 | 데이터 품질 저하 |
| `VisitorCount` axios 직접 사용 | 인터셉터 우회 | 에러 추적 불가 |

---

## 5. 권장 작업 순서

### Phase 1 — 버그 수정 (즉시)
1. `boardApi.js` 쿼리 파라미터 오류 수정 (`?${page}` → `?p=${page}`)
2. `apiClient.js` null 토큰 헤더 조건 추가
3. `uploadImageFile` 응답 처리 통일 (`res` → `res.data`)
4. `VisitorCount.js` axios → apiClient 교체
5. `BoardEditor.js` 유효성 검사 추가 (제목, 내용 비어있으면 제출 차단)
6. `alert()` → 인라인 에러 메시지로 교체 (Toast 시스템 도입 전 임시)

### Phase 2 — 핵심 UX 개선
7. 코드 블록 문법 강조 (`@tiptap/extension-code-block-lowlight`)
8. Toast 알림 시스템 도입 (`react-toastify`)
9. 응답 인터셉터 추가 (401 자동 토큰 갱신)
10. 게시글 상세 — 수정/삭제 버튼 추가 (관리자)

### Phase 3 — SEO 및 공유
11. `react-helmet-async` 도입 — 페이지별 동적 메타 태그
12. SNS 공유 버튼 (링크 복사 우선, 이후 카카오/트위터)

### Phase 4 — 콘텐츠 기능 확장
13. 읽기 예상 시간 (프론트 단 계산)
14. 목차 자동 생성 (h1~h3 추출)
15. 태그 시스템 (백엔드 협의 필요)
16. 관련 게시글 (백엔드 API 필요)

### Phase 5 — 선택적 개선
17. 다크 모드
18. About 페이지
19. RSS 피드
20. 읽기 진행바 / 고정 게시글

---

## 6. 참고 — 이미 잘 구현된 부분

다음은 수정 없이 유지해도 좋은 완성도 높은 구현:

- **TanStack Query 캐시 전략**: staleTime 계층 설정, localStorage 선택적 영속화
- **무한 스크롤**: IntersectionObserver 기반, 300px 선행 로딩
- **중첩 댓글 지연 로딩**: 답글은 열어볼 때만 fetch
- **HMAC 쿠키 기반 조회수 중복 방지**: 완전 Stateless
- **ProtectedRoute**: 역할 + 토큰 만료 동시 처리
- **DOMPurify HTML 렌더링**: XSS 방지
- **queryKeys 팩토리**: 계층적 키 구조, 일괄 invalidation 가능
