"use client";

import { Search } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";

/**
 * 상단바 검색. Enter 제출 시 홈으로 ?q= 네비게이션한다(검색 상태의 SSOT는 URL).
 * 현재 정렬(?sort=)은 유지하고, 빈 검색어 제출은 q를 지운다(전체 목록).
 */
export function SearchBox() {
  const searchParams = useSearchParams();
  const query = searchParams.get("q") ?? "";
  return <SearchForm key={query} initialValue={query} />;
}

function SearchForm({ initialValue }: { initialValue: string }) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [value, setValue] = useState(initialValue);

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
    <form role="search" onSubmit={handleSubmit} className="flex min-w-0 w-full items-center rounded-lg border border-border bg-surface focus-within:border-border-strong">
      <label htmlFor="deal-search" className="sr-only">상품명, 브랜드, 키워드 검색</label>
        <input
          id="deal-search"
          name="q"
          type="search"
          enterKeyHint="search"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder="상품명, 브랜드, 키워드 검색"
          className="min-h-11 min-w-0 w-full flex-1 rounded-lg bg-transparent px-3 text-base placeholder:text-fg-muted md:text-sm"
        />
        <button type="submit" aria-label="검색" className="grid size-11 shrink-0 place-items-center rounded-lg text-fg-muted hover:bg-surface-hover hover:text-fg">
          <Search className="size-4" aria-hidden="true" />
        </button>
    </form>
  );
}
