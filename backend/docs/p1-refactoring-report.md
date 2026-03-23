# P1 리팩토링 작업 보고서

> 작성일: 2026-03-11
> 기반 문서: `docs/code-review-report.md`
> 검증: 컴파일 성공 (`./gradlew compileJava`)
> 2차 검토: context7 MCP 기반 공식 문서 대조 완료

---

## 1. `@Async` 내부 호출 무효 수정 — P1

### 기존 코드의 문제점

**`BoardServiceImpl.java`**

```java
// 같은 클래스에서 @Async 메서드를 직접 호출 → Spring AOP 프록시를 우회하여 동기 실행됨
@Async
protected void updateBoardForRedis(Board board) { ... }

@Async
protected void deleteBoardForRedis(Long boardId) { ... }

// 동일 클래스 내 호출 → @Async 무효
updateBoardForRedis(board);  // edit(), delete(), undelete()에서 호출
deleteBoardForRedis(board.getId());  // deleteBoards()에서 호출
```

**`BoardLikeServiceImpl.java`**

```java
// 마찬가지로 같은 클래스 내부에서 @Async 메서드 호출 → 동기 실행
@Async
void addBoardLike(Long boardId, Long memberId) { ... }

@Async
public void deleteBoardLike(Long boardId, Long memberId) { ... }
```

또한 `@EnableAsync`가 `MyblogBootApplication`에 선언되어 있지 않아 `@Async` 어노테이션 자체가 동작하지 않는 상태였다.

### context7 공식 문서 대조 결과

Spring Framework 공식 문서(`spring-projects/spring-framework`)에서 확인한 핵심 제약:

> `@Async` 메서드는 반드시 **외부 빈 인스턴스(프록시 경유)**를 통해 호출해야 한다. 동일 클래스 내에서 `this.doSomething()`으로 호출하면 Spring 프록시를 우회하여 **동기 실행**된다.

```java
// 공식 문서 — 올바른 패턴
public class SampleBeanImpl implements SampleBean {
    @Async
    void doSomething() { ... }
}
// 반드시 외부에서 주입받은 빈을 통해 호출
sampleBean.doSomething();  // ← 프록시 경유 → 비동기 실행
```

### 코드 수정 내용

**`MyblogBootApplication.java`** — `@EnableAsync` 추가

```diff
+ import org.springframework.scheduling.annotation.EnableAsync;

+ @EnableAsync
  @EnableScheduling
  @SpringBootApplication
  public class MyblogBootApplication { ... }
```

**`BoardCacheService.java`** — 신규 생성 (Redis 캐시 비동기 처리 전담)

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class BoardCacheService {

    private final BoardRedisRepository boardRedisRepository;

    // 수정/삭제 상태 변경 — 비동기 (캐시 반영 지연 허용)
    @Async
    public void updateBoard(BoardForRedis boardForRedis, Board board) {
        boardForRedis.update(board);
        try {
            boardRedisRepository.update(boardForRedis);
        } catch (Exception e) {
            log.error("Redis 캐시 업데이트 실패 (boardId={}): {}", boardForRedis.getId(), e.getMessage());
        }
    }

    // 영구 삭제 — 동기 (삭제 완료 후 DB 삭제와 순서 보장 필요)
    public void deleteBoard(BoardForRedis boardForRedis) {
        try {
            boardRedisRepository.delete(boardForRedis);
        } catch (Exception e) {
            log.error("Redis 캐시 삭제 실패 (boardId={}): {}", boardForRedis.getId(), e.getMessage());
        }
    }
}
```

**`BoardServiceImpl.java`** — `BoardCacheService` 주입, 호출부 교체

```diff
+ private final BoardCacheService boardCacheService;

  // edit()
- updateBoardForRedis(board);
+ boardCacheService.updateBoard(getBoardFromCache(board.getId()), board);

  // delete()
- updateBoardForRedis(board);
+ boardCacheService.updateBoard(getBoardFromCache(board.getId()), board);

  // undelete()
- updateBoardForRedis(board);
+ boardCacheService.updateBoard(getBoardFromCache(board.getId()), board);

  // deleteBoards() private
+ BoardForRedis boardForRedis = getBoardFromCache(board.getId());
- deleteBoardForRedis(board.getId());
+ boardCacheService.deleteBoard(boardForRedis);

  // 기존 @Async private 메서드 전체 제거
- @Async
- protected void updateBoardForRedis(Board board) { ... }
- @Async
- protected void deleteBoardForRedis(Long boardId) { ... }
```

**`BoardLikeServiceImpl.java`** — `@Async` 제거

```diff
- @Async
  void addBoardLike(Long boardId, Long memberId) { ... }

- @Async
  public void deleteBoardLike(Long boardId, Long memberId) { ... }
```

`addBoardLike`, `deleteBoardLike`는 DB 쓰기 작업이므로 **비동기로 실행 시 데이터 일관성을 보장할 수 없다**. Redis 카운터(`incrementLikes`)보다 DB 반영이 늦어지면 트랜잭션 롤백이 발생해도 Redis 카운터는 이미 변경된 상태로 남는 문제가 있어 `@Async`를 제거하고 동기로 처리한다.

### `@Async` void 예외 처리 설계

공식 문서에 따르면 `@Async` void 메서드에서 발생한 예외는 기본적으로 로그만 남기고 소실된다. `BoardCacheService`에서는 내부 try-catch로 예외를 직접 처리했다. 추가로 전역 `AsyncUncaughtExceptionHandler` 설정을 통해 다른 `@Async` 메서드의 미처리 예외를 대비한다:

```java
// 향후 적용 권장 — AsyncConfig.java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
            log.error("@Async 예외 발생 [{}]: {}", method.getName(), ex.getMessage(), ex);
    }
}
```

---

## 2. Redis SCAN 커넥션 누수 수정 — P1

### 기존 코드의 문제점

**`BoardRedisRepositoryImpl.java:27-38`**

```java
// ⚠️ getConnection()으로 직접 획득한 커넥션이 반환되지 않음 → 풀 누수
RedisKeyCommands keyCommands = redisTemplate.getRequiredConnectionFactory()
        .getConnection().keyCommands();
ScanOptions options = ScanOptions.scanOptions().match(pattern).build();
Cursor<byte[]> cursor = keyCommands.scan(options);
// cursor도 close되지 않음
```

`getConnection()`으로 직접 커넥션을 획득하면 Spring의 커넥션 관리 범위를 벗어나 **자동 반환이 되지 않는다**. 스케줄러가 주기적으로 `getKeys()`를 호출하므로 누적 시 커넥션 풀이 소진될 수 있다.

### context7 공식 문서 대조 결과

Spring Data Redis 공식 문서에서 확인한 표준 패턴:

```java
// 공식 문서 — RedisCallback을 통한 커넥션 관리 (자동 획득/반환)
redisOperations.execute(new RedisCallback<Object>() {
    public Object doInRedis(RedisConnection connection) throws DataAccessException {
        // connection 라이프사이클을 Spring이 관리
        return connection.dbSize();
    }
});
```

`execute(RedisCallback)`은 커넥션 획득/반환을 Spring이 자동 처리하며, callback 범위를 벗어나면 커넥션이 즉시 반환된다.

### 코드 수정 내용

```diff
  @Override
  public Set<Long> getKeys(String pattern) {
-     RedisKeyCommands keyCommands = redisTemplate.getRequiredConnectionFactory()
-             .getConnection().keyCommands();
-     ScanOptions options = ScanOptions.scanOptions().match(pattern).build();
-     Cursor<byte[]> cursor = keyCommands.scan(options);
-     Set<Long> keys = new HashSet<>();
-     while (cursor.hasNext()) {
-         String key = new String(cursor.next());
-         keys.add(Long.parseLong(key.split(":")[1]));
-     }
-     return keys;
+     return redisTemplate.execute((RedisCallback<Set<Long>>) connection -> {
+         Set<Long> keys = new HashSet<>();
+         ScanOptions options = ScanOptions.scanOptions().match(pattern).build();
+         try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
+             while (cursor.hasNext()) {
+                 String key = new String(cursor.next());
+                 keys.add(Long.parseLong(key.split(":")[1]));
+             }
+         }
+         return keys;
+     });
  }
```

**개선 포인트:**

| 항목 | 기존 | 수정 |
|------|------|------|
| 커넥션 관리 | 직접 획득 (`getConnection()`), 반환 없음 | `RedisCallback` — Spring이 자동 관리 |
| Cursor 반환 | close 없음 | try-with-resources로 자동 close |

---

## 3. `deletePermanently(LocalDateTime)` 실제 삭제 미수행 수정 — P1

### 기존 코드의 문제점

**`BoardServiceImpl.java:172-175`**

```java
@Transactional
public void deletePermanently(LocalDateTime thresholdDate) {
    List<Board> boards = boardRepository.findByDeleteDate(thresholdDate);
    boards.stream().forEach(Board::deleteBoard);  // ⚠️ 상태 변경만, DB/Redis/S3 삭제 없음
}
```

`Board::deleteBoard`는 `BoardStatus`를 변경하는 메서드로, 실제로 어떤 저장소에서도 데이터를 삭제하지 않는다. 스케줄러가 이 메서드를 호출해도 게시글이 영구 삭제되지 않는 기능 결함이었다.

### 코드 수정 내용

```diff
  @Transactional
  public void deletePermanently(LocalDateTime thresholdDate) {
      List<Board> boards = boardRepository.findByDeleteDate(thresholdDate);
-     boards.stream().forEach(Board::deleteBoard);
+     boards.forEach(this::deleteBoards);
  }
```

`deleteBoards(Board board)`는 다음 순서로 완전 삭제를 수행한다:

```java
private void deleteBoards(Board board) {
    BoardForRedis boardForRedis = getBoardFromCache(board.getId()); // 1. Redis 캐시 조회
    fileUploadService.deleteFiles(board.getImageFiles());           // 2. S3 이미지 삭제
    boardCacheService.deleteBoard(boardForRedis);                   // 3. Redis 캐시 삭제
    boardRepository.delete(board);                                  // 4. DB 삭제
}
```

---

## 변경 파일 목록

| 파일 | 변경 유형 |
|------|----------|
| `MyblogBootApplication.java` | `@EnableAsync` 추가 |
| `BoardCacheService.java` | 신규 생성 — Redis 캐시 비동기 처리 전담 |
| `BoardServiceImpl.java` | `BoardCacheService` 주입, `@Async` private 메서드 제거, `deletePermanently` 수정 |
| `BoardLikeServiceImpl.java` | `addBoardLike`, `deleteBoardLike`의 `@Async` 제거 |
| `BoardRedisRepositoryImpl.java` | `getKeys()` 커넥션 누수 수정 |

## 검증 결과

| 항목 | 결과 |
|------|------|
| `./gradlew compileJava` | BUILD SUCCESSFUL |
| context7 공식 문서 대조 | 전 항목 공식 API 패턴과 일치 확인 |

## 잔여 P1 항목

| 항목 | 상태 | 비고 |
|------|------|------|
| `spring-cloud-aws 2.x` → 3.x 마이그레이션 | 보류 | S3 실사용 계획 있음, 별도 작업으로 진행 |
