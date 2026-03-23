# P2 리팩토링 계획서

> 작성일: 2026-03-22
> 목적: 번들 최적화(~225KB 절감) + 잔존 보안 취약점 3개 해소

---

## 현황 분석

### moment (2.30.1)
- **사용 파일 2개**, 패턴 완전히 동일
  - `src/components/Boards/BoardDetail.js:54` → `moment(board.uploadDate).format("YYYY-MM-DD")`
  - `src/components/Boards/BoardDetailV2.js:34` → `moment(board.data.createDate).format("YYYY-MM-DD")`
- `src/components/dateFormat.js`는 이미 **네이티브 Date API** 사용 — 변경 불필요
- moment는 tree-shaking 미지원 → 전체 번들에 ~225KB 포함

### react-syntax-highlighter (15.6.6)
- `package.json`에 선언되어 있으나 **`src/` 전체에서 import 없음** (미사용 좀비 패키지)
- prismjs moderate 취약점 3개가 이 체인에서 발생
- 업그레이드(15→16)보다 **제거**가 더 적절한 해결책

---

## 작업 목록

### Task 1: `moment` → `day.js` 교체

**변경 파일**: 2개
**API 변경**: 없음 — `dayjs(date).format("YYYY-MM-DD")`는 moment와 동일한 포맷 문법 사용

#### 1-1. 패키지 교체
```bash
npm remove moment
npm install dayjs
```

#### 1-2. BoardDetail.js 수정
```diff
- import moment from "moment";
+ import dayjs from "dayjs";

- const uploadDateFormat = moment(board.uploadDate).format("YYYY-MM-DD");
+ const uploadDateFormat = dayjs(board.uploadDate).format("YYYY-MM-DD");
```

#### 1-3. BoardDetailV2.js 수정
```diff
- import moment from "moment";
+ import dayjs from "dayjs";

- <span>{moment(board.data.createDate).format("YYYY-MM-DD")}</span>
+ <span>{dayjs(board.data.createDate).format("YYYY-MM-DD")}</span>
```

#### 검증
- 브라우저에서 게시글 상세 페이지 날짜 포맷(`YYYY-MM-DD`) 정상 출력 확인
- `npm run build` 빌드 성공 확인

---

### Task 2: `react-syntax-highlighter` 제거

**변경 파일**: `package.json` 만
**이유**: src/ 전체에서 import 없음 — 업그레이드가 아닌 제거가 올바른 대응

#### 2-1. 패키지 제거
```bash
npm remove react-syntax-highlighter
```

#### 2-2. 취약점 재확인
```bash
npm audit
```
> 예상 결과: 3개 → 0개

#### 검증
- `npm run build` 빌드 성공 확인
- `npm audit` 잔존 취약점 0개 확인

---

## 예상 효과

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| 번들 크기 | moment ~225KB 포함 | day.js ~2KB |
| 취약점 | 3개 (prismjs 체인) | **0개** |
| 패키지 수 | moment + react-syntax-highlighter | dayjs만 추가 |

---

## 커밋 계획

두 작업을 하나의 커밋으로 묶음:

```
chore: moment → dayjs 교체 및 미사용 react-syntax-highlighter 제거 (P2)
```

- 번들 ~225KB 절감 (moment → dayjs 2kB)
- 잔존 보안 취약점 3개 해소 (prismjs 체인 제거)
