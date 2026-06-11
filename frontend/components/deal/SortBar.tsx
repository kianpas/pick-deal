"use client";

import { Image as ImageIcon, ImageOff } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";

/** 백엔드가 지원하는 정렬만 노출한다(docs/03 §2.1). latest는 기본값이라 URL에서 생략. */
const SORTS: { id: "latest" | "discount"; name: string }[] = [
  { id: "latest", name: "최신순" },
  { id: "discount", name: "할인율순" },
];

interface Props {
  showThumbnail: boolean;
  onToggleThumbnail: () => void;
}

/**
 * 정렬 탭 + 썸네일 토글. 정렬 상태는 URL(?sort=)이 SSOT —
 * 탭 클릭은 URL만 바꾸고, 목록 갱신은 서버(page.tsx)의 재실행으로 일어난다.
 */
export function SortBar({ showThumbnail, onToggleThumbnail }: Props) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const active = searchParams.get("sort") === "discount" ? "discount" : "latest";

  function setSort(id: "latest" | "discount") {
    const params = new URLSearchParams(searchParams);
    if (id === "latest") params.delete("sort");
    else params.set("sort", id);
    const query = params.toString();
    router.replace(query ? `/?${query}` : "/", { scroll: false });
  }

  return (
    <div className="flex items-center gap-1">
      <div className="flex flex-1 items-center gap-1 overflow-x-auto scrollbar-hide">
        {SORTS.map((s) => {
          const isActive = s.id === active;
          return (
            <button
              key={s.id}
              type="button"
              onClick={() => setSort(s.id)}
              aria-pressed={isActive}
              className={`inline-flex shrink-0 items-center gap-1 rounded-full px-3 py-1.5 text-sm font-medium transition ${
                isActive
                  ? "bg-brand-soft text-brand"
                  : "text-fg-muted hover:bg-surface hover:text-fg"
              }`}
            >
              {s.name}
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
