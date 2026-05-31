"use client";

import {
  Bell,
  ChevronDown,
  Eye,
  EyeOff,
  Flame,
  Home,
  MessageSquare,
  Plus,
  User,
  UserCircle,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { useState } from "react";
import { ShopIcon } from "@/components/common/ShopIcon";
import { useFilters } from "@/components/filter/FilterProvider";
import { SHOP_COUNTS, SOURCES } from "@/lib/mock-data";
import type { ShopId } from "@/lib/types";

const NAV: { icon: LucideIcon; label: string; href: string; active?: boolean }[] = [
  { icon: Home, label: "홈", href: "/", active: true },
  { icon: Flame, label: "핫딜", href: "/deals" },
  { icon: MessageSquare, label: "커뮤니티", href: "/community" },
  { icon: Bell, label: "알림", href: "/notifications" },
  { icon: User, label: "내 활동", href: "/activity" },
  { icon: UserCircle, label: "마이페이지", href: "/me" },
];

const VISIBLE_SHOP_LIMIT = 8;

export function LeftSidebar() {
  const { hiddenSources, toggleSource, selectedShops, toggleShop, clearShops } =
    useFilters();
  const [expanded, setExpanded] = useState(false);
  const [notifyEnabled, setNotifyEnabled] = useState(true);

  const visible = expanded ? SHOP_COUNTS : SHOP_COUNTS.slice(0, VISIBLE_SHOP_LIMIT);

  return (
    <aside className="sticky top-16 hidden h-[calc(100vh-4rem)] w-60 shrink-0 flex-col border-r border-border bg-bg md:flex">
      <nav className="flex-1 overflow-y-auto scrollbar-thin px-3 pb-4 pt-4 space-y-5">
        {/* Primary nav */}
        <ul className="space-y-0.5">
          {NAV.map((item) => (
            <li key={item.label}>
              <a
                href={item.href}
                className={`flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm transition ${
                  item.active
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

        {/* Sources (출처 표시/숨김) */}
        <div>
          <div className="flex items-center justify-between px-3 pb-2">
            <span className="text-[11px] font-medium uppercase tracking-wider text-fg-subtle">
              출처
            </span>
            <span className="text-[11px] text-fg-subtle">표시/숨김</span>
          </div>

          <ul className="space-y-0.5">
            {SOURCES.map((src) => {
              const hidden = hiddenSources.has(src.id);
              return (
                <li key={src.id}>
                  <button
                    type="button"
                    onClick={() => toggleSource(src.id)}
                    aria-pressed={!hidden}
                    className={`flex w-full items-center gap-3 rounded-lg px-3 py-1.5 text-sm transition hover:bg-surface ${
                      hidden ? "text-fg-subtle" : "text-fg"
                    }`}
                  >
                    <span className="flex-1 truncate text-left">{src.name}</span>
                    {hidden ? (
                      <EyeOff className="size-3.5 text-fg-subtle" />
                    ) : (
                      <Eye className="size-3.5 text-brand" />
                    )}
                  </button>
                </li>
              );
            })}
          </ul>
        </div>

        {/* Shops (쇼핑몰 다중 선택 필터) */}
        <div>
          <div className="flex items-center justify-between px-3 pb-2">
            <span className="text-[11px] font-medium uppercase tracking-wider text-fg-subtle">
              쇼핑몰
            </span>
            {selectedShops.size > 0 ? (
              <button
                type="button"
                onClick={clearShops}
                className="text-[11px] text-brand hover:underline"
              >
                전체 해제
              </button>
            ) : (
              <button
                type="button"
                className="text-fg-subtle hover:text-fg transition"
                aria-label="쇼핑몰 추가"
              >
                <Plus className="size-3.5" />
              </button>
            )}
          </div>

          <ul className="space-y-0.5">
            {visible.map((s) => {
              if (s.id === "all") return null;
              const shopId = s.id as ShopId;
              const isSelected = selectedShops.has(shopId);
              return (
                <li key={s.id}>
                  <button
                    type="button"
                    onClick={() => toggleShop(shopId)}
                    aria-pressed={isSelected}
                    className={`flex w-full items-center gap-3 rounded-lg px-3 py-1.5 text-sm transition ${
                      isSelected
                        ? "bg-brand-soft text-brand"
                        : "text-fg-muted hover:bg-surface hover:text-fg"
                    }`}
                  >
                    <ShopIcon id={s.id} size={24} />
                    <span className="flex-1 truncate text-left">{s.name}</span>
                    <span
                      className={`text-xs tabular-nums ${
                        isSelected ? "text-brand" : "text-fg-subtle"
                      }`}
                    >
                      {s.count}
                    </span>
                  </button>
                </li>
              );
            })}
          </ul>

          {SHOP_COUNTS.length > VISIBLE_SHOP_LIMIT && (
            <button
              type="button"
              onClick={() => setExpanded((v) => !v)}
              className="mt-1 flex w-full items-center justify-center gap-1 px-3 py-1.5 text-xs text-fg-subtle hover:text-fg transition"
            >
              {expanded ? "접기" : "더보기"}
              <ChevronDown
                className={`size-3 transition ${expanded ? "rotate-180" : ""}`}
              />
            </button>
          )}
        </div>
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
