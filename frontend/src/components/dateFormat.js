export const formatTimeAgo = (timestamp) => {
  const currentDate = new Date();
  const targetDate = new Date(timestamp);

  const timeDifferenceInSeconds = Math.floor((currentDate - targetDate) / 1000);

  // 1년 이상 전
  if (timeDifferenceInSeconds >= 60 * 60 * 24 * 365) {
    const yearsAgo = Math.floor(timeDifferenceInSeconds / (60 * 60 * 24 * 365));
    return `${yearsAgo}년 전`;
  }

  // 1달 이상 전
  if (timeDifferenceInSeconds >= 60 * 60 * 24 * 30) {
    const monthsAgo = Math.floor(timeDifferenceInSeconds / (60 * 60 * 24 * 30));
    return `${monthsAgo}달 전`;
  }

  // 1주일 이상 전
  if (timeDifferenceInSeconds >= 60 * 60 * 24 * 7) {
    const weeksAgo = Math.floor(timeDifferenceInSeconds / (60 * 60 * 24 * 7));
    return `${weeksAgo}주 전`;
  }

  // 24시간 이상 전
  if (timeDifferenceInSeconds >= 60 * 60 * 24) {
    const daysAgo = Math.floor(timeDifferenceInSeconds / (60 * 60 * 24));
    return `${daysAgo}일 전`;
  }

  // 1시간 이상 전
  if (timeDifferenceInSeconds >= 60 * 60) {
    const hoursAgo = Math.floor(timeDifferenceInSeconds / (60 * 60));
    return `${hoursAgo}시간 전`;
  }

  // 1분 이상 전
  if (timeDifferenceInSeconds >= 60) {
    const minutesAgo = Math.floor(timeDifferenceInSeconds / 60);
    return `${minutesAgo}분 전`;
  }

  // 1분 내
  if (timeDifferenceInSeconds < 60 && timeDifferenceInSeconds > 5) {
    return `${timeDifferenceInSeconds}초 전`;
  }
  if (timeDifferenceInSeconds <= 5) {
    return `방금`;
  }
};
