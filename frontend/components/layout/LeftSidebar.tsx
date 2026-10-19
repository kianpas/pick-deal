"use client";

import { Home, Tag } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import { SourceVisibilityList } from "@/components/source/SourceVisibilityList";
import { READ_ONLY } from "@/lib/runtime-config";

const NAV: { icon: LucideIcon; label: string; href: string }[] = [
  { icon: Home, label: "홈", href: "/" },
  { icon: Tag, label: "키워드 관리", href: "/settings/keywords" },
];

/** 현재 경로 기준 활성 여부. 홈("/")만 정확히 일치로 판정한다. */
function isNavActive(pathname: string, href: string): boolean {
  return href === "/" ? pathname === "/" : pathname.startsWith(href);
}

export function LeftSidebar({ filters }: { filters?: ReactNode }) {
  const pathname = usePathname();

  return (
    <aside className="sticky top-16 hidden h-[calc(100vh-4rem)] w-55 shrink-0 flex-col border-e border-border bg-bg md:flex">
      <nav className="flex-1 overflow-y-auto scrollbar-thin px-3 pb-4 pt-4 space-y-5">
        {/* Primary nav */}
        <ul className="space-y-0.5">
          {NAV.filter((item) => !READ_ONLY || item.href !== "/settings/keywords").map((item) => (
            <li key={item.label}>
              <a
                href={item.href}
                className={`flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm transition ${
                  isNavActive(pathname, item.href)
                    ? "bg-surface text-fg"
                    : "text-fg-muted hover:bg-surface hover:text-fg"
                }`}
              >
                <item.icon className="size-4" />
                <span>{item.label}</span>
              </a>
            </li>
          ))}
        </ul>

        {filters}

        {/* Sources (출처 표시/숨김) — 백엔드 실데이터 */}
        {!READ_ONLY && <div>
          <div className="flex items-center justify-between px-3 pb-2">
            <span className="text-[11px] font-medium uppercase tracking-wider text-fg-subtle">
              출처 표시 설정
            </span>
            <span className="text-[11px] text-fg-subtle">표시/숨김</span>
          </div>

          <div className="px-3">
            <SourceVisibilityList />
          </div>
        </div>}

      </nav>
    </aside>
  );
}
