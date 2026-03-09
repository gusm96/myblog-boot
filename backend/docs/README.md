# MyBlog Boot - 프로젝트 분석 및 리팩토링 가이드

## 개요

Spring Boot + React 기반의 개인 블로그 애플리케이션 프로젝트 전반에 대한 검토 결과와
구조 개선 및 코드 리팩토링 계획을 정리한 문서입니다.

---

## 문서 목록

| 문서 | 설명 |
|------|------|
| [architecture.md](./architecture.md) | 전체 아키텍처 구조 및 기술 스택 |
| [bugs.md](./bugs.md) | 즉시 수정이 필요한 버그 목록 |
| [fix-p1-bugs.md](./fix-p1-bugs.md) | P1 버그 수정 상세 내역 (완료) |
| [refactoring-frontend.md](./refactoring-frontend.md) | 프론트엔드 리팩토링 계획 |
| [refactoring-backend.md](./refactoring-backend.md) | 백엔드 리팩토링 계획 |
| [api-strategy.md](./api-strategy.md) | API 버전 관리 전략 |

---

## 우선순위 요약

### P1 - 즉시 수정 (버그/보안) ✅ 완료
- [x] `BoardEditForm.js` 에디터 상태 버그
- [x] `BoardForm.js` 에디터 상태 버그 + toolbar props 케이스 오류
- [x] `categoryApi.js` 함수명 오타 및 axios 구문 오류
- [x] `CategoryModal.js` props 구조 분해 오류
- [x] `BoardDetailResDto.java` 필드명 오타 (`creatDate`)
- [x] HTML 파싱 XSS 취약점 (DOMPurify 적용)

### P2 - 단기 개선 (1~2주)
- `PageByCategory.js` React Query 방식으로 통일
- `BoardForm.js` / `BoardEditor.js` 중복 컴포넌트 통합
- React Query `staleTime` / `gcTime` 최적화
- 토큰 갱신 로직 커스텀 훅으로 분리

### P3 - 중기 개선 (3~4주)
- API 버전 전략 정리 (v1~v7 혼재)
- 에러 처리 개선 (`alert` → Toast/Modal)
- JPA N+1 쿼리 점검 및 최적화
- 백엔드 DTO 오타 수정 (`creatDate`)

### P4 - 장기 개선 (지속)
- 테스트 커버리지 확대
- 접근성 개선
- 성능 모니터링 도입
