"use client";

import {
  Gamepad2,
  Grid3x3,
  Laptop,
  Monitor,
  Shirt,
  Soup,
  Tag,
  Ticket,
  Tv,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";

/** 실데이터 카테고리명 → 아이콘. 등록되지 않은 카테고리는 기본 아이콘으로 표시한다. */
const CATEGORY_ICONS: Record<string, LucideIcon> = {
  "PC/하드웨어": Monitor,
  "노트북/모바일": Laptop,
  "가전/TV": Tv,
  "게임/SW": Gamepad2,
  "생활/식품": Soup,
  "패션/의류": Shirt,
  "상품권/쿠폰": Ticket,
};

interface Props {
  /** 백엔드가 내려준 실데이터 카테고리 목록. */
  categories: string[];
  /** 현재 선택된 카테고리(URL ?category=). 없으면 전체. */
  active?: string;
}

/**
 * 카테고리 필터 바. 선택 상태는 URL(?category=)이 SSOT —
 * 클릭은 URL만 바꾸고, 목록 갱신은 서버(page.tsx)의 재실행으로 일어난다.
 */
export function CategoryGrid({ categories, active }: Props) {
  const router = useRouter();
  const searchParams = useSearchParams();

  function setCategory(category?: string) {
    const params = new URLSearchParams(searchParams);
    if (category) params.set("category", category);
    else params.delete("category");
    const query = params.toString();
    router.replace(query ? `/?${query}` : "/", { scroll: false });
  }

  const items: { name: string; value?: string; icon: LucideIcon }[] = [
    { name: "전체", value: undefined, icon: Grid3x3 },
    ...categories.map((c) => ({ name: c, value: c, icon: CATEGORY_ICONS[c] ?? Tag })),
  ];

  return (
    <div className="flex items-end gap-2 overflow-x-auto scrollbar-hide border-b border-border">
      {items.map((c) => {
        const isActive = (active ?? undefined) === c.value;
        return (
          <button
            key={c.name}
            type="button"
            onClick={() => setCategory(c.value)}
            aria-pressed={isActive}
            className={`group flex shrink-0 flex-col items-center gap-1.5 px-3 pb-2.5 pt-3 transition ${
              isActive ? "text-brand" : "text-fg-muted hover:text-fg"
            }`}
          >
            <span
              className={`grid size-10 place-items-center rounded-full transition ${
                isActive
                  ? "bg-brand-soft"
                  : "bg-surface group-hover:bg-surface-hover"
              }`}
            >
              <c.icon className="size-5" />
            </span>
            <span className="text-xs font-medium">{c.name}</span>
            <span
              className={`h-0.5 w-full rounded-full transition ${
                isActive ? "bg-brand" : "bg-transparent"
              }`}
            />
          </button>
        );
      })}
    </div>
  );
}
