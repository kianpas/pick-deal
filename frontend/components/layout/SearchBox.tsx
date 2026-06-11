"use client";

import { Search } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";

/**
 * 상단바 검색. Enter 제출 시 홈으로 ?q= 네비게이션한다(검색 상태의 SSOT는 URL).
 * 현재 정렬(?sort=)은 유지하고, 빈 검색어 제출은 q를 지운다(전체 목록).
 */
export function SearchBox() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [value, setValue] = useState(searchParams.get("q") ?? "");

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const params = new URLSearchParams(searchParams);
    const q = value.trim();
    if (q) params.set("q", q);
    else params.delete("q");
    const query = params.toString();
    router.push(query ? `/?${query}` : "/");
  }

  return (
    <form onSubmit={handleSubmit} className="w-full">
      <label className="flex items-center gap-2 rounded-lg border border-border bg-surface px-3 py-2 text-sm focus-within:border-border-strong">
        <Search className="size-4 text-fg-subtle" />
        <input
          type="text"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder="상품명, 브랜드, 키워드 검색"
          className="flex-1 bg-transparent outline-none placeholder:text-fg-subtle"
        />
        <kbd className="hidden sm:inline-flex items-center rounded border border-border bg-surface-2 px-1.5 py-0.5 text-[11px] text-fg-subtle">
          ⏎
        </kbd>
      </label>
    </form>
  );
}
