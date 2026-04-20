# Next.js On-demand Revalidation Webhook 연동 계획서

- **작성일**: 2026-04-20
- **작업 범위**: backend (`-b`)
- **전제 / 짝 문서**: `frontend/docs/260420_app-router-top3-implementation-plan.md` (Top 1) — **이미 커밋됨** (`5fe9069`)
- **Contract**: `POST /api/revalidate` (Next.js) — `x-revalidate-secret` 헤더 + `{ tags?: string[], paths?: string[] }` 바디

---

## 1. Problem (문제 정의)

### 1-1. 무엇이 문제인가?

프론트엔드 측에서 on-demand revalidation 엔드포인트(`POST /api/revalidate`) 는 **이미 준비됨** (커밋 `5fe9069`). 하지만:
- 백엔드에서 게시글/카테고리 CUD 시 이 엔드포인트를 **호출하는 주체가 없다.**
- 현재 `PostServiceImpl` 은 이미 `PostChangeEvent` 를 publish 하지만 **구독자는 `SseEmitterService` 하나** — 실시간 브로드캐스트만 수행, Next.js 캐시 무효화는 미연동.
- `CategoryService` 는 **이벤트 자체가 없음**.

결과:
- 관리자가 새 글 발행 → Next.js ISR TTL(60s ~ 3600s) 만큼 공개 페이지 / sitemap 이 stale.
- SEO 색인 지연. 프론트 계획서 Top 1 의 "**반쪽 구현**" 을 완결시키는 마지막 퍼즐이 빠진 상태.

### 1-2. 왜 중요한가?

- 프론트 Route Handler(`/api/revalidate`) 는 **계약만 준비된 문**. 호출하지 않으면 쓸모없는 포트.
- ISR TTL 을 완화(60s → 600s) 하여 백엔드 호출을 줄이려면 on-demand 무효화가 선행 조건.
- 검색엔진 크롤러의 sitemap 재방문 주기가 불규칙해도, 사이트가 변경 즉시 sitemap 을 내놓을 수 있어야 함.

### 1-3. 해결하지 않으면?

- 관리자가 "발행" 누른 후 "반영됐나?" 불안감 → 새로고침 반복 / 고객센터 문의.
- 운영 측 ISR TTL 튜닝 자유도 봉쇄.
- Top 1 프론트 작업이 **dead code** 로 남아 회귀 검증도 되지 않음.

---

## 2. Analyze (분석 및 선택지 검토)

### 2-1. HTTP 클라이언트 선택지

| 클라이언트 | 가용 여부 (Spring Boot 3.0.4) | 장점 | 단점 |
| --- | --- | --- | --- |
| `RestTemplate` | ✅ `spring-boot-starter-web` 에 포함 | 이미 의존성 존재, blocking, 단순 | deprecated 예고 (LTS 는 아직 유지) |
| `RestClient` | ❌ Spring 6.1+ 필요 (현재 6.0) | — | 불가 |
| `WebClient` | ⚠️ `spring-boot-starter-webflux` 추가 필요 | 비동기 | 의존성 증가 + 코드 복잡도 상승 |
| `HttpClient` (JDK 11+) | ✅ 내장 | 최소 의존성 | 타임아웃/인터셉터 수동 구현 |

**채택: `RestTemplate`** — 의존성 추가 없이 즉시 사용, webhook 은 단순 POST 1회 호출로 비동기 이점 미미.
- 단점(deprecated 예고)는 `@Async` 로 스레드 분리하면 blocking 도 문제없음.
- 향후 Spring Boot 3.2 업그레이드 시 RestClient 로 이행 고려 (별도 과제).

### 2-2. 실행 모델 (동기 vs 비동기)

| 모델 | 장점 | 단점 |
| --- | --- | --- |
| 동기 listener | 단순, 실패 시 즉시 인지 | **관리자 HTTP 응답이 webhook RTT 만큼 지연** — 최악 3초+ |
| `@Async` listener | 관리자 응답 영향 없음, fire-and-forget | 실패 시 로그로만 확인 |

**채택: `@Async`** — 관리자 UX 가 최우선. 실패해도 TTL 이 결국 캐시를 무효화하므로 치명적이지 않음. `@EnableAsync` 는 이미 `MyblogBootApplication` 에 선언되어 있음.

### 2-3. 이벤트 타이밍

| 타이밍 | 설명 | 채택 여부 |
| --- | --- | --- |
| `BEFORE_COMMIT` | 트랜잭션 커밋 전 | ✗ — 롤백 시 무효화만 발생해 stale 상태 |
| `AFTER_COMMIT` | 트랜잭션 커밋 후 | ✅ — 기존 `SseEmitterService` 와 동일 패턴 |
| `AFTER_COMPLETION` | 커밋/롤백 관계없이 | ✗ — 롤백 후에도 호출됨 |

**채택: `AFTER_COMMIT`**.

### 2-4. 이벤트 구조 변경 범위

현재 `PostChangeEvent(Object source, String changeType, Long postId)` — **`slug` 필드 없음**. 하지만 프론트 태그 `post:{slug}` 는 slug 가 필요.

| 선택지 | 설명 | 선택 |
| --- | --- | --- |
| A. listener 에서 `postRepository.findById(postId)` 로 slug 조회 | 이벤트 불변 | ✗ — DELETE 후 soft-delete 상태 접근, DB 추가 호출, async 스레드에서 JPA 세션 만료 가능 |
| B. `PostChangeEvent` 에 `slug` 필드 추가 | 이벤트 확장, publisher 책임 | ✅ |

**채택 B**. 기존 생성자는 오버로드로 유지해 `SseEmitterService.handlePostChange` (getChangeType 만 사용) 는 회귀 없음.

`changeType` 을 enum 으로 바꾸는 리팩토링은 본 계획 범위 제외 — 기존 필드 호환 우선.

### 2-5. 카테고리 이벤트

`CategoryServiceImpl` 은 현재 **이벤트 없음**. `CategoryChangeEvent` 신규 추가.

- `create` / `update` / `delete` 3곳에 publish.
- 카테고리 이름 변경 시 slug 는 경로 일부가 아니므로(`/category/[name]`) tag `categories` 하나로 충분.

### 2-6. Permanent Delete (스케줄러)

`PostServiceImpl.deletePermanently(LocalDateTime thresholdDate)` — 소프트 삭제된 글을 grace period 후 영구 삭제. **이벤트 미발행**.

- 이미 soft-delete 시점에 `DELETED` 이벤트 → 프론트 캐시 무효화 완료.
- 영구 삭제는 내부 정리 — 프론트는 이미 모르던 글. 추가 무효화 불필요.
- **본 계획 범위 제외**.

### 2-7. 재시도 정책

| 정책 | 선택 |
| --- | --- |
| 재시도 없음 | ✗ — 일시적 네트워크 깜박임에 너무 약함 |
| Spring Retry (`@Retryable`) | ✗ — 의존성 추가, 설정 복잡 |
| 수동 1회 재시도 (try-catch 반복) | ✅ — 간단, 제어 용이 |

**채택**: IOException / 5xx 발생 시 1회 재시도. 그래도 실패하면 `log.warn` 만 남기고 종료 — 캐시는 결국 TTL 이 커버.

### 2-8. 테스트/개발 환경 비활성화

- 테스트 환경에서 실제 프론트가 없으므로 webhook 호출이 실패 → 로그 오염.
- `application-test.yaml` 에서 `revalidate.webhook.enabled: false` 로 차단.
- Listener 는 `enabled` flag 를 참조해 early return (또는 `@ConditionalOnProperty` 로 빈 등록 자체 차단 — 선택지 비교).

**선택**: `@ConditionalOnProperty(prefix = "revalidate.webhook", name = "enabled", havingValue = "true", matchIfMissing = true)` — 빈 등록 자체를 차단해 테스트 시 자동 비활성.

### 2-9. 보안

- `REVALIDATE_SECRET` 환경변수 (프론트와 동일 값).
- 헤더 `x-revalidate-secret` 로 전달.
- URL 쿼리 파라미터 금지 (access log 유출 방지).
- 운영: Docker Compose 내부 네트워크 통신 전제.

---

## 3. Action (구현 계획 및 설계)

### 3-1. 변경 대상 파일

#### 신규 생성

| 파일 | 역할 |
| --- | --- |
| `domain/event/CategoryChangeEvent.java` | 카테고리 CUD 이벤트 |
| `dto/revalidate/RevalidateRequest.java` | webhook 요청 페이로드 DTO |
| `configuration/RevalidateWebhookProperties.java` | `@ConfigurationProperties` (url / secret / timeout / enabled) |
| `configuration/RevalidateWebhookConfig.java` | `RestTemplate` Bean (타임아웃 설정) |
| `service/RevalidateWebhookClient.java` | HTTP 호출 + 재시도 로직 (프로덕션 코드) |
| `service/RevalidateWebhookListener.java` | `@TransactionalEventListener` + `@Async` — 태그/경로 매핑 |
| `test/.../RevalidateWebhookListenerTest.java` | Listener 단위 테스트 (`MockRestServiceServer`) |
| `backend/.env.example` 추가/수정 | `REVALIDATE_SECRET`, `REVALIDATE_WEBHOOK_URL` |

#### 수정

| 파일 | 변경 내용 |
| --- | --- |
| `domain/event/PostChangeEvent.java` | `slug: String` 필드 + 생성자 오버로드 (기존 시그니처 유지) |
| `service/implementation/PostServiceImpl.java` | `publishEvent(...)` 3곳에 slug 전달 (`result.getSlug()` / `post.getSlug()`) |
| `service/implementation/CategoryServiceImpl.java` | `ApplicationEventPublisher` 주입 + 3곳 (create/update/delete) publish |
| `resources/application.yaml` | `revalidate.webhook.*` 설정 블록 |
| `resources/application-test.yaml` | `revalidate.webhook.enabled: false` |

### 3-2. 태그/경로 매핑 (프론트 계획서와 일치)

| 이벤트 | tags | paths |
| --- | --- | --- |
| Post `CREATED` | `["posts", "slugs"]` | `["/sitemap.xml"]` |
| Post `UPDATED` | `["posts", "post:{slug}"]` | `[]` |
| Post `DELETED` | `["posts", "post:{slug}", "slugs"]` | `["/sitemap.xml"]` |
| Category `CREATED` / `UPDATED` / `DELETED` | `["categories"]` | `[]` |

### 3-3. 설정 스펙

#### `application.yaml`

```yaml
# Next.js on-demand revalidation webhook
revalidate:
  webhook:
    enabled: ${REVALIDATE_WEBHOOK_ENABLED:true}
    url: ${REVALIDATE_WEBHOOK_URL:http://localhost:3000/api/revalidate}
    secret: ${REVALIDATE_SECRET:}
    connect-timeout-ms: ${REVALIDATE_CONNECT_TIMEOUT:2000}
    read-timeout-ms: ${REVALIDATE_READ_TIMEOUT:3000}
```

#### `application-test.yaml`

```yaml
revalidate:
  webhook:
    enabled: false
```

### 3-4. DTO 스펙

```java
// dto/revalidate/RevalidateRequest.java
public record RevalidateRequest(List<String> tags, List<String> paths) {
    public static RevalidateRequest of(List<String> tags, List<String> paths) {
        return new RevalidateRequest(
            tags == null ? List.of() : tags,
            paths == null ? List.of() : paths
        );
    }
}
```

### 3-5. 핵심 클래스 스켈레톤

#### `PostChangeEvent` (수정)

```java
public class PostChangeEvent extends ApplicationEvent {
    private final String changeType;
    private final Long postId;
    private final String slug;

    // 신규 생성자
    public PostChangeEvent(Object source, String changeType, Long postId, String slug) {
        super(source);
        this.changeType = changeType;
        this.postId = postId;
        this.slug = slug;
    }

    // 하위 호환 (SseEmitterService 영향 없음)
    public PostChangeEvent(Object source, String changeType, Long postId) {
        this(source, changeType, postId, null);
    }

    public String getChangeType() { return changeType; }
    public Long getPostId() { return postId; }
    public String getSlug() { return slug; }
}
```

#### `CategoryChangeEvent` (신규)

```java
public class CategoryChangeEvent extends ApplicationEvent {
    private final String changeType; // CREATED / UPDATED / DELETED
    private final Long categoryId;

    public CategoryChangeEvent(Object source, String changeType, Long categoryId) {
        super(source);
        this.changeType = changeType;
        this.categoryId = categoryId;
    }
    public String getChangeType() { return changeType; }
    public Long getCategoryId() { return categoryId; }
}
```

#### `RevalidateWebhookProperties` (신규)

```java
@ConfigurationProperties(prefix = "revalidate.webhook")
public record RevalidateWebhookProperties(
    boolean enabled,
    String url,
    String secret,
    int connectTimeoutMs,
    int readTimeoutMs
) {}
```

#### `RevalidateWebhookConfig` (신규)

```java
@Configuration
@EnableConfigurationProperties(RevalidateWebhookProperties.class)
public class RevalidateWebhookConfig {
    @Bean("revalidateRestTemplate")
    public RestTemplate revalidateRestTemplate(RevalidateWebhookProperties props) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeoutMs());
        factory.setReadTimeout(props.readTimeoutMs());
        return new RestTemplate(factory);
    }
}
```

#### `RevalidateWebhookClient` (신규)

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class RevalidateWebhookClient {
    private final RevalidateWebhookProperties props;
    @Qualifier("revalidateRestTemplate")
    private final RestTemplate restTemplate;

    public void send(List<String> tags, List<String> paths) {
        if (!props.enabled()) return;
        if ((tags == null || tags.isEmpty()) && (paths == null || paths.isEmpty())) return;

        var body = RevalidateRequest.of(tags, paths);
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-revalidate-secret", props.secret());
        var entity = new HttpEntity<>(body, headers);

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                restTemplate.postForEntity(props.url(), entity, String.class);
                log.info("[revalidate] ok tags={} paths={}", tags, paths);
                return;
            } catch (RestClientException e) {
                if (attempt == 2) {
                    log.warn("[revalidate] failed tags={} paths={} error={}", tags, paths, e.getMessage());
                    return;
                }
            }
        }
    }
}
```

#### `RevalidateWebhookListener` (신규)

```java
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "revalidate.webhook",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class RevalidateWebhookListener {
    private final RevalidateWebhookClient client;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostChange(PostChangeEvent event) {
        List<String> tags = new ArrayList<>();
        List<String> paths = new ArrayList<>();

        tags.add("posts");
        if (event.getSlug() != null && !event.getSlug().isBlank()) {
            if ("UPDATED".equals(event.getChangeType()) || "DELETED".equals(event.getChangeType())) {
                tags.add("post:" + event.getSlug());
            }
        }
        if ("CREATED".equals(event.getChangeType()) || "DELETED".equals(event.getChangeType())) {
            tags.add("slugs");
            paths.add("/sitemap.xml");
        }
        client.send(tags, paths);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryChange(CategoryChangeEvent event) {
        client.send(List.of("categories"), List.of());
    }
}
```

### 3-6. Publisher 수정

#### `PostServiceImpl` — slug 를 이벤트에 포함

```java
// write()
eventPublisher.publishEvent(new PostChangeEvent(this, "CREATED", result.getId(), result.getSlug()));

// edit()
eventPublisher.publishEvent(new PostChangeEvent(this, "UPDATED", postId, post.getSlug()));

// delete()
eventPublisher.publishEvent(new PostChangeEvent(this, "DELETED", postId, post.getSlug()));
```

#### `CategoryServiceImpl` — ApplicationEventPublisher 주입 + 3곳 publish

```java
private final ApplicationEventPublisher eventPublisher;

// create()
categoryRepository.save(category);
eventPublisher.publishEvent(new CategoryChangeEvent(this, "CREATED", category.getId()));

// update()
category.editCategory(modifiedCategoryName);
eventPublisher.publishEvent(new CategoryChangeEvent(this, "UPDATED", categoryId));

// delete()
categoryRepository.delete(category);
eventPublisher.publishEvent(new CategoryChangeEvent(this, "DELETED", categoryId));
```

### 3-7. 주요 트레이드오프

| 트레이드오프 | 선택 |
| --- | --- |
| RestTemplate vs WebClient | RestTemplate (의존성 無 추가) |
| 동기 listener vs `@Async` | `@Async` (관리자 UX 우선) |
| 이벤트에 slug 포함 vs 리스너에서 DB 재조회 | 이벤트에 포함 (DELETE 이후 soft-delete 접근 회피) |
| `changeType` enum 리팩토링 | 보류 — 기존 String 유지, 별도 과제 |
| 재시도 1회 vs Spring Retry | 수동 1회 (의존성 無 추가) |
| 서킷 브레이커 | 없음 — 저빈도 이벤트(일 수 건) |
| `@ConditionalOnProperty` vs flag check | `@ConditionalOnProperty` — 테스트 자동 비활성 |
| 전용 Executor vs 기본 `SimpleAsyncTaskExecutor` | 기본 사용 — 저빈도. 필요 시 후속 |

### 3-8. 예상 이슈 및 대응

| 이슈 | 대응 |
| --- | --- |
| `@Async` + `@TransactionalEventListener` 순서 | Spring 문서상 공식 지원. 테스트에서 타이밍 검증 필요 |
| 테스트 시 webhook 호출 로그 폭탄 | `application-test.yaml` 에서 `enabled: false` + `@ConditionalOnProperty` |
| secret 미설정(비어있는 값) | listener 가 호출해도 401 반환 — 로그만 남고 무해 |
| Docker Compose 내부 URL 해석 실패 | 환경변수로 URL 주입, 로컬은 `localhost:3000`, 프로덕션은 `frontend:3000` |
| 기존 `PostServiceTest` 회귀 | slug 를 포함하는 새 생성자 사용. 기존 테스트가 event 검증 안 하면 영향 없음 |
| `RestClientException` 외 예외 | catch `Exception` 은 피하고 `RestClientException` 한정 — 프로그래밍 실수는 빠르게 드러나야 함 |
| `SseEmitterService.handlePostChange` 영향 | 기존 생성자(3-arg) 그대로 호출 가능 — `PostServiceImpl` 만 4-arg 로 전환 |
| `CategoryService` 이벤트 미수신자 | `SseEmitter` 에도 추후 연동 가능. 본 계획 범위 제외 |

### 3-9. `.env.example` 업데이트

```
# Next.js on-demand revalidation
REVALIDATE_WEBHOOK_ENABLED=true
REVALIDATE_WEBHOOK_URL=http://localhost:3000/api/revalidate
REVALIDATE_SECRET=dev-revalidate-secret
# REVALIDATE_CONNECT_TIMEOUT=2000
# REVALIDATE_READ_TIMEOUT=3000
```

> 프로덕션 Docker Compose 에서는 `REVALIDATE_WEBHOOK_URL=http://frontend:3000/api/revalidate` 로 재정의.

---

## 4. Result (검증 계획)

### 4-1. 단위 테스트

`RevalidateWebhookListenerTest` — `MockRestServiceServer` 사용:

| 시나리오 | 검증 |
| --- | --- |
| `PostChangeEvent("CREATED", id=1, slug="a")` | POST 호출, body `{tags: [posts, slugs], paths: [/sitemap.xml]}`, `x-revalidate-secret` 헤더 |
| `PostChangeEvent("UPDATED", id=1, slug="a")` | body `{tags: [posts, post:a], paths: []}` |
| `PostChangeEvent("DELETED", id=1, slug="a")` | body `{tags: [posts, post:a, slugs], paths: [/sitemap.xml]}` |
| `CategoryChangeEvent("CREATED", 5)` | body `{tags: [categories], paths: []}` |
| Webhook 5xx 응답 | 1회 재시도 후 `log.warn`, 예외 전파 없음 |
| `enabled: false` | RestTemplate 호출 0회 |

### 4-2. 통합 테스트 (선택)

`@SpringBootTest` + `MockRestServiceServer`:

| 시나리오 | 검증 |
| --- | --- |
| `PostService.write(...)` 호출 | 트랜잭션 커밋 후 webhook endpoint 호출 확인 |
| `CategoryService.create("java")` 호출 | 동일 |
| 트랜잭션 롤백 시나리오 | webhook 호출되지 않음 (AFTER_COMMIT 보장) |

### 4-3. 수동 E2E (로컬)

1. 로컬에서 `frontend` (`npm run dev`) + `backend` (`./gradlew bootRun`) 동시 기동.
2. `.env.local` 과 `backend/.env` 의 `REVALIDATE_SECRET` 동일 값 설정.
3. 관리자 로그인 → 새 글 발행.
4. **기대**:
   - 프론트 Route Handler 로그 `[revalidate] tags=["posts","slugs"] paths=["/sitemap.xml"]` 확인.
   - 백엔드 로그 `[revalidate] ok tags=[posts, slugs] paths=[/sitemap.xml]` 확인.
   - 다른 브라우저 시크릿 탭에서 홈(`/`) 재접속 → 60초 지나지 않아도 신규 글 노출.
   - `/sitemap.xml` 즉시 슬러그 반영.
5. secret 불일치 테스트: 백엔드 secret 변경 후 재발행 → 프론트 로그 401, 백엔드 `[revalidate] failed`.

### 4-4. 관련 테스트 실행 범위 (CLAUDE.md 규칙)

- `./gradlew test --tests "com.moya.myblogboot.service.RevalidateWebhookListenerTest"`
- `./gradlew test --tests "com.moya.myblogboot.service.PostServiceTest"` (기존 회귀)
- `./gradlew test --tests "com.moya.myblogboot.service.CategoryServiceTest"` (기존 회귀)
- 전체 `./gradlew test` 는 실행하지 않음.

### 4-5. 성공 기준

- [ ] 단위 테스트 4종 (Post 3 + Category 1) + 에러 케이스 2종 통과
- [ ] 기존 `PostServiceTest` / `CategoryServiceTest` 회귀 없음
- [ ] 수동 E2E — 글 발행 시 프론트/백엔드 양측 로그 확인
- [ ] `application-test.yaml` 에서 webhook 비활성 확인 (전체 테스트 기동 시 로그 오염 無)

### 4-6. 모니터링 (운영)

- `log.info("[revalidate] ok ...")` — 성공 호출 빈도 추적.
- `log.warn("[revalidate] failed ...")` — 실패율 알람 기준. 임계치(예: 분당 3건) 초과 시 담당자 확인.
- 장기: 프론트 Route Handler 도 구조화 로그로 전환 → 양측 매칭 분석 가능.

---

## 5. 구현 순서 (권장)

1. **Step 1 — 이벤트 확장**
   1. `PostChangeEvent` 에 `slug` 필드 추가 (하위호환 생성자 유지).
   2. `CategoryChangeEvent` 신규 생성.
2. **Step 2 — Publisher 수정**
   1. `PostServiceImpl` 3곳 — slug 전달 형태로 변경.
   2. `CategoryServiceImpl` — ApplicationEventPublisher 주입 + 3곳 publish.
3. **Step 3 — 인프라 (설정 / HTTP / DTO)**
   1. `RevalidateWebhookProperties` + `RevalidateWebhookConfig`.
   2. `RevalidateRequest` DTO.
   3. `application.yaml` + `application-test.yaml` 설정.
4. **Step 4 — Client / Listener**
   1. `RevalidateWebhookClient`.
   2. `RevalidateWebhookListener`.
5. **Step 5 — 테스트**
   1. `RevalidateWebhookListenerTest` (MockRestServiceServer).
   2. 기존 `PostServiceTest`, `CategoryServiceTest` 회귀.
6. **Step 6 — 수동 검증 + 커밋**
   1. 로컬 E2E.
   2. 재검토 체크리스트.
   3. 사용자 커밋 확인 후 진행.

---

## 6. Context7 활용 포인트 (구현 시)

- **Spring Boot 3.0.x** — `@ConfigurationProperties` + record 지원, `@TransactionalEventListener` + `@Async` 공식 예제.
- **Spring Framework 6.0** — `RestTemplate` 상태 (deprecated 계획 / 대체 경로).
- **Spring Test** — `MockRestServiceServer` 사용법.

---

## 7. 재검토 체크리스트 (완료 시)

- [ ] `PostChangeEvent` 생성자 오버로드 — 기존 `SseEmitterService` 영향 없음
- [ ] `PostServiceImpl` 3곳 모두 slug 전달
- [ ] `CategoryChangeEvent` 신규 + `CategoryServiceImpl` 3곳 publish
- [ ] Webhook `enabled=false` 시 외부 호출 0건 (테스트에서 확인)
- [ ] secret 이 access log / 에러 로그에 노출되지 않음
- [ ] `application.yaml` 만 수정하면 URL / secret 운영 분리 가능
- [ ] 단위 테스트 4+2종 통과 + 기존 테스트 회귀 없음
- [ ] 수동 E2E 검증 (프론트 + 백엔드 로그 페어링)
- [ ] `.env.example` 업데이트 — `REVALIDATE_SECRET`, `REVALIDATE_WEBHOOK_URL` 포함
- [ ] 불필요 디버그 로그 제거
- [ ] 커밋 메시지 초안 사용자 확인

---

## 8. 범위 제외 / 후속 과제

| 항목 | 이월 사유 |
| --- | --- |
| `changeType` String → enum 리팩토링 | 기존 구조 안전성 우선. 별도 리팩토링 회차 |
| `deletePermanently` (스케줄러) 이벤트 | soft-delete 시점에 이미 무효화 완료 |
| Spring Retry / Resilience4j 서킷 브레이커 | 현 빈도 저(低) — YAGNI |
| RestTemplate → RestClient 이행 | Spring Boot 3.2+ 업그레이드 시 함께 |
| ISR `revalidate` TTL 완화 (60s → 600s) | 본 작업 완료 후 운영 관측 기반 튜닝 |
| 프론트 Route Handler 응답 구조화 로그 | 관측성 회차 |
| Webhook 실패 시 DLQ / alerting | 운영 안정화 후 |
