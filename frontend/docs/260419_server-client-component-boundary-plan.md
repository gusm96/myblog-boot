# Server / Client Component 경계 재정비 계획서

- **작성일**: 2026-04-19
- **작업 범위**: `frontend/` (Next.js 16.2.3 App Router, React 19.2.4)
- **근거 리뷰**: `.claude/reviews/frontend-review-1-app-router.md` § **1-2 Server / Client Component 분리**
- **선행 작업**: 본 문서 이전 커밋 — `260419_public-route-group-consolidation-plan.md` (1-1 완료, `(admin)/management/` 그룹 신설됨)
- **범위 외 (후속 회차)**: on-demand revalidation (2-3), 특수 파일 `error.tsx`/`loading.tsx` (1-3), `useSuspenseQuery` 도입 (2회차 4-5)

---

## 1. Problem (문제 정의)

### 1.1 현상

**CC (Client Component) 비중 과다**: `.tsx` 33개 중 26개(~79%)가 `"use client"` 선언. 아래 3건은 실제 인터랙션이 없거나 과하게 CC 로 밀려 있음.

1. **`app/(admin)/management/layout.tsx` 전역 클라이언트화** (파일 상단 `"use client"`):
   - 관리자 서브트리 **전체**가 Client Component. `Header + Container + Row + AdminNavBar` 껍데기까지 모두 CSR 번들에 포함.
   - 인증 로직이 **클라이언트 `useEffect` 기반 `router.replace("/login?from=/management")`**. 따라서:
     - 보호된 관리자 레이아웃 HTML이 **먼저** 브라우저로 내려감.
     - JS 하이드레이션 완료 후 `getRoleFromToken()` 호출 → `ROLE_ADMIN` 아니면 리다이렉트.
     - 그 찰나에 사용자는 **관리자 레이아웃 Flash**를 본다.
   - 보안 경계도 아님: 실제 보호는 `proxy.ts` (HTTPS 환경 `refresh_token` 쿠키 검사) + 백엔드 JWT 필터가 담당.

2. **`components/comments/CommentSection.tsx` 불필요한 CC**:
   ```tsx
   "use client";
   export function CommentSection({ postId }: CommentSectionProps) {
     return (<><CommentForm postId={postId} /><CommentList postId={postId} /></>);
   }
   ```
   자체적으로는 훅/이벤트/상태 사용 0건. 단순 래퍼인데 `"use client"` 선언으로 불필요한 CSR 경계 생성. 상위 `posts/[slug]/page.tsx` 는 SC 이므로 **경계를 리프 두 개(`CommentForm`/`CommentList`) 에 두면 충분**.

3. **Tiptap 에디터 dynamic loader 코드 복제**:
   `app/(admin)/management/new-post/page.tsx`, `app/(admin)/management/posts/[id]/page.tsx` 두 파일이 거의 동일한 `dynamic(() => import(...), { ssr: false, loading: ... })` 블록을 복제. 페이지 자체는 로직 없는 얇은 래퍼(파라미터 추출 + 단일 컴포넌트 렌더) 임에도 `"use client"` 로 선언.

### 1.2 왜 중요한가

- **FCP/TTI 지표의 직접 저하**: 관리자 껍데기가 Server 에서 바로 렌더되면 HTML 만으로 레이아웃이 시각적으로 완성. 현재는 JS 로드/하이드 전 "빈 레이아웃" 또는 "Spinner" 상태가 보장됨.
- **보안 상의 UX 오해**: 비관리자가 `/management` 에 접근할 때 "잠깐 관리자 메뉴가 보이다 튕기는" 경험은 노출 우려를 유발(실제 데이터는 404/401 로 보호되지만, 시각적으로는 "들어와진 것처럼" 보임).
- **App Router 베스트 프랙티스 이탈**: React 19 + Next 16 의 SC 기본 원칙("상호작용이 있는 리프만 CC") 위반. 면접에서 "CC 비중이 왜 79% 인가" 질문을 받았을 때 방어 근거가 약해진다.
- **코드 중복**: Editor dynamic loader 가 새 에디터(예: 임시저장 내 수정용) 추가 시 3 곳 중복으로 확장될 기술 부채.

### 1.3 해결하지 않으면

- 관리자 `/management/**` 전체 서브트리가 영구적으로 클라이언트 렌더링에 묶인다. 추후 `error.tsx`·`loading.tsx` 를 도입해도 Layout이 CC 인 한 Streaming SSR 이점 제한.
- `CommentSection` 같은 "단순 래퍼인데 CC" 패턴이 향후 컴포넌트 추가 시 기본값처럼 복제될 위험.
- Editor 설정(로딩 UI/ssr 옵션) 이 페이지마다 달라지는 drift 위험.

---

## 2. Analyze (선택지 검토)

### 2.1 `management/layout.tsx` 전환 전략

| 옵션 | 설명 | 평가 |
| --- | --- | --- |
| A. 현 상태 유지 | Client Layout + useEffect 인증 | ❌ 위 문제 그대로 |
| B. 전면 SC + 쿠키 존재만 체크 | `cookies().get("refresh_token")` 없으면 `redirect("/login")` | 🟡 HTTPS 에선 Flash 해소, 단 role 검증이 빠져 **비관리자 로그인 유저**의 Flash 는 남음 |
| C. 전면 SC + `/api/v1/token-role` 서버 호출로 role 까지 검증 | 서버에서 백엔드에 `Cookie` 헤더 전달 후 role 검증 | 🟡 아이디어는 이상적이나 **access_token 이 localStorage 저장**이라 서버가 읽을 수 없음. `refresh_token` 만으로 role 조회하려면 백엔드 변경 필요 |
| **D. 하이브리드 — SC Layout(껍데기) + Client `RoleGate`(자식 래핑)** | Layout 자체는 SC, `{children}` 만 작은 CC `RoleGate` 로 감싸 역할 검증 | ✅ 채택 |

**D 선택 근거**:
- `Header`·`Container/Row/Col`·`AdminNavBar` 등 UI 껍데기는 서버에서 즉시 HTML 로 완성 → 번들 감소 + FCP 이점.
- `RoleGate` 는 `{children}` 만 감싸므로, Flash 범위가 "관리자 전체 레이아웃" → "본문 영역" 으로 **축소**.
- HTTP 개발 환경에서도 동작 유지 (현재 `proxy.ts` 가 HTTP 에서 쿠키 검사를 생략하므로 호환).
- access_token 저장 방식(localStorage) 변경 없이 적용 가능 → 범위 폭발 없음.

**B 옵션의 한계 재강조**: 쿠키가 있지만 role 이 `ROLE_USER` 인 유저가 `/management` 에 접근하면 여전히 관리자 껍데기 Flash 가 발생. D 도 본문 영역 Flash 는 남지만 범위가 훨씬 작고, 껍데기가 서버 렌더되므로 **악의적 시도 시 "빈 본문" 만 노출**되어 UX 혼란이 줄어든다.

### 2.2 `CommentSection` 전환

| 옵션 | 설명 | 평가 |
| --- | --- | --- |
| A. `"use client"` 제거만 | SC 래퍼 유지, 하위 `CommentForm`/`CommentList` 는 기존 CC | ✅ 채택 |
| B. 래퍼 삭제, 상위 page.tsx 에서 직접 두 컴포넌트 렌더 | 래퍼 의미 자체 제거 | ❌ 추후 댓글 섹션 헤더/정렬/페이징 확장 시 재도입 필요 |

**A 채택 근거**: 현재는 props pass-through 뿐이지만, 댓글 정렬/필터/페이징 UI 추가 여지가 현실적. 래퍼 유지하되 SC 로만 전환하는 것이 DRY + SC 기본 원칙 모두 만족.

### 2.3 Editor dynamic loader 중복 제거

| 옵션 | 설명 | 평가 |
| --- | --- | --- |
| A. 현 상태 유지 | 각 페이지가 dynamic 호출을 직접 작성 | ❌ 중복 지속 |
| B. 공용 CC 모듈 `components/editor/PostEditorDynamic.tsx` 로 추출 | dynamic 호출 + loading fallback 집중 | ✅ 채택 |

추가 이점: 페이지에서 `dynamic` 호출이 빠지면 `use(params)` (Client 전용 훅) 를 **서버 쪽 `await params`** 로 바꿀 수 있어 **두 페이지 모두 Server Component 로 전환 가능**. 에디터 컴포넌트 자체만 CC.

### 2.4 `Header` / `CategoryNav` / `VisitorCount` / `PostEventListener` 는 현 상태 유지

리뷰도 "🔵 합리적" 판정. 각각 Redux, TanStack Query, EventSource, 이벤트 리스너 사용으로 CC 필요. 본 계획서에서는 건드리지 않는다.

---

## 3. Action (구현 계획 및 설계)

### 3.1 작업 목표

- `app/(admin)/management/layout.tsx` 를 **Server Component** 로 전환 (하이브리드 전략).
- `components/comments/CommentSection.tsx` 를 **Server Component** 로 전환.
- `components/editor/PostEditorDynamic.tsx` 공용 래퍼 추출 → `new-post/page.tsx`, `posts/[id]/page.tsx` 를 **Server Component** 로 전환.

### 3.2 범위

#### 포함

- [x] `app/(admin)/management/layout.tsx` SC 전환 + `cookies()` + `redirect()` 1차 게이트
- [x] `components/management/RoleGate.tsx` 신규 (Client, 2차 게이트 — 현 useEffect 로직 이관)
- [x] `components/comments/CommentSection.tsx` `"use client"` 제거
- [x] `components/editor/PostEditorDynamic.tsx` 신규 (공용 dynamic 래퍼)
- [x] `app/(admin)/management/new-post/page.tsx`, `posts/[id]/page.tsx` SC 전환

#### 제외 (별도 계획서)

- [ ] `Header` / `CategoryNav` 분할 리팩토링 (불필요) — 현 상태 유지
- [ ] `AdminNavBar` SC 전환 검토 — `usePathname` 필요로 CC 유지, 본 회차 건드리지 않음
- [ ] 백엔드 `/api/v1/token-role` 을 `refresh_token` 기반으로 확장 (서버에서 role 까지 읽기) → 3회차 6-4 로 이월
- [ ] `useSuspenseQuery` 도입 (2회차 4-5)

### 3.3 설계 상세

#### 3.3.1 `management/layout.tsx` 하이브리드 구조

```tsx
// app/(admin)/management/layout.tsx — Server Component
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { Container, Row, Col } from "react-bootstrap";
import { Header } from "@/components/layout/Header";
import { AdminNavBar } from "@/components/management/AdminNavBar";
import { RoleGate } from "@/components/management/RoleGate";

export default async function ManagementLayout({
  children,
}: { children: React.ReactNode }) {
  // 1차 게이트 — HTTPS 운영 환경: refresh_token 쿠키 존재 확인
  //   proxy.ts 와 동일한 정책. 개발(HTTP)에서는 Secure 쿠키가 전송되지 않으므로
  //   이 체크가 통과될 수 없어 항상 /login 으로 밀리는 문제를 피하기 위해
  //   개발 환경에서는 생략한다.
  const isHttps = process.env.NODE_ENV === "production";
  if (isHttps) {
    const token = (await cookies()).get("refresh_token");
    if (!token) redirect("/login?from=/management");
  }

  return (
    <div>
      <Header />
      <main className="layout-main">
        <Container>
          <Row>
            <Col xs={12} md={3} className="order-2 order-md-1">
              <div className="sidebar-sticky">
                <AdminNavBar />
              </div>
            </Col>
            <Col xs={12} md={9} className="order-1 order-md-2">
              {/* 2차 게이트 — role 검증 (Client, 작게 한정) */}
              <RoleGate>{children}</RoleGate>
            </Col>
          </Row>
        </Container>
      </main>
    </div>
  );
}
```

**설계 의도**:
- 껍데기(`Header`, `AdminNavBar`) 는 서버에서 즉시 HTML 로 렌더 → Flash 범위가 본문 영역(`{children}`)으로 축소.
- `Header` / `AdminNavBar` 자체는 CC 지만, SC 부모가 CC 자식을 import 하는 것은 문제없음 — Next.js 가 경계 자동 식별.
- 환경 판별은 `process.env.NODE_ENV` 로 고정. `request.url` 기반(`proxy.ts` 방식) 은 Server Component 에서 즉시 접근 불가하므로 NODE_ENV 로 단순화. 단 이 결정은 **리뷰 문서에 기록된 proxy.ts 분기 기준과 일치**시켜 혼선을 방지.

#### 3.3.2 `RoleGate.tsx` 신규 (Client)

```tsx
// components/management/RoleGate.tsx
"use client";

import { useEffect, useState } from "react";
import { useSelector, useDispatch } from "react-redux";
import { useRouter } from "next/navigation";
import { Spinner } from "react-bootstrap";
import { selectIsLoggedIn } from "@/store/userSlice";
import { userLogout } from "@/store/authActions";
import { getRoleFromToken } from "@/lib/authApi";
import type { AppDispatch } from "@/store";

export function RoleGate({ children }: { children: React.ReactNode }) {
  const [role, setRole] = useState<string | null>(null);
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const dispatch = useDispatch<AppDispatch>();
  const router = useRouter();

  useEffect(() => {
    if (!isLoggedIn) {
      router.replace("/login?from=/management");
      return;
    }
    getRoleFromToken()
      .then((data: string) => setRole(data))
      .catch((error: { response?: { status?: number } }) => {
        if (error.response?.status === 401) {
          dispatch(userLogout());
          router.replace("/login?from=/management");
        }
      });
  }, [isLoggedIn, dispatch, router]);

  if (!isLoggedIn || role === null) {
    return (
      <div style={{ display: "flex", justifyContent: "center", padding: "4rem 0" }}>
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Loading...</span>
        </Spinner>
      </div>
    );
  }
  if (role !== "ROLE_ADMIN") {
    router.replace("/");
    return null;
  }
  return <>{children}</>;
}
```

**설계 의도**:
- 기존 `management/layout.tsx` 의 `useEffect` + role 검증 블록을 **원본 그대로 이관**. 로직 변경 없이 위치만 옮김.
- 상태가 `pending` 일 때 Spinner 를 `{children}` 자리(본문)에만 표시 — 껍데기는 이미 렌더됨.
- 비관리자 리다이렉트 동작은 현재와 동일.

#### 3.3.3 `CommentSection.tsx`

- 단순히 첫 줄 `"use client";` 제거. 나머지 JSX 는 동일.
- 상위 `app/(public)/posts/[slug]/page.tsx:103` 의 호출 방식 변경 없음.

#### 3.3.4 `PostEditorDynamic.tsx` 공용 래퍼

```tsx
// components/editor/PostEditorDynamic.tsx
"use client";

import dynamic from "next/dynamic";

const loading = () => <div style={{ padding: "2rem" }}>// loading editor...</div>;

// Tiptap 은 window 의존 → SSR 비활성화
export const PostEditorLazy = dynamic(
  () => import("./PostEditorClient").then((m) => m.PostEditorClient),
  { ssr: false, loading },
);

export const PostEditFormLazy = dynamic(
  () =>
    import("./PostEditFormClient").then((m) => m.PostEditFormClient),
  { ssr: false, loading },
);
```

#### 3.3.5 관리자 에디터 페이지 SC 전환

```tsx
// app/(admin)/management/new-post/page.tsx — Server Component
import { PostEditorLazy } from "@/components/editor/PostEditorDynamic";
export default function NewPostPage() {
  return <PostEditorLazy />;
}
```

```tsx
// app/(admin)/management/posts/[id]/page.tsx — Server Component
import { PostEditFormLazy } from "@/components/editor/PostEditorDynamic";

export default async function EditPostPage({
  params,
}: { params: Promise<{ id: string }> }) {
  const { id } = await params;           // SC 표준: use() 대신 await
  return <PostEditFormLazy postId={id} />;
}
```

### 3.4 변경 대상 파일 목록

| 분류 | 파일 | 작업 |
| --- | --- | --- |
| 수정 | `app/(admin)/management/layout.tsx` | CC → SC, 쿠키 1차 게이트 + `<RoleGate>` 도입 |
| 신규 | `components/management/RoleGate.tsx` | Client, 2차 role 검증 (기존 useEffect 이관) |
| 수정 | `components/comments/CommentSection.tsx` | `"use client"` 제거 |
| 신규 | `components/editor/PostEditorDynamic.tsx` | 공용 dynamic 래퍼 (`PostEditorLazy`, `PostEditFormLazy`) |
| 수정 | `app/(admin)/management/new-post/page.tsx` | CC → SC, 공용 래퍼 사용 |
| 수정 | `app/(admin)/management/posts/[id]/page.tsx` | CC → SC, `use()` → `await`, 공용 래퍼 사용 |
| 미변경 | `proxy.ts` | matcher 동일 — 영향 없음 |
| 미변경 | `Header`, `AdminNavBar`, `CommentForm`, `CommentList`, `PostEditorClient`, `PostEditFormClient` | 기존 CC 유지 |

### 3.5 구현 순서 (작은 단위·독립 체크포인트)

1. **Step 1 — `CommentSection` SC 전환 (가장 안전)**
   - `"use client"` 한 줄 제거.
   - 빌드 통과 + 게시글 상세 페이지 (`/posts/{slug}`) 댓글 영역 렌더 확인.
2. **Step 2 — Editor 공용 래퍼 + 두 페이지 SC 전환**
   - `components/editor/PostEditorDynamic.tsx` 신규.
   - `new-post/page.tsx`, `posts/[id]/page.tsx` 수정.
   - 빌드 후 관리자 에디터 페이지 접근해 에디터 로드 확인.
3. **Step 3 — `RoleGate` 신규**
   - `components/management/RoleGate.tsx` 에 기존 useEffect 로직 이관.
   - 단독으로는 아직 연결되지 않음 → 빌드만 통과 확인.
4. **Step 4 — `management/layout.tsx` SC 전환**
   - `"use client"` 제거, `cookies()` + `redirect()` 추가, `{children}` 을 `<RoleGate>` 로 래핑.
   - 빌드 후 `/management` 비로그인 접근 → (HTTP dev) RoleGate 가 `/login` 리다이렉트 확인.

### 3.6 주요 트레이드오프

| 항목 | Trade-off |
| --- | --- |
| 하이브리드 (SC Layout + CC RoleGate) | Flash 범위 대폭 축소 / 완전 제거는 아님 (본문 Spinner 표시 구간 존재) — 완전 제거는 6-4 (백엔드 refresh_token 기반 role API) 이후 가능 |
| `NODE_ENV === "production"` 분기 | proxy.ts 의 `request.url` 기반 분기와 **형식이 다름** — 두 분기가 실제로는 동일 환경을 가리키도록 배포 세팅 유지 필요(운영은 항상 HTTPS). 추후 배포에서 HTTPS/HTTP 가 엇갈리면 혼선 가능 |
| `PostEditorDynamic` 한 파일에 두 컴포넌트 export | 에디터가 추후 더 늘어나면 단일 파일이 비대해질 수 있음 — 당장은 2개라 허용 |
| `CommentSection` SC 전환의 번들 효과 | 거의 0 (하위 CommentForm/CommentList 가 이미 CC 라 "경계 위치" 만 리프로 이동) — 그래도 SC 기본 원칙 준수는 가치 있음 |

### 3.7 예상 이슈 및 대응

| 이슈 | 대응 |
| --- | --- |
| SC Layout 에서 CC 컴포넌트(`Header`, `AdminNavBar`)를 import 할 때 `"use client"` 경계 식별 실패 | Next.js 는 import 시 자동으로 경계를 식별. 문제 발생 시 각 CC 파일의 `"use client"` 디렉티브가 파일 **최상단** 에 있는지 확인(주석도 그 위에 오면 안 됨) |
| `HTTP 개발 환경에서 `cookies()` 가 `refresh_token` 을 못 읽어 무한 리다이렉트 | `isHttps` 분기로 개발 환경에서 1차 게이트 생략. 실제 역할 검증은 RoleGate(Client) 가 담당하므로 dev 에서도 기존 로직 동일하게 작동 |
| `RoleGate` 가 렌더된 후 `router.replace("/")` 호출 시 Hydration 불일치 경고 | 기존 로직과 동일 — 현재도 CC Layout 에서 동일하게 동작 중이며 경고 없음. `return null` 시점이 effect 후라 이슈 없을 것으로 예상 |
| `await params` 전환 시 TypeScript 오류 | Next.js 16 에서 `params` 는 `Promise<T>` 타입. 이미 `/posts/[slug]/page.tsx` 가 같은 패턴 사용 중 — 동일 방식으로 `const { id } = await params` |
| `PostEditorDynamic` 의 `dynamic` 호출이 모듈 스코프에서 일어나 Tree-shaking 영향 | 문제없음. 각 페이지는 필요한 export 만 import 하며 dynamic 은 **호출 당시 런타임** 에 chunk 를 로드 |

---

## 4. Result (검증 계획)

### 4.1 빌드·타입 검증

- `.next/` 삭제 후 `npm run build` 성공. 경고 수 기존 대비 증가 없음.
- 빌드 로그에서 **라우트 유형 표시**를 확인:
  - `/management` / `/management/categories` / `/management/temporary-storage` — ○(Static) 또는 ƒ(Dynamic). **변경 전후 유형 동일** 기대(내부는 RoleGate 가 여전히 CSR).
  - `/management/new-post` — 이전엔 CC 뿐인 리프였음. SC 전환 후에도 실제 에디터가 CSR 되므로 라우트 타입 변화 없음(그러나 빌드 로그에 "use client" 경계 감소는 기대).
- `npx tsc --noEmit` 통과.

### 4.2 경로별 수동 QA (개발 서버)

| 시나리오 | 입력 | 기대 결과 |
| --- | --- | --- |
| 비로그인 관리자 접근 (HTTP dev) | `/management` | SC Layout 껍데기 즉시 렌더 → RoleGate 가 `isLoggedIn=false` 확인 → `/login?from=/management` 리다이렉트 |
| 비관리자 로그인 상태 | `/management` (ROLE_USER) | 껍데기 렌더 + 본문 Spinner → `role="ROLE_USER"` 판정 → `/` 리다이렉트. **껍데기만** 잠깐 노출되고 내부 컨텐츠는 노출 안 됨 |
| 관리자 로그인 | `/management` (ROLE_ADMIN) | 껍데기 즉시 렌더 + 본문 Spinner → role 확인 완료 → 관리자 컨텐츠 렌더 |
| 새 글 작성 | `/management/new-post` | Tiptap 에디터 dynamic load (loading fallback 표시 후 로드) |
| 게시글 편집 | `/management/posts/1` | Tiptap 에디터 + postId=1 로드 |
| 댓글 섹션 | `/posts/{slug}` | 댓글 입력/목록 정상 렌더, 기존과 시각적 차이 없음 |

### 4.3 정적 검증

- `rg -n "\"use client\"" app/\\(admin\\)/management/layout.tsx` → 0건.
- `rg -n "\"use client\"" components/comments/CommentSection.tsx` → 0건.
- `rg -n "\"use client\"" app/\\(admin\\)/management/new-post/page.tsx app/\\(admin\\)/management/posts/\\[id\\]/page.tsx` → 0건.
- `rg -n "use(params)" app/` → 관리자 에디터 편집 페이지에서 제거됨 확인.
- `rg -n "getRoleFromToken" components/management/RoleGate.tsx` → 1건.

### 4.4 성공 기준

- [ ] 위 4.1 / 4.2 / 4.3 전부 통과.
- [ ] 관리자 페이지 진입 시 **네트워크 탭에서 `token-role` 응답 수신 전까지도 `Header`·`AdminNavBar` 는 이미 HTML 로 존재**함을 DevTools 로 직접 확인.
- [ ] 댓글 섹션 기능 회귀 없음: 작성/삭제/답글 전부 정상.
- [ ] 관리자 에디터(신규/편집) 로드 정상.

### 4.5 테스트 코드

- 프로젝트에 프론트 단위 테스트 인프라 없음 → 수동 QA 로 검증. 
- 추후 Playwright 시나리오(`/management` 비로그인 리다이렉트) 가 있으면 연결. 없으면 본 회차에서 별도 도입하지 않음.

---

## 5. 후속 연결 (이월 작업과의 관계)

- **1-3** `app/(admin)/management/error.tsx` — 관리자 에디터 작업 중 401/403 복구 UX. 본 계획의 SC Layout 위에 자연스럽게 얹힘.
- **6-4** 백엔드 `refresh_token` 기반 role 조회 엔드포인트 추가 시 RoleGate 를 제거하고 Layout 자체에서 `redirect()` 로 일원화 가능. 그때까지 하이브리드 유지.
- **2회차 4-5** `useSuspenseQuery` 도입 시 RoleGate 의 `useQuery` 변경은 검토 대상 — 현 회차에서는 useEffect + 내부 state 유지.

---

## 6. 체크리스트

- [ ] 계획서 검토 및 사용자 확인
- [ ] Step 1 — `CommentSection` SC 전환 + 빌드 통과
- [ ] Step 2 — `PostEditorDynamic` 신규 + 두 에디터 페이지 SC 전환 + 빌드 통과
- [ ] Step 3 — `RoleGate` 신규 + 빌드 통과
- [ ] Step 4 — `management/layout.tsx` SC 전환 + 빌드 통과 + `/management` 리다이렉트 동작 확인
- [ ] § 4 전 항목 통과
- [ ] 변경 내용 요약 후 사용자 커밋 확인 요청 (커밋 자동 실행 금지)
