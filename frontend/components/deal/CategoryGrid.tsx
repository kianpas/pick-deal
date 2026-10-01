"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useId, useState, useTransition } from "react";
import type { DealCategory } from "@/lib/api-types";
import { ChevronDown } from "lucide-react";
import { chipClassName, FilterChipRow } from "@/components/filter/FilterChipRow";

interface Props {
  /** 백엔드가 내려준 실데이터 카테고리 목록. */
  categories: DealCategory[];
  /** 현재 선택된 카테고리(URL ?category=). 없으면 전체. */
  active?: string;
}

/**
 * 카테고리 필터 바. 커뮤니티 필터와 같은 칩·한 줄 스크롤 모양을 쓴다(FilterChipRow).
 * 선택 상태는 URL(?category=)이 SSOT — 클릭은 URL만 바꾸고,
 * 목록 갱신은 서버(page.tsx)의 재실행으로 일어난다(useTransition으로 진행 피드백).
 */
export function CategoryGrid({ categories, active }: Props) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [isPending, startTransition] = useTransition();
  const [expanded, setExpanded] = useState(false);
  const listId = useId();

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
    ...categories.map((c) => ({ name: c.name, value: c.code })),
  ];

  return (
    <FilterChipRow
      label={<h2 className="text-sm font-semibold text-fg">카테고리</h2>}
      ariaLabel="카테고리 필터"
      listId={listId}
      scrollKey={active ?? ""}
      wrap={expanded}
      pending={isPending}
      trailing={
        <button type="button" aria-expanded={expanded} aria-controls={listId}
          aria-label={expanded ? "카테고리 접기" : "카테고리 전체 펼치기"}
          onClick={() => setExpanded((value) => !value)}
          className="flex min-h-11 shrink-0 items-center gap-1 rounded-lg px-2 text-xs text-fg-muted hover:text-fg md:hidden">
          {expanded ? "접기" : "펼치기"}
          <ChevronDown aria-hidden="true" className={`size-4 transition-transform ${expanded ? "rotate-180" : ""}`} />
        </button>
      }
    >
      {items.map((c) => {
        const isActive = (active ?? undefined) === c.value;
        return (
          <button
            key={c.name}
            type="button"
            disabled={isPending}
            onClick={() => setCategory(c.value)}
            aria-pressed={isActive}
            className={chipClassName(isActive)}
          >
            {c.name}
          </button>
        );
      })}
    </FilterChipRow>
  );
}
