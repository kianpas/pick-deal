export function formatPrice(value: number, currency: string): string {
  if (currency === "USD") {
    return `${value.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}$`;
  }
  return `${value.toLocaleString("ko-KR")}원`;
}

/**
 * ISO-8601(KST) 문자열을 "MM.DD HH:mm"으로 표시한다.
 * 문자열에서 직접 잘라 쓰므로 서버/클라 타임존·현재시각에 의존하지 않는다(하이드레이션 안전).
 * 예: "2026-05-20T18:10:00+09:00" → "05.20 18:10"
 */
export function formatPostedAt(iso: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(iso);
  if (!match) return iso;
  const [, , month, day, hour, minute] = match;
  return `${month}.${day} ${hour}:${minute}`;
}

/**
 * ISO-8601 시각을 현재 시각 기준 상대 표기로 바꾼다.
 * postedAt이 수집 시점 기준 근사값이라(docs/notes 2026-07-09) 분 단위 정밀 표기 대신
 * 거친 상대 표기가 데이터 정밀도에 맞다. 현재 시각에 의존하므로 클라이언트 컴포넌트에서
 * suppressHydrationWarning과 함께 쓴다.
 * 예: "방금 전" / "30분 전" / "5시간 전" / "3일 전" / "05.20"
 */
export function formatRelativeTime(iso: string, now: Date = new Date()): string {
  const time = new Date(iso).getTime();
  if (Number.isNaN(time)) return iso;

  const diffMinutes = Math.floor((now.getTime() - time) / 60_000);
  if (diffMinutes < 1) return "방금 전";
  if (diffMinutes < 60) return `${diffMinutes}분 전`;
  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) return `${diffHours}시간 전`;
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) return `${diffDays}일 전`;
  return formatPostedAt(iso).slice(0, 5); // "MM.DD"
}

/**
 * 수집 딜 제목의 "[판매처] 상품명" 접두사를 분리한다.
 * 접두사가 없으면 store는 null, title은 원문 그대로.
 */
export function splitStoreFromTitle(rawTitle: string): { store: string | null; title: string } {
  const match = /^\[([^\]]+)\]\s*(.+)$/.exec(rawTitle);
  if (!match) return { store: null, title: rawTitle };
  return { store: match[1].trim(), title: match[2].trim() };
}

/**
 * ISO-8601(KST) 문자열을 "YYYY.MM.DD HH:mm"으로 표시한다(상세 화면용 풀 날짜).
 * formatPostedAt과 같은 문자열-슬라이스 방식이라 하이드레이션 안전.
 * 예: "2026-05-20T18:10:00+09:00" → "2026.05.20 18:10"
 */
export function formatFullDateTime(iso: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(iso);
  if (!match) return iso;
  const [, year, month, day, hour, minute] = match;
  return `${year}.${month}.${day} ${hour}:${minute}`;
}
