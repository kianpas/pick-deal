"use client";

import { ChevronDown, Image as ImageIcon, ImageOff } from "lucide-react";
import { useState } from "react";
import type { SortOption } from "@/lib/types";

const SORTS: { id: SortOption; name: string }[] = [
  { id: "recommend", name: "추천순" },
  { id: "popular", name: "인기순" },
  { id: "latest", name: "최신순" },
  { id: "comments", name: "댓글순" },
  { id: "price-asc", name: "가격낮은순" },
  { id: "price-desc", name: "가격높은순" },
];

interface Props {
  showThumbnail: boolean;
  onToggleThumbnail: () => void;
}

export function SortBar({ showThumbnail, onToggleThumbnail }: Props) {
  const [active, setActive] = useState<SortOption>("recommend");

  return (
    <div className="flex items-center gap-1">
      <div className="flex flex-1 items-center gap-1 overflow-x-auto scrollbar-hide">
        {SORTS.map((s) => {
          const isActive = s.id === active;
          return (
            <button
              key={s.id}
              type="button"
              onClick={() => setActive(s.id)}
              className={`inline-flex shrink-0 items-center gap-1 rounded-full px-3 py-1.5 text-sm font-medium transition ${
                isActive
                  ? "bg-brand-soft text-brand"
                  : "text-fg-muted hover:bg-surface hover:text-fg"
              }`}
            >
              {s.name}
              {isActive && <ChevronDown className="size-3.5" />}
            </button>
          );
        })}
      </div>

      <button
        type="button"
        onClick={onToggleThumbnail}
        aria-label={showThumbnail ? "썸네일 숨기기" : "썸네일 보이기"}
        className={`ml-1 grid size-8 shrink-0 place-items-center rounded-lg border transition ${
          showThumbnail
            ? "border-brand-soft bg-brand-soft text-brand"
            : "border-border bg-surface text-fg-muted hover:bg-surface-hover hover:text-fg"
        }`}
      >
        {showThumbnail ? <ImageIcon className="size-4" /> : <ImageOff className="size-4" />}
      </button>
    </div>
  );
}
