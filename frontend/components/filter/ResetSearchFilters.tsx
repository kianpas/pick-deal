"use client";

import { useTransition } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { RotateCcw } from "lucide-react";
import { resetSearchLocation } from "@/lib/list-location";
import { clearListSnapshot } from "@/lib/list-resume";

/**
 * 목록 상단(SortBar)과 빈 결과 화면에서 같은 초기화 처리를 사용한다.
 * compact는 항상 있는 SortBar 줄에 들어가 필터를 켜고 꺼도 줄 높이가 바뀌지 않게 한다.
 */
export function ResetSearchFilters({ disabled = false, compact = false, count = 0 }: {
  disabled?: boolean;
  compact?: boolean;
  /** compact에서 보여줄 켜진 조건 종류 수. */
  count?: number;
}) {
  const router = useRouter();
  const params = useSearchParams();
  const [pending, startTransition] = useTransition();

  function reset() {
    clearListSnapshot();
    startTransition(() => router.push(resetSearchLocation(new URLSearchParams(params)), { scroll: false }));
  }

  return (
    <>
      {compact ? (
        <button type="button" disabled={disabled || pending} onClick={reset}
          className="inline-flex min-h-11 shrink-0 items-center gap-1.5 rounded-lg px-2 text-sm font-medium text-brand transition hover:bg-brand-soft disabled:opacity-60">
          <RotateCcw className="size-4" aria-hidden="true" />
          {/* 320px대에서는 정렬 줄이 두 줄로 바뀌지 않도록 아이콘·개수만 보인다. */}
          <span className="max-[359px]:sr-only"><span className="sr-only">검색·필터 </span>초기화</span>
          {count > 0 && (
            <>
              <span aria-hidden="true" className="min-w-5 rounded-full bg-brand-soft px-1.5 text-center text-xs tabular-nums">{count}</span>
              <span className="sr-only">(조건 {count}개)</span>
            </>
          )}
        </button>
      ) : (
        <button type="button" disabled={disabled || pending} onClick={reset}
          className="min-h-11 rounded-lg px-3 py-2 text-sm text-brand disabled:opacity-60">
          검색·필터 초기화
        </button>
      )}
      <span role="status" className="sr-only">{pending ? "검색·필터 초기화 중" : ""}</span>
    </>
  );
}
