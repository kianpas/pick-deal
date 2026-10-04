"use client";

import { useTransition } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { resetSearchLocation } from "@/lib/list-location";
import { clearListSnapshot } from "@/lib/list-resume";

/** 목록 상단과 빈 결과 화면에서 같은 초기화 처리를 사용한다. */
export function ResetSearchFilters({ disabled = false }: { disabled?: boolean }) {
  const router = useRouter();
  const params = useSearchParams();
  const [pending, startTransition] = useTransition();

  function reset() {
    clearListSnapshot();
    startTransition(() => router.push(resetSearchLocation(new URLSearchParams(params)), { scroll: false }));
  }

  return (
    <>
      <button type="button" disabled={disabled || pending} onClick={reset}
        className="min-h-11 rounded-lg px-3 py-2 text-sm text-brand disabled:opacity-60">
        검색·필터 초기화
      </button>
      <span role="status" className="sr-only">{pending ? "검색·필터 초기화 중" : ""}</span>
    </>
  );
}
