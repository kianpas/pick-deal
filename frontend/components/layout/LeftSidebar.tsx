"use client";

import {
  Bell,
  Home,
  Tag,
  UserCircle,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { SourceVisibilityList } from "@/components/source/SourceVisibilityList";
import { READ_ONLY } from "@/lib/runtime-config";

const NAV: { icon: LucideIcon; label: string; href: string }[] = [
  { icon: Home, label: "홈", href: "/" },
  { icon: Tag, label: "키워드 관리", href: "/settings/keywords" },
  { icon: Bell, label: "알림", href: "/notifications" },
  { icon: UserCircle, label: "마이페이지", href: "/me" },
];

/** 현재 경로 기준 활성 여부. 홈("/")만 정확히 일치로 판정한다. */
function isNavActive(pathname: string, href: string): boolean {
  return href === "/" ? pathname === "/" : pathname.startsWith(href);
}

export function LeftSidebar() {
  const pathname = usePathname();
  const [notifyEnabled, setNotifyEnabled] = useState(true);


  return (
    <aside className="sticky top-16 hidden h-[calc(100vh-4rem)] w-60 shrink-0 flex-col border-r border-border bg-bg md:flex">
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

      {/* Notify toggle */}
      <div className="border-t border-border p-3">
        <div className="flex items-center gap-3 rounded-lg px-3 py-2 text-sm">
          <Bell className="size-4 text-fg-muted" />
          <span className="flex-1 text-fg">알림 받기</span>
          <button
            type="button"
            role="switch"
            aria-checked={notifyEnabled}
            onClick={() => setNotifyEnabled((v) => !v)}
            className={`relative inline-flex h-5 w-9 shrink-0 rounded-full transition ${
              notifyEnabled ? "bg-brand-strong" : "bg-surface-2"
            }`}
          >
            <span
              className={`absolute top-0.5 size-4 rounded-full bg-white shadow transition ${
                notifyEnabled ? "left-[18px]" : "left-0.5"
              }`}
            />
          </button>
        </div>
      </div>
    </aside>
  );
}
