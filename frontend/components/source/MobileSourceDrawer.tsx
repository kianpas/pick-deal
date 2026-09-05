"use client";

import { SlidersHorizontal, X } from "lucide-react";
import { useEffect, useState } from "react";
import { SourceVisibilityList } from "@/components/source/SourceVisibilityList";

export function MobileSourceDrawer() {
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!open) return;

    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") setOpen(false);
    }

    document.addEventListener("keydown", closeOnEscape);
    return () => document.removeEventListener("keydown", closeOnEscape);
  }, [open]);

  return (
    <>
      <button
        type="button"
        aria-label="출처 설정"
        aria-expanded={open}
        onClick={() => setOpen(true)}
        className="fixed bottom-4 right-4 z-40 flex h-11 items-center gap-2 rounded-full border border-border bg-surface px-4 text-sm font-medium text-fg shadow-lg transition hover:bg-surface-hover md:hidden"
      >
        <SlidersHorizontal className="size-4" />
        출처
      </button>

      {open && (
        <div className="fixed inset-0 z-50 md:hidden">
          <button
            type="button"
            aria-label="출처 설정 닫기"
            onClick={() => setOpen(false)}
            className="absolute inset-0 bg-bg/75 backdrop-blur-sm"
          />
          <section
            role="dialog"
            aria-modal="true"
            aria-labelledby="mobile-source-title"
            className="absolute inset-y-0 right-0 flex w-[min(20rem,85vw)] flex-col border-l border-border bg-bg shadow-xl"
          >
            <div className="flex h-16 items-center justify-between border-b border-border px-4">
              <div>
                <h2 id="mobile-source-title" className="text-sm font-semibold text-fg">
                  출처 설정
                </h2>
                <p className="text-xs text-fg-subtle">목록에 표시할 출처를 선택하세요.</p>
              </div>
              <button
                type="button"
                aria-label="닫기"
                onClick={() => setOpen(false)}
                className="grid size-9 place-items-center rounded-lg text-fg-muted transition hover:bg-surface hover:text-fg"
              >
                <X className="size-4" />
              </button>
            </div>
            <div className="flex-1 overflow-y-auto p-4">
              <SourceVisibilityList />
            </div>
          </section>
        </div>
      )}
    </>
  );
}
