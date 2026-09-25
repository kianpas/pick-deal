import Link from "next/link";
import { Suspense } from "react";
import { Bell, Flame } from "lucide-react";
import { SearchBox } from "@/components/layout/SearchBox";
import { ThemeToggle } from "@/components/common/ThemeToggle";

/**
 * 배경은 풀너비, 콘텐츠는 본문과 같은 최대 너비를 사용하는 헤더.
 * 좁은 화면은 검색을 둘째 줄에 배치해 로고·액션과 공간을 경쟁하지 않게 한다.
 */
export function TopBar() {
  return (
    <header className="sticky top-0 z-30 border-b border-border bg-bg/85 backdrop-blur">
      <div className="relative mx-auto grid w-full max-w-[75rem] grid-cols-[1fr_auto] items-center gap-x-3 gap-y-2 px-4 py-2 sm:px-5 md:flex md:min-h-16">
        <a href="#main-content" className="sr-only focus:not-sr-only focus:absolute focus:start-4 focus:top-2 focus:z-50 focus:rounded-lg focus:bg-bg focus:p-3">
          본문 바로가기
        </a>
        {/* Logo */}
        <Link href="/" className="flex min-h-11 w-fit shrink-0 items-center gap-1.5">
          <span className="text-lg font-semibold tracking-tight">PickDeal</span>
          <Flame className="size-4 text-brand" aria-hidden />
        </Link>

        {/* Search — useSearchParams를 쓰는 클라이언트 컴포넌트라 Suspense로 감싼다 */}
        <div className="order-3 col-span-2 mx-auto min-w-0 w-full md:order-none md:max-w-2xl">
          <Suspense fallback={<div className="h-11 rounded-lg border border-border bg-surface" />}>
            <SearchBox />
          </Suspense>
        </div>

        {/* Right actions */}
        <div className="flex shrink-0 items-center gap-2">
          <ThemeToggle variant="icon" />

          <button
            type="button"
            aria-label="알림"
            className="relative grid size-11 place-items-center rounded-lg border border-border bg-surface text-fg-muted hover:bg-surface-hover hover:text-fg transition"
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
      </div>
    </header>
  );
}
