# 백엔드 리팩토링 진행 내역

> 작업일: 2026-02-24
> 기준 문서: [refactoring-backend.md](./refactoring-backend.md)

---

## 완료 항목

### RFC-BE-009 (P2) — InitDb.java 운영 환경 안전 처리

**대상 파일**: `src/main/java/com/moya/myblogboot/configuration/InitDb.java`

**변경 내용**:
```diff
+ import org.springframework.context.annotation.Profile;
+ import org.springframework.stereotype.Component;

+ @Component
+ @Profile("dev")
  @RequiredArgsConstructor
  public class InitDb {
```

**이유**: `@Component` 없이는 Spring 빈으로 등록되지 않아 실행 자체가 되지 않는 상태였음.
`@Component`와 `@Profile("dev")`를 함께 추가해 `dev` 프로파일 환경에서만 초기 데이터 삽입이 실행되도록 명시적으로 제한함.

---

### RFC-BE-002 (P3) — 레거시 API 엔드포인트 Deprecated 처리

**대상 파일**: `src/main/java/com/moya/myblogboot/controller/BoardController.java`

**변경 내용**: v4, v5, v6 엔드포인트에 `@Deprecated` 추가

```diff
  // 게시글 상세 V4
+ @Deprecated
  @GetMapping("/api/v4/boards/{boardId}")
  public ResponseEntity<BoardDetailResDto> getBoardDetail(...) { ... }

  // 게시글 상세 조회 V5
+ @Deprecated
  @GetMapping("/api/v5/boards/{boardId}")
  public ResponseEntity<BoardDetailResDto> getBoardDetailV5(...) { ... }

  // 게시글 상세 조회 V6
+ @Deprecated
  @GetMapping("/api/v6/boards/{boardId}")
  public ResponseEntity<BoardDetailResDto> getBoardDetailV6(...) { ... }
```

**현재 사용 중인 버전**: v7 (`/api/v7/boards/{boardId}`)
**향후 계획**: v4~v6 엔드포인트 제거 일정 수립 필요 → [api-strategy.md](./api-strategy.md) 참고

---

### RFC-BE-003 (P3) — 서비스 레이어 메서드명 개선

**변경 전 → 변경 후**

| 변경 전 | 변경 후 | 설명 |
|--------|--------|------|
| `retrieve(Long boardId)` | `findById(Long boardId)` | 엔티티 직접 조회 (내부/타 서비스 사용) |
| `retrieveDto(Long boardId)` | `getBoardDetail(Long boardId)` | 상세 DTO 조회 (조회수 미증가) |
| `retrieveAndIncrementViewsDto(Long boardId)` | `getBoardDetailAndIncrementViews(Long boardId)` | 상세 DTO 조회 + 조회수 증가 |
| `retrieveBoardInRedisStore(Long boardId)` | `getBoardFromCache(Long boardId)` | Redis 캐시에서 조회 |

**수정된 파일 목록**:

| 파일 | 역할 |
|------|------|
| `service/BoardService.java` | 인터페이스 메서드 선언 변경 |
| `service/implementation/BoardServiceImpl.java` | 구현체 메서드 + 내부 호출부 변경 |
| `controller/BoardController.java` | 컨트롤러에서 서비스 호출부 변경 |
| `scheduler/BoardScheduledTask.java` | 스케줄러에서 서비스 호출부 변경 |
| `service/implementation/BoardLikeServiceImpl.java` | 좋아요 서비스에서 호출부 변경 |
| `service/implementation/CommentServiceImpl.java` | 댓글 서비스에서 호출부 변경 |

---

### RFC-BE-001 (P3) — BoardDetailResDto 필드명 오타 수정

**상태**: 이미 완료됨 (BUG-005에서 수정)

`creatDate` → `createDate` 수정은 [fix-p1-bugs.md](./fix-p1-bugs.md)의 BUG-005에서 이미 처리됨.
현재 `BoardDetailResDto.java`의 필드명은 `createDate`로 정상 상태.

---

### RFC-BE-004 (P3) — 글로벌 예외 핸들러 정비

**상태**: 이미 구현됨

`src/main/java/com/moya/myblogboot/exception/GlobalExceptionHandler.java` 파일이 이미 존재하며,
아래 예외들에 대한 처리가 구현되어 있음:
- `DuplicateKeyException` → 409 Conflict
- `EntityNotFoundException`, `UsernameNotFoundException`, `NoSuchElementException` → 404 Not Found
- `BadCredentialsException` → 400 Bad Request
- `MethodArgumentNotValidException` → 400 Bad Request
- `AccessDeniedException`, `SignatureException`, `ExpiredTokenException`, `ExpiredJwtException` → 401 Unauthorized
- `ExpiredRefreshTokenException` → 401 + 쿠키 삭제
- `RuntimeException`, `PersistenceException` → 500 Internal Server Error
- `ImageUploadFailException`, `ImageDeleteFailException` → 500 Internal Server Error

---

## 미완료 항목

### RFC-BE-005 (P3) — 조회수 업데이트 스케줄러 안정성 개선

`BoardScheduledTask.java`의 `updateFromRedisStoreToDB()` 메서드는 실패 시 `RuntimeException`을 던지는 것으로 끝남.
분산 환경에서의 중복 실행 방지를 위한 `ShedLock` 또는 Redis 분산 락 도입이 필요하나, 의존성 추가가 필요하므로 별도 작업으로 진행.

**현황 분석**:
- `@Profile("!test")` 로 테스트 환경 제외는 이미 적용됨
- 실패 시 재시도 로직 없음
- 분산 환경 배포 시 중복 실행 가능성 있음

---

### RFC-BE-006 (P3) — N+1 쿼리 점검 및 최적화

게시글 목록 조회 시 `Member`, `Category` 등을 개별 조회하는 N+1 문제 여부를 리포지토리 레이어에서 확인 필요.
Fetch Join 쿼리 적용 여부 검토 필요.

---

### RFC-BE-007 (P4) — 환경별 설정 분리 확인

`application.yml` / `application-prod.yml` / `application-dev.yml` 분리 여부 및 민감 정보 환경 변수 처리 확인 필요.

---

### RFC-BE-008 (P4) — 테스트 커버리지 확대

추가 필요 테스트:
- `BoardServiceImpl` 단위 테스트
- `CategoryService` 단위 테스트
- `CommentService` 단위 테스트
- 인증 관련 통합 테스트

---

## 체크리스트 요약

| ID | 우선순위 | 내용 | 상태 |
|----|--------|------|------|
| RFC-BE-001 | P3 | BoardDetailResDto 필드명 오타 수정 | ✅ 완료 (BUG-005) |
| RFC-BE-002 | P3 | 레거시 API 엔드포인트 Deprecated 처리 | ✅ 완료 |
| RFC-BE-003 | P3 | 서비스 레이어 메서드명 개선 | ✅ 완료 |
| RFC-BE-004 | P3 | 글로벌 예외 핸들러 정비 | ✅ 완료 (기존 구현됨) |
| RFC-BE-005 | P3 | 스케줄러 안정성 개선 | ⏳ 미완료 |
| RFC-BE-006 | P3 | N+1 쿼리 점검 및 최적화 | ⏳ 미완료 |
| RFC-BE-007 | P4 | 환경별 설정 분리 확인 | ⏳ 미완료 |
| RFC-BE-008 | P4 | 테스트 커버리지 확대 | ⏳ 미완료 |
| RFC-BE-009 | P2 | InitDb 운영 환경 안전 처리 | ✅ 완료 |
