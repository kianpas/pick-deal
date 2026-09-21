"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useId, useRef, useState, useTransition } from "react";
import { ChevronDown } from "lucide-react";

interface Props {
  /** 백엔드가 내려준 실데이터 카테고리 목록. */
  categories: string[];
  /** 현재 선택된 카테고리(URL ?category=). 없으면 전체. */
  active?: string;
}

/**
 * 카테고리 필터 바. SortBar와 같은 pill 시각 언어를 쓴다.
 * 선택 상태는 URL(?category=)이 SSOT — 클릭은 URL만 바꾸고,
 * 목록 갱신은 서버(page.tsx)의 재실행으로 일어난다(useTransition으로 진행 피드백).
 */
export function CategoryGrid({ categories, active }: Props) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [isPending, startTransition] = useTransition();
  const [expanded, setExpanded] = useState(false);
  const listId = useId();
  const listRef = useRef<HTMLDivElement>(null);
  const activeRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    const list = listRef.current;
    const button = activeRef.current;
    if (!expanded && list && button) {
      list.scrollLeft = button.offsetLeft;
    }
  }, [active, expanded]);

  function setCategory(category?: string) {
    const params = new URLSearchParams(searchParams);
    if (category) params.set("category", category);
    else params.delete("category");
    const query = params.toString();
    startTransition(() => {
      router.replace(query ? `/?${query}` : "/", { scroll: false });
    });
  }

  const items: { name: string; value?: string }[] = [
    { name: "전체", value: undefined },
    ...categories.map((c) => ({ name: c, value: c })),
  ];

  return (
    <div className="flex min-w-0 items-start gap-2">
    <div
      id={listId}
      ref={listRef}
      role="group"
      aria-label="카테고리 필터"
      className={`relative flex min-w-0 flex-1 items-center gap-1 p-1 transition-opacity md:flex-wrap ${expanded ? "flex-wrap" : "flex-nowrap overflow-x-auto scrollbar-thin"} ${
        isPending ? "opacity-60" : ""
      }`}
      aria-busy={isPending}
    >
      {items.map((c) => {
        const isActive = (active ?? undefined) === c.value;
        return (
          <button
            key={c.name}
            type="button"
            ref={isActive ? activeRef : undefined}
            disabled={isPending}
            onClick={() => setCategory(c.value)}
            aria-pressed={isActive}
            className={`inline-flex min-h-11 max-w-full shrink-0 items-center rounded-full px-3 py-1.5 text-start text-sm font-medium wrap-anywhere transition md:min-h-10 ${
              isActive
                ? "bg-brand-soft text-brand"
                : "text-fg-muted hover:bg-surface hover:text-fg"
            }`}
          >
            {isActive && <span aria-hidden="true" className="me-1">✓</span>}{c.name}
          </button>
        );
      })}
    </div>
    <button type="button" aria-expanded={expanded} aria-controls={listId}
      onClick={() => setExpanded((value) => !value)}
      className="mt-1 flex min-h-11 shrink-0 items-center gap-1 rounded-lg border border-border px-2 text-xs text-fg-muted md:hidden">
      {expanded ? "접기" : "전체 펼치기"}
      <ChevronDown aria-hidden="true" className={`size-4 ${expanded ? "rotate-180" : ""}`} />
    </button>
    </div>
  );
}
