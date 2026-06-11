import Link from "next/link";
import { Suspense } from "react";
import { Bell, Flame } from "lucide-react";
import { SearchBox } from "@/components/layout/SearchBox";
import { ThemeToggle } from "@/components/common/ThemeToggle";

/**
 * 풀너비 헤더.
 * 모바일에서는 글쓰기/알림 라벨을 숨기고 아이콘만 노출한다.
 */
export function TopBar() {
  return (
    <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-border bg-bg/85 px-4 backdrop-blur md:px-6">
      {/* Logo */}
      <Link href="/" className="flex shrink-0 items-center gap-1.5">
        <span className="text-lg font-semibold tracking-tight">PickDeal</span>
        <Flame className="size-4 text-brand" aria-hidden />
      </Link>

      {/* Search — useSearchParams를 쓰는 클라이언트 컴포넌트라 Suspense로 감싼다 */}
      <div className="mx-auto w-full max-w-2xl">
        <Suspense fallback={<div className="h-[38px] rounded-lg border border-border bg-surface" />}>
          <SearchBox />
        </Suspense>
      </div>

      {/* Right actions */}
      <div className="flex shrink-0 items-center gap-2">
        <ThemeToggle variant="icon" />

        <button
          type="button"
          aria-label="알림"
          className="relative grid size-9 place-items-center rounded-lg border border-border bg-surface text-fg-muted hover:bg-surface-hover hover:text-fg transition"
        >
          <Bell className="size-4" />
          <span className="absolute -right-0.5 -top-0.5 grid size-4 place-items-center rounded-full bg-brand-strong text-[10px] font-medium text-white">
            3
          </span>
        </button>

        <div
          className="size-9 shrink-0 rounded-full bg-gradient-to-br from-brand to-brand-strong ring-1 ring-border"
          aria-label="프로필"
        />
      </div>
    </header>
  );
}
