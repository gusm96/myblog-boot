# API 버전 관리 전략

---

## 현황 분석

현재 `BoardController`에 v1 ~ v7까지의 API 버전이 혼재되어 있음.

### 현재 엔드포인트 목록

| 버전 | 엔드포인트 | 설명 | 상태 |
|------|-----------|------|------|
| v1 | GET `/api/v1/boards` | 게시글 목록 | 사용 중 |
| v1 | GET `/api/v1/boards/category` | 카테고리별 목록 | 사용 중 |
| v1 | POST `/api/v1/boards` | 게시글 작성 | 사용 중 |
| v1 | PUT `/api/v1/boards/{id}` | 게시글 수정 | 사용 중 |
| v1 | DELETE `/api/v1/boards/{id}` | 게시글 삭제 | 사용 중 |
| v4 | GET `/api/v4/boards/{id}` | 게시글 상세 (조회수 무제한 증가) | 레거시 |
| v5 | GET `/api/v5/boards/{id}` | 게시글 상세 (Redis IP 중복 제어) | 레거시 |
| v6 | GET `/api/v6/boards/{id}` | 게시글 상세 (Cookie 24h 제어) | 레거시 |
| v7 | GET `/api/v7/boards/{id}` | 게시글 상세 (사용자별 기록) | **현재 사용** |
| v2 | GET `/api/v2/likes/{id}` | 좋아요 상태 | 사용 중 |
| v2 | POST `/api/v2/likes/{id}` | 좋아요 추가 | 사용 중 |

---

## 문제점

1. **버전 번호 일관성 없음**: v1, v2, v4, v5, v6, v7이 혼재 (v3 없음)
2. **레거시 버전 방치**: v4, v5, v6은 사용되지 않지만 코드에 남아 있음
3. **API 버전 전략 부재**: 새 버전 추가 기준이 불명확

---

## 개선 방향

### 단기: 레거시 제거

1. 프론트엔드에서 v4, v5, v6 엔드포인트를 호출하는 코드가 없는지 확인
2. 확인 후 `BoardController`에서 `v4`, `v5`, `v6` 메서드 제거

### 중기: 버전 통일

`v7`의 게시글 상세 조회 로직을 `v1`으로 통합하고 버전 개념 단순화.

```
GET /api/v1/boards/{boardId}  ← v7 로직으로 대체
```

변경 시 프론트엔드 `apiConfig.js`의 `BOARD_GET`도 함께 수정.

### 장기: API 버전 정책 수립

| 원칙 | 내용 |
|------|------|
| 하위 호환 변경 | 버전 올리지 않음 (필드 추가 등) |
| 파괴적 변경 | 버전 올림 (응답 구조 변경, 필드 제거 등) |
| 레거시 유지 기간 | 신 버전 배포 후 최소 2주간 구 버전 유지 |
| 레거시 표시 | `@Deprecated` 어노테이션 + 응답 헤더에 `Deprecation` 포함 |

```java
// 레거시 엔드포인트 표시 방법
@Deprecated
@GetMapping("/api/v6/boards/{boardId}")
public ResponseEntity<?> getBoard_v6(...) {
    // ...
}
```

---

## 마이그레이션 계획

### 1단계: 레거시 엔드포인트 파악 및 제거
- [ ] v4, v5, v6 사용처 확인 (프론트엔드, 외부 클라이언트)
- [ ] 사용처 없음 확인 후 코드 제거

### 2단계: v7 로직을 v1으로 통합
- [ ] `BoardController`에서 v1 게시글 상세 조회를 v7 로직으로 교체
- [ ] `apiConfig.js`에서 `BOARD_GET` 엔드포인트를 v1으로 변경
- [ ] v7 엔드포인트 제거

### 3단계: API 버전 정책 문서화
- [ ] API 변경 정책 팀 내 합의
- [ ] README 또는 별도 API 문서에 정책 반영
