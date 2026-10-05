"use client";

import { useRouter } from "next/navigation";
import { useTransition } from "react";

export function KeywordLoadError() {
  const router = useRouter();
  const [pending, startTransition] = useTransition();

  return (
    <div className="space-y-3 rounded-lg border border-border p-4">
      <p className="text-sm text-danger">키워드를 불러오지 못했어요.</p>
      <button type="button" disabled={pending}
        onClick={() => startTransition(() => router.refresh())}
        className="min-h-11 rounded-lg border border-border px-4 py-2 text-sm text-brand disabled:opacity-60">
        다시 시도
      </button>
      <p role="status" className="text-xs text-fg-muted">
        {pending ? "키워드를 다시 불러오는 중입니다." : "잠시 후 다시 시도해 주세요."}
      </p>
    </div>
  );
}
