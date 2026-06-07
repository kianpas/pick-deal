"use client";

import {
  Bell,
  ChevronDown,
  Eye,
  EyeOff,
  Home,
  Plus,
  Tag,
  UserCircle,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ShopIcon } from "@/components/common/ShopIcon";
import { useFilters } from "@/components/filter/FilterProvider";
import { getSources, updateSourceVisibility } from "@/lib/api";
import type { SourceItem } from "@/lib/api-types";
import { SHOP_COUNTS } from "@/lib/mock-data";
import type { ShopId } from "@/lib/types";

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

const VISIBLE_SHOP_LIMIT = 8;

export function LeftSidebar() {
  const { selectedShops, toggleShop, clearShops } = useFilters();
  const router = useRouter();
  const pathname = usePathname();
  const [expanded, setExpanded] = useState(false);
  const [notifyEnabled, setNotifyEnabled] = useState(true);

  // 출처 표시/숨김은 백엔드 DB가 SSOT(AGENTS.md). 마운트 시 실데이터를 불러온다.
  const [sources, setSources] = useState<SourceItem[] | null>(null);
  const [pendingSourceId, setPendingSourceId] = useState<number | null>(null);

  useEffect(() => {
    let active = true;
    getSources()
      .then((data) => {
        if (active) setSources(data);
      })
      .catch(() => {
        if (active) setSources([]); // 백엔드 미기동 등 → 빈 목록으로 표시
      });
    return () => {
      active = false;
    };
  }, []);

  async function handleToggleSource(src: SourceItem) {
    const nextVisible = !src.visible;
    // 낙관적 업데이트(실패 시 롤백)
    setSources((prev) =>
      prev?.map((s) => (s.id === src.id ? { ...s, visible: nextVisible } : s)) ?? prev,
    );
    setPendingSourceId(src.id);
    try {
      await updateSourceVisibility(src.id, nextVisible);
      // 목록은 서버(page.tsx)가 출처 숨김을 반영하므로 서버 컴포넌트를 재실행한다.
      router.refresh();
    } catch {
      setSources((prev) =>
        prev?.map((s) => (s.id === src.id ? { ...s, visible: src.visible } : s)) ?? prev,
      );
    } finally {
      setPendingSourceId(null);
    }
  }

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
        <div>
          <div className="flex items-center justify-between px-3 pb-2">
            <span className="text-[11px] font-medium uppercase tracking-wider text-fg-subtle">
              출처
            </span>
            <span className="text-[11px] text-fg-subtle">표시/숨김</span>
          </div>

          {sources === null ? (
            <p className="px-3 py-1.5 text-xs text-fg-subtle">불러오는 중…</p>
          ) : sources.length === 0 ? (
            <p className="px-3 py-1.5 text-xs text-fg-subtle">출처가 없습니다.</p>
          ) : (
            <ul className="space-y-0.5">
              {sources.map((src) => (
                <li key={src.id}>
                  <button
                    type="button"
                    onClick={() => handleToggleSource(src)}
                    disabled={pendingSourceId === src.id}
                    aria-pressed={src.visible}
                    className={`flex w-full items-center gap-3 rounded-lg px-3 py-1.5 text-sm transition hover:bg-surface disabled:opacity-50 ${
                      src.visible ? "text-fg" : "text-fg-subtle"
                    }`}
                  >
                    <span className="flex-1 truncate text-left">{src.name}</span>
                    {src.visible ? (
                      <Eye className="size-3.5 text-brand" />
                    ) : (
                      <EyeOff className="size-3.5 text-fg-subtle" />
                    )}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* Shops (쇼핑몰 다중 선택 필터) — 데모(판매처 개념은 수집기 단계로 보류) */}
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
