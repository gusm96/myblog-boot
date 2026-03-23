# 테스트 코드 점검 보고서

> 작성일: 2026-03-10 (최종 수정: 2026-03-10)
> 검증 방법: context7 MCP 기반 공식 문서 대조 (Testcontainers `/testcontainers/testcontainers-java`, Spring Boot `/spring-projects/spring-boot`)
> 대상: `src/test/java/com/moya/myblogboot/` 전체 21개 테스트 파일

---

## 요약

| 심각도 | 건수 | 상태 |
|--------|------|------|
| P0 (즉시 수정) | 2 | **수정 완료** |
| P1 (개선 권장) | 3 | 1건 수정 완료, 2건 잔여 |
| P2 (품질 개선) | 4 | 잔여 |

### Testcontainers 통합 — 수정 완료

- **Testcontainers 2.0.3** + **Jackson 2.17.2** 강제 업그레이드로 Docker Desktop 4.57.0 호환 문제 해결
- `AbstractContainerBaseTest`: Singleton Container 패턴 (`redis:7-alpine`) + `@DynamicPropertySource`
- 모든 18개 `@SpringBootTest` 클래스가 `AbstractContainerBaseTest` 상속
- **전체 91개 테스트 통과** (0 failures, 0 errors, 0 skipped)

---

## P0 — 수정 완료

### 1. `MyblogBootApplicationTests` — 미사용 import 제거 + `@ActiveProfiles` 추가 + Testcontainers 연동

**수정 내역:**
- Testcontainers 관련 dead import 7개 제거
- `@ActiveProfiles("test")` 추가
- `extends AbstractContainerBaseTest` 추가

### 2. `VisitorCountServiceV2ImplTest` — `@ActiveProfiles("test")` 추가 + Testcontainers 연동

**수정 내역:**
- `@ActiveProfiles("test")` 추가
- `extends AbstractContainerBaseTest` 추가

---

## P1 — 개선 권장

### 3. `AuthServiceImplTest` — `RedisTemplate<Long, Object>` 타입 불일치 **[수정 완료]**

**수정 내역:**
- `RedisTemplate<Long, Object>` → `RedisTemplate<String, Object>` 변경
- 관련 `createTemporaryNumber()` 테스트에서 key 타입을 `String.valueOf()`로 변환

### 4. `VisitorCountServiceV2ImplTest` — 빈 테스트 메서드 **[잔여]**

```java
@Test
@DisplayName("Redis Store에서 VisitorCount 찾기")
void retrieveVisitorCountFromRedisStore() {
    // given
    // when
    // then
}
```

- 테스트 본문이 비어 있어 아무것도 검증하지 않는다. `@Disabled("구현 예정")` 추가 권장.

### 5. `FileUploadControllerTest` — `@MockBean` deprecated (Spring Boot 3.4+) **[잔여]**

- 현재 Spring Boot 3.0.4에서는 문제없음. 향후 업그레이드 시 `@MockitoBean` 전환 필요.

---

## P2 — 품질 개선 (잔여)

### 6. 컨트롤러 테스트 — 다중 `@BeforeEach` 실행 순서 미보장

- JUnit 5에서 같은 클래스 내 다중 `@BeforeEach`의 실행 순서 보장 안 됨
- 현재 실제 문제는 없으나 단일 메서드로 통합 권장

### 7. `BoardRedisRepositoryTest` — `static` 필드의 부적절한 사용

- `@Transactional` 롤백 환경에서 `static` 필드 사용 → 테스트 격리 위반 가능성
- `private Long memberId;`로 변경 권장

### 8. `VisitorCountServiceImplTest` — `assertThat()` 미종결

```java
// 현재 (assertion 체인 미종결 → 항상 통과)
Assertions.assertThat(result.getTotal() > before.getTotal());

// 수정 방향
Assertions.assertThat(result.getTotal()).isGreaterThan(before.getTotal());
```

### 9. `CategoryQueryRepositoryTest` — assertion 없는 테스트

- `System.out.println`으로 결과를 출력할 뿐 assertion이 없어 항상 통과한다.

---

## 주석 처리된 Dead Code

| 파일 | 내용 |
|------|------|
| `BoardViewsServiceTest.java` | 파일 전체가 `/* */` 블록 주석 처리 |
| `VisitorCountServiceImplTest.java:35-51` | `@BeforeEach` 메서드가 블록 주석 처리 |
| `BoardRedisRepositoryTest.java:87-113` | `게시글_좋아요_V2` 테스트가 블록 주석 처리 |

---

## Testcontainers 설정 상세

### 의존성 (`build.gradle`)

```groovy
testImplementation 'org.testcontainers:testcontainers:2.0.3'
testImplementation 'org.testcontainers:testcontainers-junit-jupiter:2.0.3'

// Jackson 버전 강제 업그레이드 (Testcontainers 2.x 호환)
dependencyManagement {
    dependencies {
        dependencySet(group: 'com.fasterxml.jackson.core', version: '2.17.2') {
            entry 'jackson-databind'
            entry 'jackson-core'
            entry 'jackson-annotations'
        }
    }
}
```

### 왜 Jackson 업그레이드가 필요한가?

- Docker Desktop 4.57.0은 Docker API 최소 v1.44를 요구
- Testcontainers 1.x (docker-java 3.4.x)는 API v1.32를 하드코딩 → 400 Bad Request
- Testcontainers 2.x (docker-java 4.x)는 API v1.44+ 지원하지만, Jackson 2.17+ 필요
- Spring Boot 3.0.4의 기본 Jackson 2.14.2와 충돌 → `dependencyManagement`로 2.17.2 강제

### Singleton Container 패턴 (`AbstractContainerBaseTest.java`)

```java
public abstract class AbstractContainerBaseTest {
    static final GenericContainer<?> REDIS_CONTAINER;

    static {
        REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        REDIS_CONTAINER.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
    }
}
```

- `static {}` 초기화 블록으로 JVM 당 한 번만 Redis 컨테이너 시작
- `@DynamicPropertySource`로 Spring에 동적 host/port 주입
- `application-test.yaml`에서 Redis host/port 제거 (Testcontainers가 주입)

---

## 테스트 실행 결과 (최종)

| 테스트 그룹 | 개수 | 결과 |
|-------------|------|------|
| `AuthControllerTest` | 10 | **통과** |
| `BoardControllerTest` | 17 | **통과** |
| `CategoryControllerTest` | 9 | **통과** |
| `CommentControllerTest` | 7 | **통과** |
| `FileUploadControllerTest` | 2 | **통과** |
| `MyblogBootApplicationTests` | 1 | **통과** |
| `BoardRedisRepositoryTest` | 1 | **통과** |
| `BoardViewCountRedisRepositoryTest` | 1 | **통과** |
| `CategoryQueryRepositoryTest` | 1 | **통과** |
| `MemberRepositoryTest` | 2 | **통과** |
| `BoardScheduledTaskTest` | 4 | **통과** |
| `AuthServiceImplTest` | 5 | **통과** |
| `BoardServiceImplTest` | 12 | **통과** |
| `CategoryServiceImplTest` | 7 | **통과** |
| `CommentServiceImplTest` | 7 | **통과** |
| `VisitorCountServiceImplTest` | 3 | **통과** |
| `VisitorCountServiceV2ImplTest` | 1 | **통과** |
| `TemporaryNumberServiceTest` | 1 | **통과** |

**총 91개 테스트: 91 통과, 0 실패, 0 스킵**

---

## 수정 이력

| 날짜 | 내용 |
|------|------|
| 2026-03-10 | 초기 점검 보고서 작성 (91개 중 7개 실패) |
| 2026-03-10 | P0 2건 + P1 1건 수정 완료 (7개 실패 → 0개 실패) |
| 2026-03-10 | Testcontainers 2.0.3 통합 완료 (Docker Desktop 4.57 호환), Jackson 2.17.2 업그레이드 |
