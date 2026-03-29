import { useEffect, useRef, useCallback } from "react";

/**
 * Intersection Observer 기반 무한스크롤 훅
 *
 * @param {function} onIntersect - 트리거 요소가 뷰포트에 진입할 때 호출할 함수
 * @param {boolean}  enabled     - false면 관찰 중단 (hasNextPage && !isFetchingNextPage 전달)
 * @returns {React.RefObject} triggerRef - 감시할 DOM 요소에 연결할 ref
 *
 * rootMargin '300px': 트리거 요소가 뷰포트 하단 300px 앞에 도달하면 미리 요청
 * → 스크롤이 끝에 닿기 전에 데이터를 로드해 끊김 없는 스크롤 UX 제공
 */
export const useInfiniteScroll = (onIntersect, enabled = true) => {
  const triggerRef = useRef(null);

  const handleIntersect = useCallback(
    (entries) => {
      if (entries[0].isIntersecting && enabled) {
        onIntersect();
      }
    },
    [onIntersect, enabled]
  );

  useEffect(() => {
    const el = triggerRef.current;
    if (!el) return;

    const observer = new IntersectionObserver(handleIntersect, {
      threshold: 0,
      rootMargin: "300px",
    });

    observer.observe(el);
    return () => observer.disconnect();
  }, [handleIntersect]);

  return triggerRef;
};
