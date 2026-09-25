"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useTransition } from "react";
import type { SourceItem } from "@/lib/api-types";

export function ActiveFilters({ sources }: { sources: SourceItem[] }) {
  const params = useSearchParams();
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const items = ["q", "category", "shopName", "sourceId"].flatMap((key) =>
    [...new Set(params.getAll(key))].filter(Boolean).map((value) => ({
      key, value,
      label: key === "sourceId" ? `커뮤니티: ${sources.find((s) => String(s.id) === value)?.name ?? value}`
        : `${key === "q" ? "검색" : key === "category" ? "카테고리" : "쇼핑몰"}: ${value}`,
    })),
  );
  if (!items.length) return null;

  function remove(key?: string, value?: string) {
    const next = new URLSearchParams(params);
    if (key) next.delete(key, value);
    else for (const name of ["q", "category", "shopName", "sourceId"]) next.delete(name);
    next.delete("page");
    startTransition(() => router.push(next.size ? `/?${next}` : "/", { scroll: false }));
  }

  return (
    <section aria-label="선택한 조건" aria-busy={pending} className="flex flex-wrap items-center gap-2">
      {items.map(({ key, value, label }) => (
        <button key={`${key}:${value}`} type="button" disabled={pending} onClick={() => remove(key, value)}
          aria-label={`${label} 해제`}
          className="min-h-11 max-w-full rounded-lg border border-border bg-surface px-3 py-2 text-start text-xs text-fg-muted wrap-anywhere hover:text-fg disabled:opacity-60">
          {label} <span aria-hidden="true">×</span>
        </button>
      ))}
      <button type="button" disabled={pending} onClick={() => remove()}
        className="min-h-11 px-3 text-sm text-brand disabled:opacity-60">전체 초기화</button>
      <span role="status" className="sr-only">{pending ? "조건 해제 중" : `${items.length}개 조건 적용`}</span>
    </section>
  );
}
