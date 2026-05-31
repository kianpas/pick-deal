"use client";

import { useState } from "react";
import { CategoryGrid } from "./CategoryGrid";
import { DealList } from "./DealList";
import { SortBar } from "./SortBar";
import { useFilters } from "@/components/filter/FilterProvider";
import type { Deal } from "@/lib/types";

interface Props {
  deals: Deal[];
}

export function DealFeed({ deals }: Props) {
  const [showThumbnail, setShowThumbnail] = useState(true);
  const { applyFilters } = useFilters();

  const filtered = applyFilters(deals);

  return (
    <div className="space-y-4">
      <SortBar
        showThumbnail={showThumbnail}
        onToggleThumbnail={() => setShowThumbnail((v) => !v)}
      />
      <CategoryGrid />
      {filtered.length > 0 ? (
        <DealList deals={filtered} showThumbnail={showThumbnail} />
      ) : (
        <div className="rounded-xl border border-dashed border-border py-12 text-center text-sm text-fg-muted">
          조건에 맞는 딜이 없어요. 필터를 조정해 보세요.
        </div>
      )}
      <div className="py-6 text-center text-sm text-fg-muted">더 많은 핫딜 보기 ↓</div>
    </div>
  );
}
