"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useTransition } from "react";
import { hasSearchFilters } from "@/lib/list-location";
import { clearListSnapshot } from "@/lib/list-resume";
import { ResetSearchFilters } from "./ResetSearchFilters";

/**
 * 목록 근처에 토글로 보이지 않는 조건(검색어·쇼핑몰)만 해제 칩으로 보여준다.
 * 커뮤니티·카테고리·종료 숨기기는 바로 위의 칩/토글이 이미 선택 상태를 보여주므로
 * 같은 정보를 한 줄 더 쌓아 목록을 밀어내지 않는다. 초기화는 탐색 조건이 하나라도 있으면 표시한다.
 */
export function ActiveFilters() {
  const params = useSearchParams();
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const items = ["q", "shopName"].flatMap((key) =>
    [...new Set(params.getAll(key))].filter(Boolean).map((value) => ({
      key, value,
      label: `${key === "q" ? "검색" : "쇼핑몰"}: ${value}`,
    })),
  );
  if (!hasSearchFilters(new URLSearchParams(params))) return null;

  function remove(key: string, value: string) {
    const next = new URLSearchParams(params);
    next.delete(key, value);
    next.delete("page");
    next.delete("resume");
    clearListSnapshot();
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
      <ResetSearchFilters disabled={pending} />
      <span role="status" className="sr-only">{pending ? "조건 해제 중" : ""}</span>
    </section>
  );
}
