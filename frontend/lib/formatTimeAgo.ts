export function formatTimeAgo(timestamp: string): string {
  const currentDate = new Date();
  const targetDate = new Date(timestamp);
  const timeDifferenceInSeconds = Math.floor(
    (currentDate.getTime() - targetDate.getTime()) / 1000
  );

  if (timeDifferenceInSeconds >= 60 * 60 * 24 * 365) {
    return `${Math.floor(timeDifferenceInSeconds / (60 * 60 * 24 * 365))}년 전`;
  }
  if (timeDifferenceInSeconds >= 60 * 60 * 24 * 30) {
    return `${Math.floor(timeDifferenceInSeconds / (60 * 60 * 24 * 30))}달 전`;
  }
  if (timeDifferenceInSeconds >= 60 * 60 * 24 * 7) {
    return `${Math.floor(timeDifferenceInSeconds / (60 * 60 * 24 * 7))}주 전`;
  }
  if (timeDifferenceInSeconds >= 60 * 60 * 24) {
    return `${Math.floor(timeDifferenceInSeconds / (60 * 60 * 24))}일 전`;
  }
  if (timeDifferenceInSeconds >= 60 * 60) {
    return `${Math.floor(timeDifferenceInSeconds / (60 * 60))}시간 전`;
  }
  if (timeDifferenceInSeconds >= 60) {
    return `${Math.floor(timeDifferenceInSeconds / 60)}분 전`;
  }
  if (timeDifferenceInSeconds > 5) {
    return `${timeDifferenceInSeconds}초 전`;
  }
  return "방금";
}
