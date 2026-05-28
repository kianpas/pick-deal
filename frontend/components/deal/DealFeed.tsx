"use client";

import { useState } from "react";
import { CategoryGrid } from "./CategoryGrid";
import { DealList } from "./DealList";
import { SortBar } from "./SortBar";
import type { Deal } from "@/lib/types";

interface Props {
  deals: Deal[];
}

export function DealFeed({ deals }: Props) {
  const [showThumbnail, setShowThumbnail] = useState(true);

  return (
    <div className="space-y-4">
      <SortBar
        showThumbnail={showThumbnail}
        onToggleThumbnail={() => setShowThumbnail((v) => !v)}
      />
      <CategoryGrid />
      <DealList deals={deals} showThumbnail={showThumbnail} />
      <div className="py-6 text-center text-sm text-fg-muted">더 많은 핫딜 보기 ↓</div>
    </div>
  );
}
