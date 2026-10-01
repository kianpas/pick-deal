import type { DealSummary } from "./api-types";

const KEY = "pickdeal:list-resume:v1";
const MAX_AGE = 15 * 60 * 1000;

export interface ListSnapshot {
  listHref: string;
  deals: DealSummary[];
  nextPage: number;
  hasNext: boolean;
  selectedDealId: number;
  scrollY: number;
  viewportWidth: number;
  showThumbnail: boolean;
  savedAt: number;
}

/** 설정 저장소가 아닌, 같은 탭의 상세 왕복을 위한 일회성 화면 스냅샷. */
export function saveListSnapshot(snapshot: Omit<ListSnapshot, "savedAt">): void {
  try {
    sessionStorage.setItem(KEY, JSON.stringify({ ...snapshot, savedAt: Date.now() }));
  } catch {
    // 저장 차단/용량 초과여도 일반 링크 이동은 정상 동작한다.
    clearListSnapshot();
  }
}

export function clearListSnapshot(): void {
  try { sessionStorage.removeItem(KEY); } catch { /* 저장소가 차단된 환경 */ }
}

export function takeListSnapshot(listHref: string, resumeId: string | null): ListSnapshot | null {
  try {
    const raw = sessionStorage.getItem(KEY);
    clearListSnapshot();
    if (!raw) return null;
    const saved = JSON.parse(raw) as ListSnapshot;
    const age = Date.now() - saved.savedAt;
    if (saved.listHref !== listHref || !Number.isFinite(age) || age < 0 || age > MAX_AGE
        || !Array.isArray(saved.deals) || !saved.deals.length
        || !saved.deals.every((deal) => Number.isSafeInteger(deal.id) && deal.id > 0 && typeof deal.title === "string")
        || !Number.isSafeInteger(saved.nextPage) || saved.nextPage < 1
        || !Number.isSafeInteger(saved.selectedDealId)
        || !saved.deals.some((deal) => deal.id === saved.selectedDealId)
        || typeof saved.hasNext !== "boolean" || typeof saved.showThumbnail !== "boolean"
        || !Number.isFinite(saved.scrollY) || saved.scrollY < 0
        || !Number.isFinite(saved.viewportWidth) || saved.viewportWidth <= 0
        || (resumeId !== null && resumeId !== String(saved.selectedDealId))) return null;
    return saved;
  } catch { return null; }
}
