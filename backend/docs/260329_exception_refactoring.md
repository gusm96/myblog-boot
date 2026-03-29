# Spring Boot 예외 처리 완전 정복 — 안티패턴에서 프로덕션 레벨까지

> 이 문서는 Spring Boot 3 + JPA 기반 블로그 프로젝트(myblog-boot)의 실제 코드를 분석하고, 예외 처리의 안티패턴을 식별하여 올바른 패턴으로 개선하는 과정을 다룬다.
> 

---

# 1. Java 예외 계층 구조 이해하기

Java의 예외는 `Throwable`을 정점으로 두 갈래로 나뉜다.

- **Error** — `OutOfMemoryError`, `StackOverflowError` 등 JVM 레벨의 심각한 문제. 애플리케이션에서 잡으면 안 된다.
- **Exception** — 애플리케이션 레벨에서 처리할 수 있는 예외. 여기서 다시 **Checked**와 **Unchecked**로 나뉜다.

## Checked vs Unchecked 핵심 정리

| 구분 | Checked Exception | Unchecked Exception |
| --- | --- | --- |
| 상속 | `Exception` 직접 상속 | `RuntimeException` 상속 |
| 컴파일 시 강제 | `try-catch` 또는 `throws` 필수 | 선택 사항 |
| 대표 예시 | `IOException`, `SQLException` | `NullPointerException`, `IllegalArgumentException` |
| Spring 트랜잭션 롤백 | 기본적으로 롤백 안 됨 | 기본적으로 롤백됨 |

**Spring Boot에서는 Unchecked(RuntimeException) 기반 커스텀 예외가 표준이다.** 이유는 다음과 같다.

1. 서비스 계층의 메서드 시그니처를 `throws` 선언으로 오염시키지 않는다.
2. Spring의 `@Transactional`이 기본적으로 RuntimeException에 대해서만 롤백한다.
3. `@ExceptionHandler`로 전역 처리할 때 호출부에서 강제로 잡을 필요가 없다.

---

# 2. 현재 코드의 안티패턴 분석

실제 프로젝트 코드에서 발견된 안티패턴들을 구체적으로 짚어본다.

## 안티패턴 ① : `catch (Exception e) → throw new RuntimeException`

```java
// ❌ BoardServiceImpl.java — 게시글 작성
try {
    Board result = boardRepository.save(newBoard);
    category.addBoard(result);
    return result.getId();
} catch (Exception e) {
    log.error("게시글 등록 중 에러 발생 : {}", e.getMessage());
    throw new RuntimeException("게시글 등록을 실패했습니다");
}
```

**왜 나쁜가?**

1. **원인 예외(cause)가 소실된다.** `new RuntimeException("메시지")`만 던지면 원래 어떤 예외가 발생했는지 스택트레이스에서 추적이 불가능하다. 새벽 3시에 장애 대응할 때 로그에 `RuntimeException: 게시글 등록을 실패했습니다`만 보이면 원인을 알 수 없다.
2. **`Exception`을 통째로 잡으면 예상치 못한 예외까지 삼켜버린다.** `NullPointerException`, `ClassCastException` 같은 버그성 예외도 "게시글 등록 실패"로 포장되어 버린다.
3. **GlobalExceptionHandler에서 `RuntimeException`을 잡아 500을 반환하므로**, 모든 비즈니스 실패가 500 Internal Server Error가 된다. 클라이언트는 자신의 잘못인지 서버의 잘못인지 구분할 수 없다.

## 안티패턴 ② : `e.printStackTrace()`로 로깅

```java
// ❌ BoardLikeServiceImpl.java — 좋아요 추가
catch (Exception e) {
    e.printStackTrace();
    throw new RuntimeException("게시글 좋아요를 실패했습니다.");
}
```

**왜 나쁜가?**

1. `e.printStackTrace()`는 `System.err`로 직접 출력한다. SLF4J/Logback 같은 로깅 프레임워크를 우회하므로 **로그 레벨 관리, 파일 출력, 패턴 포맷팅이 전혀 적용되지 않는다.**
2. 운영 환경에서 로그 수집기(ELK, CloudWatch 등)가 `System.err` 출력을 수집하지 못할 수 있다.
3. 멀티스레드 환경에서 출력이 뒤섞여 읽기 어렵다.

## 안티패턴 ③ : 성공/실패를 문자열로 반환

```java
// ❌ CategoryServiceImpl.java
public String create(String categoryName) {
    // ...
    categoryRepository.save(category);
    return "카테고리가 정상적으로 등록되었습니다.";
}

// ❌ CommentServiceImpl.java
public String delete(Long commentId, Long memberId) {
    // ...
    return "댓글이 삭제되었습니다.";
}
```

**왜 나쁜가?**

1. **API 응답의 일관성이 깨진다.** 어떤 API는 문자열을 반환하고, 어떤 API는 `Long`을, 어떤 API는 `BoardDetailResDto`를 반환한다. 클라이언트는 API마다 다른 파싱 로직을 작성해야 한다.
2. **국제화(i18n) 대응이 불가능하다.** 메시지가 코드에 하드코딩되어 있다.
3. **성공 응답에 메시지를 담을 필요가 없다.** HTTP 200 OK 자체가 "성공"을 의미한다. 실패 시에만 에러 정보를 담으면 된다.

## 안티패턴 ④ : GlobalExceptionHandler의 응답 형식 불일치

```java
// ❌ 현재 GlobalExceptionHandler.java
@ExceptionHandler(DuplicateKeyException.class)
public ResponseEntity<?> handleDuplicateKeyException(DuplicateKeyException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
}

@ExceptionHandler({RuntimeException.class, PersistenceException.class})
public ResponseEntity<?> handleRuntimeException(RuntimeException e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("서버 내부 오류가 발생했습니다.");
}
```

**왜 나쁜가?**

1. **응답 타입이 `ResponseEntity<?>`로 통일되지 않았다.** 어떤 핸들러는 `String`을, 어떤 핸들러는 `List`를 body에 담는다. 클라이언트가 에러 응답의 형식을 예측할 수 없다.
2. **에러 코드가 없다.** HTTP 상태코드만으로는 "중복 회원인지" "중복 카테고리인지" 구분할 수 없다. 커스텀 에러 코드가 필요하다.
3. `e.getMessage()`를 그대로 클라이언트에 노출하면, 내부 구현 정보가 유출될 수 있다.

---

# 3. 올바른 예외 처리 설계

## 3.1 표준 에러 응답 포맷 만들기

모든 에러 응답의 형식을 하나로 통일한다.

```java
// ✅ ErrorResponse.java
@Getter
public class ErrorResponse {
    private final String code;       // 애플리케이션 에러 코드
    private final String message;    // 사용자 친화적 메시지
    private final int status;        // HTTP 상태코드

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<FieldError> errors;  // Validation 에러 상세

    @Builder
    private ErrorResponse(String code, String message, int status,
                          List<FieldError> errors) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.errors = errors != null ? errors : List.of();
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .status(errorCode.getStatusValue())
                .build();
    }

    public static ErrorResponse of(ErrorCode errorCode,
                                   List<FieldError> errors) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .status(errorCode.getStatusValue())
                .errors(errors)
                .build();
    }

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String value;
        private final String reason;
    }
}
```

**응답 예시:**

```json
{
  "code": "BOARD_NOT_FOUND",
  "message": "해당 게시글이 존재하지 않습니다.",
  "status": 404,
  "errors": []
}
```

## 3.2 에러 코드 Enum 정의

```java
// ✅ ErrorCode.java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "유효하지 않은 입력입니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C002", "요청 값의 타입이 올바르지 않습니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "C003", "요청 본문을 읽을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "서버 내부 오류가 발생했습니다."),

    // 인증/인가
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 토큰입니다."),

    // 회원
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "회원이 존재하지 않습니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "M002", "이미 존재하는 아이디입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "M003", "비밀번호가 일치하지 않습니다."),

    // 게시글
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "해당 게시글이 존재하지 않습니다."),
    BOARD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "B002", "게시글 수정/삭제 권한이 없습니다."),
    DUPLICATE_BOARD_LIKE(HttpStatus.CONFLICT, "B003", "이미 좋아요한 게시글입니다."),

    // 댓글
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CM001", "해당 댓글이 존재하지 않습니다."),
    COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CM002", "댓글 수정/삭제 권한이 없습니다."),

    // 카테고리
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CT001", "해당 카테고리를 찾을 수 없습니다."),
    DUPLICATE_CATEGORY(HttpStatus.CONFLICT, "CT002", "이미 존재하는 카테고리입니다."),
    CATEGORY_HAS_BOARDS(HttpStatus.BAD_REQUEST, "CT003", "등록된 게시글이 존재해 삭제할 수 없습니다."),

    // 파일
    IMAGE_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "F001", "이미지 업로드를 실패했습니다."),
    IMAGE_DELETE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "F002", "이미지 삭제를 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    public int getStatusValue() {
        return status.value();
    }
}
```

**에러 코드 명명 규칙:**

- 도메인 접두사(C, A, M, B, CM, CT, F) + 숫자 3자리
- 새로운 도메인이 추가될 때 접두사만 정하면 된다.

## 3.3 커스텀 예외 클래스 설계

모든 비즈니스 예외의 공통 부모를 만들고, `ErrorCode`를 필수로 갖게 한다.

```java
// ✅ BusinessException.java — 모든 비즈니스 예외의 부모
@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
```

```java
// ✅ 도메인별 커스텀 예외 — 필요할 때만 만든다
public class EntityNotFoundException extends BusinessException {
    public EntityNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}

public class DuplicateException extends BusinessException {
    public DuplicateException(ErrorCode errorCode) {
        super(errorCode);
    }
}

public class UnauthorizedException extends BusinessException {
    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }
}
```

**커스텀 예외를 만드는 기준:**

- `GlobalExceptionHandler`에서 **같은 HTTP 상태코드 + 같은 처리 로직**으로 묶일 수 있는 단위로 만든다.
- 도메인마다 예외 클래스를 만들지 않는다. `BoardNotFoundException`, `CommentNotFoundException`을 각각 만들 필요 없이 `EntityNotFoundException` + `ErrorCode`로 구분한다.

## 3.4 GlobalExceptionHandler 재설계

```java
// ✅ GlobalExceptionHandler.java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 비즈니스 예외 — 모든 커스텀 예외를 한 번에 처리
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("Business exception: {} - {}",
                errorCode.getCode(), e.getMessage(), e);
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    // Validation 예외 — @Valid 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> fieldErrors =
                e.getBindingResult().getFieldErrors().stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .value(error.getRejectedValue() != null
                                ? error.getRejectedValue().toString() : "")
                        .reason(error.getDefaultMessage())
                        .build())
                .toList();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, fieldErrors));
    }

    // 타입 불일치 — @RequestParam, @PathVariable 타입 변환 실패
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch: param={}, value={}, requiredType={}",
                e.getName(), e.getValue(), e.getRequiredType());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_TYPE_VALUE));
    }

    // JSON 파싱 실패 — 잘못된 요청 본문
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException e) {
        log.warn("Message not readable: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST_BODY));
    }

    // 예상치 못한 예외 — 최후의 방어선
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected exception: ", e);  // 전체 스택트레이스 기록
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
```

**핵심 포인트:**

- `BusinessException` 하나로 모든 비즈니스 예외를 처리한다. 핸들러 메서드가 폭증하지 않는다.
- 비즈니스 예외는 `log.warn`, 예상치 못한 예외는 `log.error`로 레벨을 구분한다.
- 클라이언트에게는 항상 `ErrorResponse` 형태로 응답한다.
- `MethodArgumentTypeMismatchException`과 `HttpMessageNotReadableException`은 실무에서 자주 발생하는데 누락하기 쉽다. `/api/v1/boards/abc`처럼 Long 타입 PathVariable에 문자열이 들어오거나, JSON 형식이 깨진 요청이 올 때 이 핸들러가 없으면 Spring 기본 에러 페이지(HTML)가 반환되어 API 응답 일관성이 깨진다.
- `log.warn("...", e.getMessage(), e)` — 마지막 인자에 예외 객체 자체를 넘기면 SLF4J가 스택트레이스까지 함께 기록한다. 비즈니스 예외라도 cause chain을 추적할 수 있어야 디버깅이 가능하다.

---

# 4. Before/After 비교 — 실전 리팩토링

## 게시글 작성

**Before:**

```java
public Long write(BoardReqDto boardReqDto, Long memberId) {
    Member member = authService.retrieve(memberId);
    Category category = categoryService.retrieve(boardReqDto.getCategory());
    Board newBoard = boardReqDto.toEntity(category, member);
    try {
        if (boardReqDto.getImages() != null && boardReqDto.getImages().size() > 0) {
            saveImageFile(boardReqDto.getImages(), newBoard);
        }
        Board result = boardRepository.save(newBoard);
        category.addBoard(result);
        return result.getId();
    } catch (Exception e) {
        log.error("게시글 등록 중 에러 발생 : {}", e.getMessage());
        throw new RuntimeException("게시글 등록을 실패했습니다");
    }
}
```

**After:**

```java
@Transactional
public Long write(BoardReqDto boardReqDto, Long memberId) {
    Member member = authService.retrieve(memberId);
    Category category = categoryService.retrieve(boardReqDto.getCategory());
    Board newBoard = boardReqDto.toEntity(category, member);

    if (boardReqDto.getImages() != null && !boardReqDto.getImages().isEmpty()) {
        saveImageFile(boardReqDto.getImages(), newBoard);
    }
    Board result = boardRepository.save(newBoard);
    category.addBoard(result);
    return result.getId();
    // try-catch 제거 — 예외 발생 시 @Transactional이 롤백하고,
    // GlobalExceptionHandler가 처리한다.
}
```

**변경 이유:** `boardRepository.save()`에서 발생할 수 있는 예외는 JPA의 `DataAccessException` 계열이다. 이를 서비스에서 잡아서 RuntimeException으로 재포장할 이유가 없다. `@Transactional`이 자동 롤백하고, `GlobalExceptionHandler`의 `handleUnexpected()`가 500 에러로 처리한다.

## 좋아요 추가

**Before:**

```java
public Long addLikes(Long boardId, Long memberId) {
    if (isLiked(boardId, memberId)) {
        throw new DuplicateKeyException("이미 좋아요한 게시글 입니다.");
    }
    BoardForRedis board = boardService.getBoardFromCache(boardId);
    try {
        addBoardLike(boardId, memberId);
        return boardRedisRepository.incrementLikes(board).totalLikes();
    } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException("게시글 좋아요를 실패했습니다.");
    }
}
```

**After:**

```java
public Long addLikes(Long boardId, Long memberId) {
    if (isLiked(boardId, memberId)) {
        throw new DuplicateException(ErrorCode.DUPLICATE_BOARD_LIKE);
    }
    BoardForRedis board = boardService.getBoardFromCache(boardId);
    addBoardLike(boardId, memberId);
    return boardRedisRepository.incrementLikes(board).totalLikes();
    // e.printStackTrace() 제거 — 로깅은 GlobalExceptionHandler에서
    // 불필요한 try-catch 제거
}
```

## 댓글 수정 (권한 검사)

**Before:**

```java
public String update(Long commentId, Long memberId, String modifiedComment) {
    Comment findComment = retrieveWithMember(commentId);
    if (!findComment.getMember().getId().equals(memberId)) {
        throw new UnauthorizedAccessException("권한이 없습니다.");
    }
    findComment.updateComment(modifiedComment);
    return "댓글이 수정되었습니다.";
}
```

**After:**

```java
public void update(Long commentId, Long memberId, String modifiedComment) {
    Comment comment = retrieveWithMember(commentId);
    if (!comment.getMember().getId().equals(memberId)) {
        throw new UnauthorizedException(ErrorCode.COMMENT_ACCESS_DENIED);
    }
    comment.updateComment(modifiedComment);
    // 반환값 없음 — 성공은 HTTP 200 OK로 충분
}
```

## 카테고리 삭제

**Before:**

```java
public String delete(Long categoryId) {
    Category category = retrieve(categoryId);
    if (category.getBoards().size() > 0) {
        throw new RuntimeException("등록된 게시글이 존재해 삭제할 수 없습니다.");
    }
    try {
        categoryRepository.delete(category);
        return "카테고리가 삭제되었습니다.";
    } catch (Exception e) {
        log.error("카테고리 삭제 실패");
        throw new RuntimeException("카테고리 삭제 중 오류가 발생했습니다.");
    }
}
```

**After:**

```java
public void delete(Long categoryId) {
    Category category = retrieve(categoryId);
    if (!category.getBoards().isEmpty()) {
        throw new BusinessException(ErrorCode.CATEGORY_HAS_BOARDS);
    }
    categoryRepository.delete(category);
}
```

---

# 5. 계층별 예외 처리 책임 정리

각 계층이 예외에 대해 어떤 책임을 지는지 명확히 구분한다.

| 계층 | 책임 | 예시 |
| --- | --- | --- |
| **Controller** | 예외를 직접 처리하지 않는다. 요청 바인딩·검증만 수행. | `@Valid`로 DTO 검증 |
| **Service** | 비즈니스 규칙 위반 시 커스텀 예외를 던진다. `try-catch`는 복구 가능한 경우에만. | `throw new EntityNotFoundException(ErrorCode.BOARD_NOT_FOUND)` |
| **Repository** | Spring Data JPA가 자동 변환하는 `DataAccessException`을 그대로 전파. | JPA가 알아서 처리 |
| **GlobalExceptionHandler** | 모든 예외의 최종 처리자. 일관된 형식으로 응답. | `ErrorResponse` 반환 |

**핵심 원칙: 서비스 계층에서 `try-catch`를 쓰는 경우는 딱 두 가지뿐이다.**

1. **예외를 복구할 수 있을 때** — 재시도, 대안 로직 실행, 기본값 반환
2. **외부 시스템 예외를 비즈니스 예외로 변환할 때** — 예: IOException → ImageUploadException

그 외에는 `try-catch` 없이 예외를 그대로 전파하고, `GlobalExceptionHandler`에게 맡긴다.

---

# 6. 내 코드 점검 체크리스트

- [ ]  `catch (Exception e)` — 너무 넓은 범위를 잡고 있지 않은가? 구체적인 예외 타입으로 변경했는가?
- [ ]  `throw new RuntimeException("메시지")` — ErrorCode를 가진 커스텀 예외로 교체했는가?
- [ ]  `e.printStackTrace()` — `log.error("설명", e)`로 전부 교체했는가?
- [ ]  서비스 메서드가 `String` 메시지를 반환하고 있지 않은가? `void`로 변경하고 성공 여부는 HTTP 상태코드로 전달하는가?
- [ ]  GlobalExceptionHandler가 `ResponseEntity<String>` 대신 `ResponseEntity<ErrorResponse>`를 반환하는가?
- [ ]  모든 에러 응답이 동일한 JSON 구조(code, message, status)를 따르는가?
- [ ]  비즈니스 예외(`log.warn`)와 시스템 예외(`log.error`)의 로그 레벨이 구분되어 있는가?
- [ ]  서비스 계층의 `try-catch`가 복구 또는 예외 변환 목적인가? 아니라면 제거했는가?
- [ ]  커스텀 예외 클래스가 `ErrorCode`를 필수로 갖고 있는가?
- [ ]  예외 메시지에 내부 구현 정보(SQL, 스택트레이스 등)가 클라이언트에 노출되지 않는가?