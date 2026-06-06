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
