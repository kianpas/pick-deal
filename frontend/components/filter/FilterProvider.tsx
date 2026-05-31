"use client";

import { createContext, useContext, useMemo, useState } from "react";
import type { ReactNode } from "react";
import type { Deal, ShopId, SourceId } from "@/lib/types";

/**
 * 딜 필터 상태를 사이드바(설정)와 목록(소비) 간에 공유한다.
 *
 * - hiddenSources: 출처 "표시/숨김" 설정. 숨긴 출처의 딜은 목록에서 제외한다.
 *   (docs/01 §3.1 #4 — 추후 백엔드 DB에 저장될 영구 설정)
 * - selectedShops: 쇼핑몰 다중 선택 필터. 비어 있으면 전체 노출,
 *   하나 이상 선택되면 해당 쇼핑몰의 딜만 노출하는 일시적 탐색 필터.
 *
 * 필터 적용 순서는 docs/01 §3.2를 따른다(출처 숨김 우선 → 쇼핑몰).
 */
interface FilterContextValue {
  hiddenSources: Set<SourceId>;
  toggleSource: (id: SourceId) => void;
  selectedShops: Set<ShopId>;
  toggleShop: (id: ShopId) => void;
  clearShops: () => void;
  applyFilters: (deals: Deal[]) => Deal[];
}

const FilterContext = createContext<FilterContextValue | null>(null);

export function FilterProvider({ children }: { children: ReactNode }) {
  const [hiddenSources, setHiddenSources] = useState<Set<SourceId>>(new Set());
  const [selectedShops, setSelectedShops] = useState<Set<ShopId>>(new Set());

  const value = useMemo<FilterContextValue>(() => {
    const toggleInSet = <T,>(prev: Set<T>, id: T): Set<T> => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    };

    return {
      hiddenSources,
      toggleSource: (id) => setHiddenSources((prev) => toggleInSet(prev, id)),
      selectedShops,
      toggleShop: (id) => setSelectedShops((prev) => toggleInSet(prev, id)),
      clearShops: () => setSelectedShops(new Set()),
      applyFilters: (deals) =>
        deals
          .filter((d) => !hiddenSources.has(d.source))
          .filter((d) => selectedShops.size === 0 || selectedShops.has(d.shop)),
    };
  }, [hiddenSources, selectedShops]);

  return <FilterContext.Provider value={value}>{children}</FilterContext.Provider>;
}

export function useFilters(): FilterContextValue {
  const ctx = useContext(FilterContext);
  if (!ctx) throw new Error("useFilters must be used within a FilterProvider");
  return ctx;
}
