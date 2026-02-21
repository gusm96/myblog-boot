# 백엔드 리팩토링 계획

---

## RFC-BE-001: BoardDetailResDto 필드명 오타 수정

**우선순위**: P3
**대상 파일**: `src/main/java/com/moya/myblogboot/dto/board/BoardDetailResDto.java`

### 현황
```java
private LocalDateTime creatDate;  // 오타
```

### 개선 방향
```java
private LocalDateTime createDate;  // 수정
```

> **주의**: API 응답 필드명이 변경되므로, 프론트엔드에서 `creatDate`로 접근 중인 코드도 함께 수정 필요.
> `@JsonProperty("creatDate")`로 하위 호환성 유지 후 순차적으로 제거하는 방법도 고려.

---

## RFC-BE-002: API 버전 정리

**우선순위**: P3
**대상 파일**: `src/main/java/com/moya/myblogboot/controller/BoardController.java`

### 현황
동일한 게시글 조회 API가 v4 ~ v7까지 존재. 현재 프론트엔드는 v7을 사용 중.

```java
// v4, v5, v6는 레거시 - 사용 여부 확인 필요
@GetMapping("/api/v4/boards/{boardId}")
@GetMapping("/api/v5/boards/{boardId}")
@GetMapping("/api/v6/boards/{boardId}")
@GetMapping("/api/v7/boards/{boardId}")  // 현재 사용
```

### 개선 방향
1. 현재 사용 중인 v7 확인 후 v1으로 통합 (또는 최종 버전만 유지)
2. 레거시 버전은 Deprecated 주석 후 제거 일정 수립
3. API 버전 관리 정책 문서화 → [api-strategy.md](./api-strategy.md) 참고

---

## RFC-BE-003: 서비스 레이어 메서드 명확성 개선

**우선순위**: P3
**대상 파일**: `src/main/java/com/moya/myblogboot/service/BoardService.java`

### 현황
유사한 역할의 메서드가 여러 개 존재하여 혼란스러움.

```java
Board retrieve(Long boardId);
BoardDetailResDto retrieveDto(Long boardId);
BoardDetailResDto retrieveAndIncrementViewsDto(Long boardId);
BoardForRedis retrieveBoardInRedisStore(Long boardId);
```

### 개선 방향
메서드 이름에 의도를 명확히 표현.

```java
// 엔티티 조회 (내부 사용)
Board findById(Long boardId);

// 상세 DTO 조회 (조회수 미증가 - 관리자 등)
BoardDetailResDto getBoardDetail(Long boardId);

// 상세 DTO 조회 + 조회수 증가 (일반 사용자)
BoardDetailResDto getBoardDetailAndIncrementViews(Long boardId);

// Redis 캐시에서 조회
BoardForRedis getBoardFromCache(Long boardId);
```

---

## RFC-BE-004: 예외 처리 일관성 개선

**우선순위**: P3

### 현황
- 서비스 레이어에서 예외 종류가 혼재될 가능성
- 컨트롤러에서 예외 처리 방식 불일치

### 개선 방향
`@ControllerAdvice` 기반 글로벌 예외 핸들러 강화.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(e.getMessage()));
    }
}
```

---

## RFC-BE-005: 조회수 업데이트 스케줄러 안정성 개선

**우선순위**: P3
**대상 파일**: `src/main/java/com/moya/myblogboot/scheduler/BoardScheduledTask.java`

### 현황
Redis 캐시에 쌓인 조회수를 주기적으로 DB에 반영하는 스케줄러.

### 확인 필요 사항
- 스케줄러 실패 시 조회수 유실 가능성
- 분산 환경 배포 시 중복 실행 방지 (Redis 분산 락 필요 여부)
- 트랜잭션 처리 방식

### 개선 방향
```java
@Scheduled(cron = "0 0 * * * *")
@SchedulerLock(name = "boardViewsSync", lockAtMostFor = "PT10M")
public void syncBoardViewsToDB() {
    // Redis → DB 동기화
    // 실패 시 재시도 또는 알림
}
```

---

## RFC-BE-006: 불필요한 N+1 쿼리 점검

**우선순위**: P3
**대상 파일**: 리포지토리 및 서비스 레이어

### 확인 포인트
게시글 목록 조회 시 각 게시글마다 Member, Category를 개별 조회하는지 확인.

```java
// 문제 가능성 있는 패턴
List<Board> boards = boardRepository.findAll();
boards.forEach(board -> {
    board.getMember().getNickname();   // N번 쿼리
    board.getCategory().getName();     // N번 쿼리
});

// 개선 방향: Fetch Join 사용
@Query("SELECT b FROM Board b JOIN FETCH b.member JOIN FETCH b.category WHERE ...")
List<Board> findAllWithMemberAndCategory(Pageable pageable);
```

---

## RFC-BE-007: 환경별 설정 분리 확인

**우선순위**: P4

### 확인 필요 사항
- `application.yml` / `application-prod.yml` / `application-dev.yml` 분리 여부
- 개발 환경에서 H2, 프로덕션에서 실제 DB 사용 여부
- 민감 정보(DB 비밀번호, JWT 시크릿, AWS 키) 환경 변수 처리 여부

---

## RFC-BE-008: 테스트 커버리지 확대

**우선순위**: P4
**현재 테스트 파일**:
- `BoardControllerTest.java`
- `BoardRedisRepositoryTest.java`
- `VisitorCountServiceImplTest.java`
- `VisitorCountServiceV2ImplTest.java`

### 추가 필요 테스트
- `BoardServiceImpl` 단위 테스트
- `CategoryService` 단위 테스트
- `CommentService` 단위 테스트
- 인증 관련 통합 테스트

---

## RFC-BE-009: InitDb.java 운영 환경 안전 처리

**우선순위**: P2
**대상 파일**: `src/main/java/com/moya/myblogboot/configuration/InitDb.java`

### 현황
초기 데이터 삽입을 담당하는 `InitDb` 클래스가 프로덕션 환경에서 실행될 위험이 있음.

### 개선 방향
`@Profile("dev")` 또는 `@ConditionalOnProperty`로 개발 환경에서만 실행되도록 제한.

```java
@Component
@Profile("dev")  // 개발 환경에서만 실행
public class InitDb implements ApplicationRunner {
    // 초기 데이터 삽입
}
```

---

## 작업 진행 체크리스트

- [ ] RFC-BE-001: BoardDetailResDto 필드명 오타 수정 (프론트 함께)
- [ ] RFC-BE-002: API 버전 정리 (레거시 엔드포인트 제거)
- [ ] RFC-BE-003: 서비스 레이어 메서드명 개선
- [ ] RFC-BE-004: 글로벌 예외 핸들러 정비
- [ ] RFC-BE-005: 스케줄러 안정성 개선
- [ ] RFC-BE-006: N+1 쿼리 점검 및 최적화
- [ ] RFC-BE-007: 환경별 설정 분리 확인
- [ ] RFC-BE-008: 테스트 커버리지 확대
- [ ] RFC-BE-009: InitDb 운영 환경 안전 처리
