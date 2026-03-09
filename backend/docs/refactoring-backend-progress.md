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

### RFC-BE-005 (P3) — 조회수 업데이트 스케줄러 안정성 개선

**상태**: 1단계 완료 (단일 인스턴스 안정성 확보)

**변경 내용**:
- `ReentrantLock` 도입 (`tryLock`으로 논블로킹 중복 실행 방지)
- 개별 게시글 단위 에러 핸들링 (하나의 실패가 전체 작업을 중단하지 않음)
- 캐시 삭제 실패 시 안전 처리 (예외 전파 없이 로깅)
- 처리 결과 로깅 개선 (성공/실패 건수 표시)
- 단위 테스트 추가 (`BoardScheduledTaskTest.java` — 4개 케이스)

**향후 2단계**: 다중 인스턴스 배포 시 ShedLock 또는 Redis SET NX 분산 락 도입 필요

상세 내용: [rfc-be-005-scheduler-stability.md](./rfc-be-005-scheduler-stability.md)

---

### RFC-BE-006 (P3) — N+1 쿼리 점검 및 최적화

**상태**: 완료

**점검 결과**: 게시글 목록 쿼리에서 N+1 문제 없음 (`BoardResDto`가 스칼라 필드만 접근)

**수정 내용**:
- 검색 카운트 쿼리 최적화: `fetch().size()` → `select(board.count())` (전체 엔티티 메모리 로드 제거)
- `JPAQueryFactory` 스레드 안전성 개선: 인스턴스 변수 → Spring 빈 주입 (`QuerydslConfig.java` 추가)
- 대상: `BoardQuerydslRepositoryImpl`, `CommentQuerydslRepositoryImpl`, `CategoryQuerydslRepositoryImpl`

상세 내용: [rfc-be-006-n-plus-one-query.md](./rfc-be-006-n-plus-one-query.md)

---

## 미완료 항목

### RFC-BE-007 (P4) — 환경별 설정 분리

**상태**: 완료

**변경 내용**:
- `application.yaml` → 공통 설정만 유지 (서버, JPA 공통, Redis, JWT, S3)
- `application-dev.yaml` (신규) → 로컬 개발용: show-sql=true, ddl-auto=update, verbose logging
- `application-prod.yaml` (신규) → 운영용: show-sql=false, ddl-auto=validate, sql.init.mode=never
- `docker-compose.yaml` → `SPRING_PROFILES_ACTIVE: prod` 추가, Redis 환경변수명 정합성 수정

**민감 정보 환경변수 처리 현황**: 모두 완료
| 항목 | 환경변수 |
|------|---------|
| DB 접속 정보 | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` |
| Redis 접속 | `REDIS_HOST`, `REDIS_PORT` (기본값: localhost, 6379) |
| JWT 시크릿 | `JWT_SECRET_KEY` |
| AWS S3 | `AWS_CREDENTIALS_ACCESS_KEY`, `AWS_CREDENTIALS_SECRET_KEY` |

**로컬 실행 시**: `-Dspring.profiles.active=dev` 또는 `SPRING_PROFILES_ACTIVE=dev` 설정 필요

---

### RFC-BE-008 (P4) — 테스트 커버리지 확대

**상태**: 완료

**추가된 테스트 (3개 파일, 총 26개 케이스)**:

| 파일 | 테스트 수 | 주요 검증 항목 |
|------|---------|--------------|
| `BoardServiceImplTest` | 12 | 목록 조회, 작성/수정/삭제(권한 검사), 복원, 조회수 중복 검사 |
| `CategoryServiceImplTest` | 7 | 생성(중복 검사), 수정, 삭제(게시글 존재 시 예외), 조회 |
| `CommentServiceImplTest` | 7 | 댓글/대댓글 작성, 수정/삭제(권한 검사), 목록 조회 |

**테스트 환경 개선**:
- `application-test.yaml`에 JWT/AWS 환경변수 기본값 추가 → 테스트 환경 자립성 확보

---

## 체크리스트 요약

| ID | 우선순위 | 내용 | 상태 |
|----|--------|------|------|
| RFC-BE-001 | P3 | BoardDetailResDto 필드명 오타 수정 | ✅ 완료 (BUG-005) |
| RFC-BE-002 | P3 | 레거시 API 엔드포인트 Deprecated 처리 | ✅ 완료 |
| RFC-BE-003 | P3 | 서비스 레이어 메서드명 개선 | ✅ 완료 |
| RFC-BE-004 | P3 | 글로벌 예외 핸들러 정비 | ✅ 완료 (기존 구현됨) |
| RFC-BE-005 | P3 | 스케줄러 안정성 개선 | ✅ 완료 (1단계) |
| RFC-BE-006 | P3 | N+1 쿼리 점검 및 최적화 | ✅ 완료 |
| RFC-BE-007 | P4 | 환경별 설정 분리 확인 | ✅ 완료 |
| RFC-BE-008 | P4 | 테스트 커버리지 확대 | ✅ 완료 |
| RFC-BE-009 | P2 | InitDb 운영 환경 안전 처리 | ✅ 완료 |
