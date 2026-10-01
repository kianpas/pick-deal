"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useTransition } from "react";
import type { SourceItem } from "@/lib/api-types";
import { chipClassName, FilterChipRow } from "@/components/filter/FilterChipRow";

export function CommunityFilter({ sources, selected, failed }: {
  sources: SourceItem[];
  selected: number[];
  failed: boolean;
}) {
  const router = useRouter();
  const params = useSearchParams();
  const [pending, startTransition] = useTransition();

  function select(id?: number) {
    const next = id === undefined ? [] : selected.includes(id)
      ? selected.filter((value) => value !== id) : [...selected, id];
    const query = new URLSearchParams(params);
    query.delete("sourceId");
    query.delete("page");
    next.forEach((value) => query.append("sourceId", String(value)));
    startTransition(() => router.push(query.size ? `/?${query}` : "/", { scroll: false }));
  }

  return (
    <section aria-label="커뮤니티 필터" className="space-y-1">
      {/* 선택 상태는 칩 자체(✓·강조)로만 보여준다 — 아래에 같은 정보를 한 줄 더 쌓아 목록을 밀지 않는다. */}
      <FilterChipRow
        label={<h2 className="text-sm font-semibold text-fg">커뮤니티<span className="sr-only"> · 여러 곳 선택 가능</span></h2>}
        ariaLabel="커뮤니티 선택"
        scrollKey={selected.join(",")}
        pending={pending}
      >
        {[{ id: undefined, name: "전체" }, ...sources.filter((source) => source.visible)].map((source) => {
          const active = source.id === undefined ? selected.length === 0 : selected.includes(source.id);
          return (
            <button key={source.id ?? "all"} type="button" disabled={pending}
              aria-pressed={active} onClick={() => select(source.id)}
              className={chipClassName(active)}>
              {active && source.id !== undefined && <span aria-hidden="true">✓</span>}{source.name}
            </button>
          );
        })}
      </FilterChipRow>
      {failed && <p role="status" className="text-xs text-danger">커뮤니티를 불러오지 못했어요. 페이지를 새로고침해 주세요.</p>}
      {selected.some((id) => !sources.some((source) => source.visible && source.id === id)) && !failed && (
        <p className="text-xs text-fg-muted">선택한 커뮤니티 중 표시할 수 없는 항목이 있어요. 전체를 눌러 해제할 수 있습니다.</p>
      )}
    </section>
  );
}
