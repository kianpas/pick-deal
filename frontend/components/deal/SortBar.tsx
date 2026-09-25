"use client";

import { Image as ImageIcon, ImageOff } from "lucide-react";

interface Props {
  showThumbnail: boolean;
  onToggleThumbnail: () => void;
}

/**
 * 목록 상단 바. 정렬은 최신순 하나뿐이라(할인율순은 수집 딜에 정가/할인율이 없어 제외 —
 * 원가 데이터가 생기면 그때 정렬 탭을 되살린다) 라벨만 표시하고, 썸네일 토글을 제공한다.
 */
export function SortBar({ showThumbnail, onToggleThumbnail }: Props) {
  return (
    <div className="flex items-center justify-between gap-1">
      <span className="px-1 text-sm font-medium text-fg-muted">최신순</span>

      <button
        type="button"
        onClick={onToggleThumbnail}
        aria-label="썸네일 표시"
        aria-pressed={showThumbnail}
        className={`inline-flex min-h-11 shrink-0 items-center gap-2 rounded-lg border px-3 text-sm transition ${
          showThumbnail
            ? "border-brand-soft bg-brand-soft text-brand"
            : "border-border bg-surface text-fg-muted hover:bg-surface-hover hover:text-fg"
        }`}
      >
        {showThumbnail ? <ImageIcon className="size-4" aria-hidden="true" /> : <ImageOff className="size-4" aria-hidden="true" />}
        <span>썸네일 표시</span>
      </button>
    </div>
  );
}
