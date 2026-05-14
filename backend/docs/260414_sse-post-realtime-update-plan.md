# SSE 기반 게시글 실시간 업데이트 계획서

---

## 1. Problem (문제 정의)

### 어떤 문제를 해결하려는가?

관리자가 게시글을 작성/수정/삭제하면, 이미 페이지를 열어 둔 사용자들은 **수동 새로고침** 없이는 변경된 목록을 볼 수 없다.

현재 `queryClient.invalidateQueries()`는 호출한 브라우저 인스턴스의 캐시만 무효화하므로, 다른 사용자의 TanStack Query 캐시에는 영향이 없다.

### 이 문제가 왜 중요한가?

- 사용자가 이미 삭제되거나 변경된 게시글에 접근하면 404 에러 또는 오래된 정보를 보게 됨
- 새 글을 발행해도 방문 중인 사용자에게 즉시 노출되지 않아 콘텐츠 도달 시간이 지연됨
- 블로그의 실시간성이 떨어져 사용자 경험이 저하됨

### 해결하지 않을 경우 어떤 문제가 발생하는가?

- 사용자는 `staleTime` 만료 + 트리거(포커스, 라우트 변경)가 발생할 때까지 구 데이터를 계속 표시
- 삭제된 게시글 클릭 시 에러 발생
- 수정된 게시글의 이전 버전이 캐시에서 노출

---

## 2. Analyze (분석 및 선택지 검토)

### 현재 구조 분석

```
[Admin 브라우저]                         [Backend]                      [User 브라우저]
 PostEditorClient.tsx                   PostController                  PostListInfinite.tsx
      |                                     |                               |
      | POST /api/v1/posts                  |                               |
      |------------------------------------>|                               |
      |                                     | PostService.write()           |
      |                                     |  -> DB 저장                   |
      |           200 OK (postId)           |                               |
      |<------------------------------------|                               |
      |                                     |                               |
      | invalidateQueries("posts")          |                               |
      | <- 본인 브라우저만 갱신됨 ----------X|                               |
      |                                     |               캐시 만료까지    |
      |                                     |               stale 데이터 -->|
```

**핵심 단절**: 백엔드는 글 저장 후 아무에게도 알리지 않으므로, 다른 클라이언트는 `staleTime` 만료 + 트리거 전까지 구 데이터를 표시한다.

### 가능한 구현 방법

#### 선택지 1: Polling (주기적 API 호출)

- **장점**: 구현 가장 단순, 별도 프로토콜 불필요
- **단점**: 불필요한 트래픽 발생, 실시간성 낮음 (폴링 주기만큼 딜레이), 서버 부하 증가

#### 선택지 2: WebSocket (양방향 통신)

- **장점**: 양방향 통신 가능, 낮은 지연
- **단점**: 별도 핸드셰이크 + `ws://` 프로토콜, 자동 재접속 직접 구현 필요, 인프라(프록시) 설정 복잡, 양방향이 불필요한 시나리오에 과잉 설계

#### 선택지 3: Server-Sent Events (SSE)

- **장점**: 단방향(서버 -> 클라이언트) 단순 구조, HTTP/1.1 기존 인프라 그대로 사용, `EventSource` API 자동 재접속 내장, 구현 복잡도 낮음
- **단점**: 단방향만 가능 (이 시나리오에서는 문제 없음), IE 미지원 (모던 브라우저 모두 지원)

### 최종 선택: SSE

| 항목 | SSE | WebSocket |
|------|-----|-----------|
| 방향 | 단방향 (서버 -> 클라이언트) | 양방향 |
| 프로토콜 | HTTP/1.1 (기존 인프라 그대로) | 별도 핸드셰이크 + ws:// |
| 자동 재접속 | `EventSource` API 기본 내장 | 직접 구현 필요 |
| 복잡도 | 낮음 | 높음 |

**선택 이유**: "새 글이 올라왔다"는 **서버 -> 클라이언트 단방향 알림**이므로 SSE가 가장 적합하다. Polling은 불필요한 트래픽을 유발하고, WebSocket은 양방향이 불필요한 시나리오에 과잉 설계다.

---

## 3. Action (구현 계획 및 설계)

### 작업 목표 및 범위

- 백엔드: 게시글 변경 시 Spring ApplicationEvent 발행 -> SSE로 연결된 모든 클라이언트에 push
- 프론트엔드: SSE 구독 -> 이벤트 수신 시 `invalidateQueries()` 호출로 자동 갱신

### 시퀀스 다이어그램

```
[User 브라우저 A]         [Backend]            [Admin 브라우저]
      |                      |                       |
      | GET /api/v1/sse/posts|                       |
      |--------------------->|  SseEmitterService    |
      |  (EventSource 구독)  |    .subscribe()       |
      |                      |    emitters에 등록     |
      |                      |                       |
      |                      |    POST /api/v1/posts  |
      |                      |<----------------------|
      |                      |                       |
      |                      | PostService.write()   |
      |                      |  -> DB 저장           |
      |                      |  -> ApplicationEvent  |
      |                      |    발행               |
      |                      |                       |
      |   event: POST_CHANGED| @EventListener        |
      |<---------------------|  SseEmitterService    |
      |                      |    .broadcast()       |
      |                      |                       |
      | invalidateQueries()  |                       |
      |  -> 자동 refetch     |                       |
```

### 전체 컴포넌트 구조

```
Backend:
+-----------------------------------------------------+
| PostService                                         |
|   write() / edit() / delete()                       |
|     +-- ApplicationEventPublisher.publishEvent()    |
|           +-- PostChangeEvent(type, postId)         |
+-----------------------------------------------------+
        | Spring ApplicationEvent
        v
+-----------------------------------------------------+
| SseEmitterService                                   |
|   @TransactionalEventListener(PostChangeEvent)      |
|     +-- broadcast(event) -> 모든 SseEmitter에 전송  |
|                                                     |
|   subscribe() -> SseEmitter 생성, emitters에 추가    |
|   heartbeat() -> @Scheduled 30초마다 ":" 전송        |
|   cleanup()  -> 완료/에러/타임아웃 emitter 제거      |
+-----------------------------------------------------+
        | SseEmitter
        v
+-----------------------------------------------------+
| SseController                                       |
|   GET /api/v1/sse/posts -> sseEmitterService        |
|                            .subscribe()             |
+-----------------------------------------------------+

Frontend:
+-----------------------------------------------------+
| usePostEventSource (custom hook)                    |
|   new EventSource("/api/v1/sse/posts")              |
|   onmessage: "POST_CHANGED"                        |
|     -> queryClient.invalidateQueries(["posts"])     |
|     -> queryClient.invalidateQueries(["categories"])|
|   onerror: 자동 재접속 (EventSource 내장)            |
+-----------------------------------------------------+
        | 적용 위치
        v
+-----------------------------------------------------+
| (public)/layout.tsx                                 |
|   <PostEventListener /> <- 전체 public 페이지에 적용 |
+-----------------------------------------------------+
```

### 구현 접근 방식 및 설계 결정

#### 백엔드

**3-1. PostChangeEvent (도메인 이벤트)**

```java
// domain/event/PostChangeEvent.java
public class PostChangeEvent extends ApplicationEvent {
    private final String changeType;   // "CREATED" | "UPDATED" | "DELETED"
    private final Long postId;
    // constructor, getters
}
```

- `ApplicationEvent`를 상속하여 Spring의 `ApplicationEventPublisher`로 발행
- 이벤트 클래스는 `domain/event/` 패키지에 배치

**3-2. SseEmitterService (구독 / 브로드캐스트 / 하트비트)**

```java
// service/SseEmitterService.java
@Service
public class SseEmitterService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private static final long EMITTER_TIMEOUT = 30 * 60 * 1000L; // 30분

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);
        emitter.send(SseEmitter.event()
                .name("CONNECTED")
                .data("connected"));
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(()   -> emitters.remove(emitter));
        emitter.onError(e      -> emitters.remove(emitter));
        return emitter;
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handlePostChange(PostChangeEvent event) {
        String data = event.getChangeType();
        broadcast("POST_CHANGED", data);
    }

    @Scheduled(fixedRate = 30_000)
    public void heartbeat() {
        broadcast("HEARTBEAT", "ping");
    }

    private void broadcast(String eventName, String data) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }
}
```

**3-3. SseController**

```java
// controller/SseController.java
@RestController
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterService sseEmitterService;

    @GetMapping(value = "/api/v1/sse/posts", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        return sseEmitterService.subscribe();
    }
}
```

**3-4. PostServiceImpl 변경 -- 이벤트 발행**

```java
// 변경사항만 표시
private final ApplicationEventPublisher eventPublisher; // 필드 추가

public Long write(...) {
    // ... 기존 로직 동일 ...
    Post result = postRepository.save(newPost);
    category.addPost(result);
    eventPublisher.publishEvent(new PostChangeEvent(this, "CREATED", result.getId()));
    return result.getId();
}

public Long edit(...) {
    // ... 기존 로직 동일 ...
    eventPublisher.publishEvent(new PostChangeEvent(this, "UPDATED", postId));
    return postId;
}

public void delete(...) {
    // ... 기존 로직 동일 ...
    eventPublisher.publishEvent(new PostChangeEvent(this, "DELETED", postId));
}
```

**3-5. 보안 설정 변경**

- `ShouldNotFilterPath`: `/api/v1/sse` 추가 (JWT 필터 스킵 -- 익명 사용자도 구독 가능)
- `WebSecurityConfig`: 별도 변경 불필요 (현재 `.anyRequest().permitAll()`이므로 GET 허용됨)

#### 프론트엔드

**3-6. usePostEventSource 훅**

```typescript
// hooks/usePostEventSource.ts
"use client";

import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/queryKeys";

const SSE_URL = (process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080")
  + "/api/v1/sse/posts";

export function usePostEventSource() {
  const queryClient = useQueryClient();

  useEffect(() => {
    const es = new EventSource(SSE_URL, { withCredentials: true });

    es.addEventListener("POST_CHANGED", () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.categories.all() });
    });

    es.addEventListener("CONNECTED", () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.categories.all() });
    });

    return () => es.close();
  }, [queryClient]);
}
```

- `queryKeys.posts.all()` = `["posts"]` -> `posts` 하위 모든 쿼리가 무효화됨
- 카테고리 목록도 `boardsCount`가 변경되므로 함께 무효화
- `CONNECTED` 이벤트 수신 시에도 invalidate -> 재접속 시 놓친 변경 보완

**3-7. PostEventListener 컴포넌트 & 적용 위치**

```typescript
// components/layout/PostEventListener.tsx
"use client";

import { usePostEventSource } from "@/hooks/usePostEventSource";

export function PostEventListener() {
  usePostEventSource();
  return null;
}
```

```tsx
// app/(public)/layout.tsx -- 변경
import { PostEventListener } from "@/components/layout/PostEventListener";

export default function PublicLayout({ children }) {
  return (
    <div>
      <PostEventListener />
      <Header />
      <main>...</main>
    </div>
  );
}
```

- `(public)/layout.tsx`에만 적용: 일반 사용자 영역에서만 SSE 구독
- 관리자 페이지(`/management`)에서는 구독하지 않음 (admin은 자신이 작업한 결과를 이미 `invalidateQueries`로 갱신)

### 변경 대상 파일 목록

#### 백엔드 (신규)

| 파일 | 설명 |
|------|------|
| `domain/event/PostChangeEvent.java` | Spring ApplicationEvent -- 변경 유형 + postId |
| `service/SseEmitterService.java` | SSE 구독 관리, 브로드캐스트, 하트비트 |
| `controller/SseController.java` | `GET /api/v1/sse/posts` 엔드포인트 |

#### 백엔드 (수정)

| 파일 | 변경 내용 |
|------|-----------|
| `service/implementation/PostServiceImpl.java` | `ApplicationEventPublisher` 주입 + write/edit/delete에 이벤트 발행 추가 |
| `constants/ShouldNotFilterPath.java` | `/api/v1/sse` 추가 |

#### 프론트엔드 (신규)

| 파일 | 설명 |
|------|------|
| `hooks/usePostEventSource.ts` | EventSource 구독 + queryClient.invalidateQueries |
| `components/layout/PostEventListener.tsx` | 렌더리스 SSE 구독 컴포넌트 |

#### 프론트엔드 (수정)

| 파일 | 변경 내용 |
|------|-----------|
| `app/(public)/layout.tsx` | `<PostEventListener />` 추가 |

### 주요 트레이드오프

| 항목 | 결정 | 이유 |
|------|------|------|
| `CopyOnWriteArrayList` | 채택 | 읽기(브로드캐스트)가 쓰기(subscribe/remove)보다 압도적으로 많음. 개인 블로그 규모에서 충분 |
| `EMITTER_TIMEOUT = 30분` | 채택 | 브라우저 `EventSource`는 timeout 후 자동 재접속하므로 무한보다 유한 타임아웃이 자원 관리에 유리 |
| `heartbeat 30초` | 채택 | Nginx/CloudFlare 등 리버스 프록시의 기본 idle timeout이 60~120초이므로 30초 간격이면 안전 |
| `@TransactionalEventListener` | 채택 | 트랜잭션 커밋 후에만 이벤트 발행 -- 롤백 시 잘못된 알림 방지 |

### 예상되는 이슈 및 대응 방안

| 이슈 | 대응 |
|------|------|
| SSE가 리버스 프록시(Nginx)에서 버퍼링됨 | `X-Accel-Buffering: no` + `Cache-Control: no-cache` 헤더 설정 |
| `@Transactional` 내에서 이벤트 발행 -> 롤백 시 잘못된 알림 | `@TransactionalEventListener(phase = AFTER_COMMIT)` 사용 |
| 동시 접속자가 많을 때 `CopyOnWriteArrayList` 성능 | 개인 블로그 규모(동시 접속 ~50명 이하)에서는 문제 없음. 대규모 시 `ConcurrentHashMap` + 별도 스레드풀 고려 |
| 브라우저 탭이 백그라운드일 때 `EventSource` 동작 | 모던 브라우저는 백그라운드에서도 연결 유지. 일부 모바일에서 끊길 수 있음 -> `EventSource` 자동 재접속으로 대응 |
| SseEmitter timeout 후 재접속 시 누락된 이벤트 | CONNECTED 이벤트 수신 시 `invalidateQueries` 호출하여 최신 데이터 확보 |
| CORS: `withCredentials: true` + `EventSource` | 현재 CORS 설정(`allowCredentials: true`)과 호환 |
| Vite dev proxy에서 SSE 동작 | `/api` prefix가 이미 프록시 설정됨. 문제 발생 시 Vite proxy SSE 버퍼링 해제 설정 필요 |

---

## 4. Result (검증 계획)

### 수동 테스트 시나리오

1. 브라우저 A: 블로그 메인 페이지 열기 -> 게시글 목록 확인
2. 브라우저 B (또는 시크릿 창): 관리자 로그인 -> 새 게시글 작성
3. 브라우저 A: **새로고침 없이** 게시글 목록에 새 글이 자동 노출되는지 확인
4. 같은 방식으로 수정/삭제도 검증

### 자동 테스트

| 테스트 대상 | 검증 내용 |
|------------|-----------|
| `SseEmitterService` 단위 테스트 | subscribe -> broadcast -> emitter.send 호출 검증 |
| `SseController` 통합 테스트 | MockMvc로 `GET /api/v1/sse/posts` -> `text/event-stream` 응답 확인 |
| `PostServiceImpl` | 이벤트 발행 검증 (`@Captor ApplicationEvent`) |

### 성공 기준

- [ ] SSE 연결이 정상적으로 수립되고, `CONNECTED` 이벤트가 수신됨
- [ ] 게시글 작성/수정/삭제 시 다른 브라우저에서 **새로고침 없이** 목록이 자동 갱신됨
- [ ] 30초 하트비트가 정상 전송되어 프록시 idle timeout에 걸리지 않음
- [ ] 네트워크 끊김 후 `EventSource` 자동 재접속이 동작하고, 재접속 시 최신 데이터를 fetch함
- [ ] 관련 단위/통합 테스트 전부 통과
