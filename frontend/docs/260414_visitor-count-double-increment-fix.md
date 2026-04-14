# 방문자 수 2씩 증가 버그 수정 계획서

## 작업 목표 및 배경

### 증상
페이지 접속 시 방문자 수(total, today)가 매번 2씩 증가한다.

### 원인 분석

#### 핵심 원인: Next.js 개발 모드 + React StrictMode
- `next.config.ts`에 `reactStrictMode` 설정이 없음 → Next.js 13+ 기본값: `true`
- React 18 StrictMode는 **개발 모드**에서 의도적으로 `useEffect`를 두 번 실행 (버그 감지 목적)
- `VisitorCount.tsx`의 `useEffect`가 두 번 실행되며 두 개의 HTTP 요청이 거의 동시에 발송됨

#### 요청 흐름 (버그 발생 시)
```
1. 컴포넌트 마운트 → useEffect 실행 → GET /api/v2/visitor-count (요청 A, 쿠키 없음)
2. StrictMode: 컴포넌트 언마운트 시뮬레이션 (cleanup 없어서 요청 A 취소 안 됨)
3. 컴포넌트 리마운트 → useEffect 재실행 → GET /api/v2/visitor-count (요청 B, 쿠키 없음)
4. 요청 A 응답 도착 → 인터셉터: 쿠키 없음 → isNewVisitor=true → count +1, 쿠키 세팅
5. 요청 B 응답 도착 → 인터셉터: 쿠키 없음 (요청 B는 이미 전송됨) → isNewVisitor=true → count +1
6. 결과: +2 증가
```

#### 왜 production에서는 정상인가
- `NODE_ENV=production` 빌드에서 React StrictMode의 이중 호출은 비활성화됨
- Docker 컨테이너로 운영 시 정상적으로 +1

---

## 구현 접근 방식

### 선택한 방법: AbortController를 사용한 useEffect 정리

`useEffect` cleanup에서 `AbortController.abort()`를 호출하면:
- StrictMode가 언마운트 시뮬레이션 시 → 첫 번째 요청이 취소됨
- 리마운트 후 두 번째 요청만 실제로 서버에 도달 → +1

```tsx
useEffect(() => {
  const controller = new AbortController();

  const fetchVisitorData = async () => {
    try {
      const response = await apiClient.get("/api/v2/visitor-count", {
        signal: controller.signal,
      });
      const { total, today, yesterday } = response.data;
      setVisitor({ total, today, yesterday });
      setLoading(false);
    } catch (error: unknown) {
      // AbortError(ERR_CANCELED): StrictMode 이중 호출 취소 → 무시
      if ((error as { code?: string })?.code === "ERR_CANCELED") return;
      setLoading(false); // 그 외 에러는 로딩 종료
    }
  };

  fetchVisitorData();

  return () => controller.abort(); // cleanup: 요청 취소
}, []);
```

### 왜 reactStrictMode: false를 선택하지 않는가
- StrictMode는 개발 중 side effect 감지에 도움을 줌
- 근본 원인(정리 없는 useEffect)을 수정하는 것이 올바른 접근

---

## 변경 대상 파일

| 파일 | 변경 내용 |
|------|-----------|
| `frontend-next/components/layout/VisitorCount.tsx` | AbortController 추가, ERR_CANCELED 처리 |

---

## 예상 이슈 및 대응

| 이슈 | 대응 |
|------|------|
| Axios가 AbortController signal을 지원하는가 | Axios 0.22+ 지원 (현재 1.x 사용 중) |
| 취소된 요청에서 finally가 실행되어 loading 상태가 잘못 변경 | catch에서 ERR_CANCELED 조기 return, setLoading(false)를 try/catch 각각에서 명시적 호출 |
| production에서 동작 변화 | 없음. cleanup은 unmount 시에만 실행되며, production에서는 StrictMode 이중 호출 없음 |
