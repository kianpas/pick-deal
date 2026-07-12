"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useTransition } from "react";

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
    <div
      className={`flex items-center gap-1 overflow-x-auto scrollbar-hide transition-opacity ${
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
            onClick={() => setCategory(c.value)}
            aria-pressed={isActive}
            className={`inline-flex shrink-0 items-center rounded-full px-3 py-1.5 text-sm font-medium transition ${
              isActive
                ? "bg-brand-soft text-brand"
                : "text-fg-muted hover:bg-surface hover:text-fg"
            }`}
          >
            {c.name}
          </button>
        );
      })}
    </div>
  );
}
