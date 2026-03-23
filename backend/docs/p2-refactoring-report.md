# P2 리팩토링 리포트 — myblog-boot

> 작성일: 2026-03-12
> 기준 문서: `docs/code-review-report.md`
> 대상 우선순위: **P2** (유지보수성 · 보안 · 성능)
> 빌드 결과: **BUILD SUCCESSFUL** (91개 테스트 통과)

---

## 작업 요약

| # | 분류 | 항목 | 파일 | 상태 |
|---|------|------|------|------|
| 1 | 유지보수성 | Spring Security Lambda DSL 마이그레이션 | `WebSecurityConfig.java` | ✅ 완료 |
| 2 | 성능 | `synchronized` 캐시 조회 병목 제거 | `BoardServiceImpl.java` | ✅ 완료 |
| 3 | 성능 | N+1 쿼리 방지 — 게시글 목록 fetch join | `BoardRepository.java` | ✅ 완료 |
| 4 | 보안 | Authorization 헤더 NPE | `AuthController.java` | ✅ P1에서 완료 |
| 5 | 보안 | Cookie `Secure` + `SameSite` 플래그 | `CookieUtil.java` | ✅ P1에서 완료 |
| 6 | 보안 | JwtFilter Bearer 대소문자 처리 | `JwtFilter.java` | ✅ P1에서 완료 |
| 7 | 보안 | GlobalExceptionHandler 내부 메시지 노출 | `GlobalExceptionHandler.java` | ✅ P1에서 완료 |
| 8 | 보안 | `ExpiredRefreshTokenException` NPE | `GlobalExceptionHandler.java` | ✅ P1에서 완료 |

> **4~8번 항목**은 P1 작업(2026-03-11) 중 함께 수정 완료된 것을 이번 검증에서 확인함.

---

## 상세 변경 내역

### 1. Spring Security Lambda DSL 마이그레이션

**파일:** `src/main/java/com/moya/myblogboot/configuration/WebSecurityConfig.java`

**문제:** Spring Security 6.1+에서 deprecated된 `.and()` 체이닝 방식 사용.

```java
// Before — deprecated 체이닝 스타일
return http.httpBasic().disable()
        .csrf().disable()
        .cors().and()
        .authorizeHttpRequests()
        .requestMatchers(...)
        .and()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and().addFilterBefore(...).build();
```

```java
// After — Lambda DSL
return http
        .httpBasic(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/v1/boards").hasRole("ADMIN")
                // ... 동일 규칙 유지
                .anyRequest().permitAll()
        )
        .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .addFilterBefore(new JwtFilter(authService, secret), UsernamePasswordAuthenticationFilter.class)
        .build();
```

**추가된 import:**
- `org.springframework.security.config.Customizer`
- `org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer`

**효과:** deprecated API 제거, Spring Security 6.x 표준 준수, 향후 버전 업그레이드 대비.

---

### 2. `synchronized` 캐시 조회 병목 제거

**파일:** `src/main/java/com/moya/myblogboot/service/implementation/BoardServiceImpl.java:244`

**문제:** `getBoardFromCache(Long boardId)`에 `synchronized` 키워드가 적용되어, 모든 게시글 캐시 조회가 단일 모니터 락을 공유함. Board A 조회 중 Board B 조회도 블로킹되는 심각한 동시성 병목.

```java
// Before
public synchronized BoardForRedis getBoardFromCache(Long boardId) {
    Optional<BoardForRedis> boardForRedis = boardRedisRepository.findOne(boardId);
    if (boardForRedis.isEmpty()) {
        return retrieveBoardAndSetRedisStore(boardId);
    }
    return boardForRedis.get();
}
```

```java
// After
public BoardForRedis getBoardFromCache(Long boardId) {
    Optional<BoardForRedis> boardForRedis = boardRedisRepository.findOne(boardId);
    if (boardForRedis.isEmpty()) {
        return retrieveBoardAndSetRedisStore(boardId);
    }
    return boardForRedis.get();
}
```

**근거:**
- Redis(Lettuce)는 thread-safe하며 자체 원자성을 보장함.
- 캐시 미스 시 두 스레드가 동시에 DB를 조회하더라도 같은 값을 Redis에 저장하는 것이므로 데이터 정합성에 영향 없음 (멱등성 보장).
- `synchronized`는 JVM 수준의 단일 인스턴스 락으로, Redis 기반 분산 환경에서는 의미가 없음.

**효과:** 동시 게시글 조회 처리량 대폭 향상.

---

### 3. 게시글 목록 조회 N+1 쿼리 방지

**파일:** `src/main/java/com/moya/myblogboot/repository/BoardRepository.java:26`

**문제:** `findAll` 쿼리에 `member`, `category` fetch join이 없어 향후 `BoardResDto`가 해당 연관관계를 접근할 경우 N+1 쿼리 발생 가능. 또한 fetch join + pagination 조합에서 countQuery 미분리 시 Hibernate 경고 발생.

```java
// Before
@Query("select b from Board b where b.boardStatus = :boardStatus")
Page<Board> findAll(@Param("boardStatus") BoardStatus boardStatus, Pageable pageable);
```

```java
// After — fetch join + countQuery 분리
@Query(value = "select b from Board b join fetch b.member join fetch b.category where b.boardStatus = :boardStatus",
        countQuery = "select count(b) from Board b where b.boardStatus = :boardStatus")
Page<Board> findAll(@Param("boardStatus") BoardStatus boardStatus, Pageable pageable);
```

**효과:**
- 목록 조회 시 `member`, `category` 지연 로딩 쿼리 제거.
- `countQuery` 분리로 Hibernate의 `HHH90003004` 경고 방지 및 COUNT 쿼리 최적화.

---

## P2로 분류되었으나 이전에 완료된 항목

### 4. Authorization 헤더 NPE 방지 (2-1)

**파일:** `AuthController.java:85-91`

P1 작업 중 수정 완료. `null` 체크 및 `toLowerCase().startsWith("bearer ")` 적용.

### 5. Cookie `Secure` + `SameSite` 플래그 (2-4)

**파일:** `CookieUtil.java`

P1 작업 중 수정 완료. `setSecure(true)` + `setAttribute("SameSite", "Lax")` 적용.

### 6. JwtFilter Bearer 대소문자 처리 (2-2)

**파일:** `JwtFilter.java:41`

P1 작업 중 수정 완료. `authorization.toLowerCase().startsWith("bearer ")` 적용.

### 7. GlobalExceptionHandler 내부 메시지 노출 (2-5)

**파일:** `GlobalExceptionHandler.java:82-86`

P1 작업 중 수정 완료. `RuntimeException` 핸들러가 `"서버 내부 오류가 발생했습니다."` 고정 메시지 반환 + `log.error`로 내부 기록.

### 8. `ExpiredRefreshTokenException` NPE (2-6)

**파일:** `GlobalExceptionHandler.java:72-79`

P1 작업 중 수정 완료. `refreshTokenCookie != null` 조건 체크 후 `deleteCookie` 호출.

---

## 테스트 결과

```
BUILD SUCCESSFUL
91 tests passed, 0 failures, 0 errors
```

---

## 다음 단계 (P3)

| 항목 | 내용 |
|------|------|
| Java 17 Record 활용 | `Token`, `TokenInfo`, `TokenReqDto` 등 불변 DTO → Record 변환 |
| Pattern Matching | `BoardController`, `CommentController`의 `instanceof` 패턴 매칭 적용 |
| `getMemberId()` 중복 제거 | `BoardController`, `CommentController` 공통 유틸로 추출 |
| Raw Type 제거 | `CommentController`, `CategoryController`의 `List` → 제네릭 타입 지정 |
| 미사용 import 정리 | `CommentController` 외 다수 |
| snake_case 필드명 수정 | `Token.java`, `Comment.java` |
| devtools 중복 선언 제거 | `build.gradle` |
| lettuce-core 수동 의존성 제거 | `build.gradle` |
