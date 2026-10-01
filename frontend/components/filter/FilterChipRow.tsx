"use client";

import { useEffect, useRef } from "react";
import type { ReactNode } from "react";

/**
 * 목록 상단 필터 칩(커뮤니티·카테고리)의 공통 모양.
 * 보이는 높이는 36px로 낮추고 세로 터치 영역만 가상 요소로 44px까지 넓힌다
 * — 키 큰 둥근 배경이 원처럼 보이거나 주변 요소에 닿지 않게 한다.
 */
export function chipClassName(active: boolean) {
  return `relative inline-flex min-h-9 shrink-0 items-center gap-1 rounded-full border px-3 py-1.5 text-sm font-medium whitespace-nowrap transition disabled:opacity-60 after:absolute after:inset-x-0 after:-inset-y-1 after:content-[''] md:whitespace-normal md:wrap-anywhere ${
    active
      ? "border-brand bg-brand-soft text-brand"
      : "border-border bg-surface text-fg-muted hover:bg-surface-hover hover:text-fg"
  }`;
}

/**
 * 라벨 + 칩 목록 한 줄. 모바일은 한 줄 가로 스크롤(스크롤바 대신 끝 페이드로 더 있음을 알림),
 * md 이상은 줄바꿈한다. `wrap`이면 모바일에서도 줄바꿈한다(카테고리 펼치기).
 * 선택된 칩(aria-pressed)이 스크롤 밖에 있으면 보이도록 옮긴다 — 선택 상태를 다른 줄에
 * 중복 표시하지 않으므로, 칩 자체가 항상 보여야 한다.
 */
export function FilterChipRow({ label, children, trailing, wrap = false, listId, pending = false, ariaLabel, scrollKey }: {
  label: ReactNode;
  children: ReactNode;
  trailing?: ReactNode;
  wrap?: boolean;
  listId?: string;
  pending?: boolean;
  ariaLabel: string;
  /** 선택이 바뀌면 달라지는 값. 바뀔 때 첫 선택 칩을 스크롤해 보여준다. */
  scrollKey: string;
}) {
  const listRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const list = listRef.current;
    if (!list || wrap || list.scrollWidth <= list.clientWidth) return;
    const chip = list.querySelector<HTMLElement>('[aria-pressed="true"]');
    if (!chip) return;
    // 칩의 offsetParent는 relative인 목록이다. 끝 페이드(1.5rem)에 가리지 않는 범위를 '보임'으로 본다.
    const fade = 24;
    const start = chip.offsetLeft;
    const end = start + chip.offsetWidth;
    if (start < list.scrollLeft + 12 || end > list.scrollLeft + list.clientWidth - fade) {
      list.scrollLeft = Math.max(0, start - 12);
    }
  }, [scrollKey, wrap]);

  return (
    <div className="flex min-w-0 items-start gap-1">
      <div className="flex min-h-11 shrink-0 items-center">{label}</div>
      <div
        id={listId}
        ref={listRef}
        role="group"
        aria-label={ariaLabel}
        aria-busy={pending}
        className={`relative flex min-w-0 flex-1 items-center gap-2 py-1 ps-3 transition-opacity md:flex-wrap md:overflow-visible md:pe-1 md:[mask-image:none] ${
          wrap
            ? "flex-wrap pe-1"
            // 양 끝을 짧게 흐려 스크롤 가능함을 알린다. 여백(ps-3·pe-6)이 페이드 폭보다 넓어
            // 스크롤 시작·끝에서는 첫·마지막 칩이 흐려지지 않는다.
            : "flex-nowrap overflow-x-auto scrollbar-hide pe-6 [mask-image:linear-gradient(to_right,transparent,black_0.75rem,black_calc(100%-1.5rem),transparent)]"
        } ${pending ? "opacity-60" : ""}`}
      >
        {children}
      </div>
      {trailing}
    </div>
  );
}
