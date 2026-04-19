import { useEffect, useRef, useCallback } from "react";

/**
 * Intersection Observer 기반 무한스크롤 훅
 * rootMargin '300px': 뷰포트 하단 300px 앞에서 미리 요청 → 끊김 없는 UX
 */
export function useInfiniteScroll(
  onIntersect: () => void,
  enabled = true
): React.RefObject<HTMLDivElement | null> {
  const triggerRef = useRef<HTMLDivElement | null>(null);

  const handleIntersect = useCallback(
    (entries: IntersectionObserverEntry[]) => {
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
}
