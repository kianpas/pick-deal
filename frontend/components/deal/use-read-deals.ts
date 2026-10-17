"use client";

import { useEffect, useMemo, useSyncExternalStore } from "react";
import {
  READ_DEALS_CHANGED_EVENT,
  READ_DEALS_STORAGE_KEY,
  markDealRead,
  parseReadDeals,
  readStoredReadDeals,
  type ReadDeals,
} from "@/lib/read-deals";

function subscribe(onChange: () => void): () => void {
  const onStorage = (event: StorageEvent) => {
    if (event.key === null || event.key === READ_DEALS_STORAGE_KEY) onChange();
  };
  window.addEventListener(READ_DEALS_CHANGED_EVENT, onChange);
  window.addEventListener("storage", onStorage);
  return () => {
    window.removeEventListener(READ_DEALS_CHANGED_EVENT, onChange);
    window.removeEventListener("storage", onStorage);
  };
}

/**
 * 현재 브라우저의 읽음 기록. 서버 렌더와 첫 hydration은 빈 기록으로 맞춘 뒤
 * 클라이언트에서 저장소 값을 반영해 hydration 불일치를 피한다.
 */
export function useReadDeals(): ReadDeals {
  // 원문 문자열을 스냅샷으로 써서 값이 같으면 다시 렌더하지 않는다.
  const raw = useSyncExternalStore(subscribe, readStoredReadDeals, () => null);
  return useMemo(() => parseReadDeals(raw), [raw]);
}

/** 상세 화면이 열리면 해당 딜(과 그룹)을 읽음으로 기록한다. */
export function MarkDealRead({ id, groupId }: { id: number; groupId: number | null }) {
  useEffect(() => {
    markDealRead({ id, groupId });
  }, [id, groupId]);
  return null;
}
