export const READ_DEALS_STORAGE_KEY = "pickdeal:read-deals:v1";
/** 서버 보관 기간(docs/04 §4)과 맞춘다. 그보다 오래된 딜은 목록에 다시 나오지 않는다. */
const MAX_AGE = 90 * 24 * 60 * 60 * 1000;
const MAX_ENTRIES = 2000;
export const READ_DEALS_CHANGED_EVENT = "pickdeal:read-deals-changed";

/** 읽음 기록 키 → 읽은 시각(ms). 그룹 카드는 대표가 바뀔 수 있어 그룹 ID로도 기록한다. */
export type ReadDeals = ReadonlyMap<string, number>;

interface ReadTarget {
  id: number;
  groupId: number | null;
}

function keysOf(deal: ReadTarget): string[] {
  return deal.groupId === null ? [`d:${deal.id}`] : [`g:${deal.groupId}`, `d:${deal.id}`];
}

/** 저장된 원문을 검증하고 오래된 기록을 버린다. 형식이 깨졌으면 빈 기록으로 본다. */
export function parseReadDeals(raw: string | null, now = Date.now()): ReadDeals {
  if (!raw) return new Map();
  try {
    const parsed: unknown = JSON.parse(raw);
    if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) return new Map();
    const entries = Object.entries(parsed)
      .filter((entry): entry is [string, number] => /^[dg]:[1-9]\d*$/.test(entry[0])
        && typeof entry[1] === "number" && Number.isFinite(entry[1])
        && entry[1] <= now && now - entry[1] <= MAX_AGE)
      .sort((a, b) => b[1] - a[1])
      .slice(0, MAX_ENTRIES);
    return new Map(entries);
  } catch {
    return new Map();
  }
}

export function isDealRead(readDeals: ReadDeals, deal: ReadTarget): boolean {
  return keysOf(deal).some((key) => readDeals.has(key));
}

/** 브라우저 기기별 편의 기록이다. 저장이 차단돼도 화면 이동은 그대로 동작한다. */
export function markDealRead(deal: ReadTarget, now = Date.now()): void {
  try {
    const next = new Map(parseReadDeals(localStorage.getItem(READ_DEALS_STORAGE_KEY), now));
    for (const key of keysOf(deal)) next.set(key, now);
    const kept = [...next].sort((a, b) => b[1] - a[1]).slice(0, MAX_ENTRIES);
    localStorage.setItem(READ_DEALS_STORAGE_KEY, JSON.stringify(Object.fromEntries(kept)));
    window.dispatchEvent(new Event(READ_DEALS_CHANGED_EVENT));
  } catch {
    // 저장소 차단·용량 초과 시 읽음 표시만 생략한다.
  }
}

export function readStoredReadDeals(): string | null {
  try {
    return localStorage.getItem(READ_DEALS_STORAGE_KEY);
  } catch {
    return null;
  }
}
