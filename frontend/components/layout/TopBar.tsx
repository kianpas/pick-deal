import { Bell, Flame, Search } from "lucide-react";
import { ThemeToggle } from "@/components/common/ThemeToggle";

/**
 * 풀너비 헤더.
 * 모바일에서는 글쓰기/알림 라벨을 숨기고 아이콘만 노출한다.
 */
export function TopBar() {
  return (
    <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-border bg-bg/85 px-4 backdrop-blur md:px-6">
      {/* Logo */}
      <a href="/" className="flex shrink-0 items-center gap-1.5">
        <span className="text-lg font-semibold tracking-tight">PickDeal</span>
        <Flame className="size-4 text-brand" aria-hidden />
      </a>

      {/* Search */}
      <div className="mx-auto w-full max-w-2xl">
        <label className="flex items-center gap-2 rounded-lg border border-border bg-surface px-3 py-2 text-sm focus-within:border-border-strong">
          <Search className="size-4 text-fg-subtle" />
          <input
            type="text"
            placeholder="상품명, 브랜드, 키워드 검색"
            className="flex-1 bg-transparent outline-none placeholder:text-fg-subtle"
          />
          <kbd className="hidden sm:inline-flex items-center rounded border border-border bg-surface-2 px-1.5 py-0.5 text-[11px] text-fg-subtle">
            /
          </kbd>
        </label>
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
