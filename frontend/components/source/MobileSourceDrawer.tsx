"use client";

import { SlidersHorizontal, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { SourceVisibilityList } from "@/components/source/SourceVisibilityList";

export function MobileSourceDrawer() {
  const [open, setOpen] = useState(false);
  const dialogRef = useRef<HTMLDialogElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) return;

    const dialog = dialogRef.current;
    const trigger = triggerRef.current;
    if (!dialog) return;
    const previousOverflow = document.body.style.overflow;
    dialog.showModal();
    document.body.style.overflow = "hidden";
    const desktop = window.matchMedia("(min-width: 48rem)");
    const closeOnDesktop = () => { if (desktop.matches) setOpen(false); };
    desktop.addEventListener("change", closeOnDesktop);
    return () => {
      desktop.removeEventListener("change", closeOnDesktop);
      dialog.close();
      document.body.style.overflow = previousOverflow;
      trigger?.focus();
    };
  }, [open]);

  return (
    <div className="mb-4 md:hidden">
      <button
        ref={triggerRef}
        type="button"
        aria-label="출처 설정"
        aria-haspopup="dialog"
        aria-expanded={open}
        onClick={() => setOpen(true)}
        className="flex min-h-11 items-center gap-2 rounded-lg border border-border bg-surface px-4 text-sm font-medium text-fg transition hover:bg-surface-hover"
      >
        <SlidersHorizontal className="size-4" />
        출처 설정
      </button>

      <dialog ref={dialogRef} aria-labelledby="mobile-source-title"
        onCancel={() => setOpen(false)}
        onClick={(event) => { if (event.target === event.currentTarget) setOpen(false); }}
        className="fixed inset-0 m-0 ms-auto h-dvh max-h-dvh w-[min(20rem,100%)] max-w-full border-s border-border bg-bg p-0 text-fg backdrop:bg-bg/75 backdrop:backdrop-blur-sm">
          <div className="flex h-full flex-col">
            <div className="flex shrink-0 items-center justify-between gap-3 border-b border-border px-4 pt-[max(1rem,env(safe-area-inset-top))] pb-4">
              <div>
                <h2 id="mobile-source-title" className="text-sm font-semibold text-fg">
                  출처 설정
                </h2>
                <p className="text-xs text-fg-subtle">목록에 표시할 출처를 선택하세요.</p>
              </div>
              <button
                type="button"
                aria-label="출처 설정 닫기"
                onClick={() => setOpen(false)}
                className="grid size-11 shrink-0 place-items-center rounded-lg text-fg-muted transition hover:bg-surface hover:text-fg"
              >
                <X className="size-4" />
              </button>
            </div>
            <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain p-4 pb-[max(1rem,env(safe-area-inset-bottom))]">
              <SourceVisibilityList />
            </div>
          </div>
      </dialog>
    </div>
  );
}
