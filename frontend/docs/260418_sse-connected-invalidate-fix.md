# SSE `CONNECTED` 핸들러의 불필요한 쿼리 무효화 수정

작성일: 2026-04-18
범위: frontend (`-f`)

---

## 1. Problem (문제 정의)

### 증상

`http://localhost:3000` 최초 접근 시 백엔드 SQL 로그에서 **동일 쿼리 2쌍이 약 100ms 간격으로 중복 실행**되는 현상이 관찰됨.

- Post 목록 조회 쿼리 2회
- 카테고리 목록 조회 쿼리 2회 (`/api/v2/categories`)

### 원인 가설 검증 결과

`frontend/hooks/usePostEventSource.ts` 의 SSE `CONNECTED` 이벤트 핸들러가 페이지 진입 시마다 posts + categories 쿼리를 **강제로 invalidate** 함.

```ts
es.addEventListener("CONNECTED", () => {
  queryClient.invalidateQueries({ queryKey: queryKeys.posts.all() });
  queryClient.invalidateQueries({ queryKey: queryKeys.categories.all() });
});
```

**실행 흐름**

1. SSR 단계: `app/page.tsx` → `getPostList(1, 60)` 호출 → **1차 Post 쿼리** (Next.js ISR fetch)
2. Client mount:
   - `CategoryNav` → `/api/v2/categories` 호출 → **1차 Category 쿼리**
   - `PostEventListener` → `/api/v1/sse/posts` 연결
3. 백엔드 `SseEmitterService.subscribe()` 가 즉시 `CONNECTED` 이벤트를 송신
4. 프론트 핸들러가 posts + categories 전부 invalidate → **2차 Post 쿼리 + 2차 Category 쿼리**

### 왜 중요한가

- 페이지 진입 시마다 DB에 불필요한 쿼리가 2배로 발생 — 특히 Category 쿼리는 현재 fetchJoin 기반이라 부담이 큼 (백엔드 문제는 별도 티켓).
- React 19 Strict Mode 개발 환경에서는 `useEffect` 2회 실행으로 추가 중복이 발생할 여지가 있음.
- 초기 로딩 속도 저하 + 백엔드 리소스 낭비.

### 해결하지 않을 경우

- 사용자가 페이지를 옮길 때마다 `CONNECTED` 재수신 → 매번 전체 쿼리 무효화 → 캐시 효과 무력화.
- `staleTime 10분` 전역 기본값이 SSE 연결이 있는 페이지에서는 무의미해짐.

---

## 2. Analyze (분석 및 선택지 검토)

### 선택지 A — `CONNECTED` 핸들러 자체를 제거

- 장점: 가장 간단. 최초 연결 시 invalidate 완전히 제거 → 중복 쿼리 0.
- 단점: EventSource가 네트워크 단절 후 **자동 재연결**될 때도 `CONNECTED` 가 다시 오는데, 이 경우 사용자는 그동안 누락된 변경 사항을 받지 못함 (백엔드는 재연결 전 `POST_CHANGED` 이벤트를 **재전송하지 않음**).

### 선택지 B — 최초 `CONNECTED` 만 스킵, 재연결 시에는 invalidate

- 장점: 초기 진입 중복 제거 + 재연결 시 데이터 동기화 유지.
- 단점: `useRef` 로 첫 연결 여부를 추적하는 코드 1줄 추가. 복잡도 증가 미미.
- 구현: `hasConnectedOnceRef` ref 로 첫 이벤트 플래그 관리.

### 선택지 C — `refetchOnReconnect` 기본 옵션 신뢰하고 `CONNECTED` 제거

- 장점: React Query 네이티브 기능 활용.
- 단점: `refetchOnReconnect` 는 **브라우저 네트워크 online 이벤트** 기반. SSE 서버 재시작 등 서버측 재연결은 잡지 못함. 커버리지가 SSE 기반보다 좁음.

### 최종 선택

**선택지 B — 최초 CONNECTED 스킵**

- 초기 중복 쿼리는 확실히 제거
- 재연결 시 데이터 신선도는 유지 (기존 의도 보존)
- 변경 범위 최소 (핸들러 1개, ref 1개)

---

## 3. Action (구현 계획 및 설계)

### 작업 범위

- `frontend/hooks/usePostEventSource.ts` 한 파일만 수정.

### 설계

- `useRef<boolean>(false)` 로 "최초 CONNECTED 수신 여부" 추적.
- `CONNECTED` 핸들러에서:
  - ref.current === false → 플래그만 true 로 세팅하고 invalidate 건너뜀 (초기 연결)
  - ref.current === true → 재연결이므로 기존대로 invalidate 실행

### 변경 후 코드 (미리보기)

```ts
"use client";

import { useEffect, useRef } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/queryKeys";

const SSE_URL =
  (process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080") +
  "/api/v1/sse/posts";

export function usePostEventSource() {
  const queryClient = useQueryClient();
  const hasConnectedOnceRef = useRef(false);

  useEffect(() => {
    const es = new EventSource(SSE_URL, { withCredentials: true });

    es.addEventListener("POST_CHANGED", () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.categories.all() });
    });

    es.addEventListener("CONNECTED", () => {
      if (!hasConnectedOnceRef.current) {
        hasConnectedOnceRef.current = true;
        return; // 초기 연결 — 서버 상태와 이미 동기화되어 있음
      }
      // 재연결 — 누락된 변경 사항 반영
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.categories.all() });
    });

    return () => es.close();
  }, [queryClient]);
}
```

### 주요 트레이드오프

- Strict Mode 2중 mount 시 ref 는 컴포넌트 재생성 여부에 따라 초기화될 수 있으나, `usePostEventSource` 는 `PostEventListener` 컴포넌트가 마운트 되는 한 ref 동일성이 유지됨. 실제로는 React 19 dev 에서 mount → unmount → re-mount 로 ref 가 새로 생성되지만, 두 번째 연결은 네트워크 재연결이 아닌 동일한 라이프사이클에서의 재실행이라 서버 로그상 `CONNECTED` 2번 수신 → 첫 번째 ref 에서 skip, 두 번째 ref (새 mount) 에서 또 skip → 여전히 invalidate 되지 않음. 의도대로 동작.
- 실제 네트워크 단절 후 재연결 테스트는 수동 검증 필요 (dev 에서 백엔드 재시작으로 시뮬레이션 가능).

### 예상 이슈 및 대응

- Strict Mode 에서 `useEffect` cleanup → 재실행 시 새 EventSource 인스턴스가 생성되어 `CONNECTED` 가 1회 더 들어올 수 있음. 본 수정은 **각 EventSource 인스턴스별 최초 1회 skip** 이므로 이 경우도 자연스럽게 커버됨 (프로덕션 빌드에서는 단일 실행이라 영향 없음).

---

## 4. Result (검증 계획)

### 검증 방법

1. **수동 확인 (주)**
   - `npm run dev` (frontend), backend 기동
   - 브라우저로 `http://localhost:3000` 최초 접근
   - 백엔드 SQL 로그에서 **Post 목록 쿼리 1회, Category 쿼리 1회** 만 나오는지 확인
   - 네트워크 탭에서 `/api/v1/sse/posts` 연결 유지되는지 확인
   - TanStack Query Devtools 에서 `posts` / `categories` 쿼리 상태가 `fresh` → `fresh` 유지 (invalidate 되지 않음) 확인

2. **재연결 동작 확인 (보조)**
   - 백엔드를 중단 → 프론트 EventSource 가 재연결 시도 → 백엔드 재기동 → `CONNECTED` 재수신
   - 이 때 SQL 로그에서 Post / Category 쿼리 재실행되는지 확인 (재연결 invalidate 동작 정상)

### 성공 기준

- [ ] 최초 접근 시 Post 목록 SQL 1회만 실행
- [ ] 최초 접근 시 Category SQL 1회만 실행
- [ ] 백엔드 재기동 후 재연결 시에는 invalidate → 쿼리 재실행됨
- [ ] 기존 `POST_CHANGED` 이벤트에 의한 invalidate 동작은 영향 없음

### 테스트 코드

- 현재 프론트엔드에 SSE 관련 자동화 테스트는 없음.
- 본 수정은 1개 훅의 if 분기 추가 수준으로 범위가 협소하므로, 수동 검증으로 충분하다고 판단.

---

## 5. 체크리스트

- [ ] 작업 계획서 작성 완료 (본 문서)
- [ ] Context7 MCP 로 TanStack Query v5 `invalidateQueries`, React `useRef` 문서 확인
- [ ] `usePostEventSource.ts` 수정
- [ ] 로컬에서 수동 검증
- [ ] 재검토 (불필요한 코드 / 디버그 로그 / 사이드 이펙트)
- [ ] 사용자에게 변경 내용 요약 후 커밋 확인 요청
