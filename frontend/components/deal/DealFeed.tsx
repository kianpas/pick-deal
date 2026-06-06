"use client";

import { useState } from "react";
import { CategoryGrid } from "./CategoryGrid";
import { DealList } from "./DealList";
import { SortBar } from "./SortBar";
import type { DealSummary } from "@/lib/api-types";

interface Props {
  deals: DealSummary[];
}

/**
 * 딜 목록 영역. 서버(page.tsx)에서 백엔드로 받아온 목록을 그대로 렌더한다.
 * 출처/키워드 필터는 백엔드가 서버에서 적용하므로(docs/01 §3.2) 여기서 다시 거르지 않는다.
 * SortBar/CategoryGrid는 아직 데모(로컬 상태)이며, 백엔드 정렬/카테고리 연동은 후속 단계.
 */
export function DealFeed({ deals }: Props) {
  const [showThumbnail, setShowThumbnail] = useState(true);

  return (
    <div className="space-y-4">
      <SortBar
        showThumbnail={showThumbnail}
        onToggleThumbnail={() => setShowThumbnail((v) => !v)}
      />
      <CategoryGrid />
      {deals.length > 0 ? (
        <DealList deals={deals} showThumbnail={showThumbnail} />
      ) : (
        <div className="rounded-xl border border-dashed border-border py-12 text-center text-sm text-fg-muted">
          표시할 핫딜이 없어요.
        </div>
      )}
    </div>
  );
}
