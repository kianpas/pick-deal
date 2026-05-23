"use client";

import { Moon, Sun } from "lucide-react";
import { useTheme } from "@/lib/theme";

interface Props {
  /** "icon": 아이콘만 (헤더용) | "row": 사이드바형 가로 행 */
  variant?: "icon" | "row";
}

export function ThemeToggle({ variant = "icon" }: Props) {
  const { theme, toggle } = useTheme();

  if (theme === null) {
    return variant === "icon" ? (
      <div className="size-9" aria-hidden />
    ) : (
      <div className="h-9" aria-hidden />
    );
  }

  const isDark = theme === "dark";

  if (variant === "row") {
    return (
      <button
        type="button"
        onClick={toggle}
        className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-sm text-fg-muted hover:bg-surface hover:text-fg transition"
        aria-label="테마 전환"
      >
        {isDark ? <Sun className="size-4" /> : <Moon className="size-4" />}
        <span>{isDark ? "라이트 모드" : "다크 모드"}</span>
      </button>
    );
  }

  return (
    <button
      type="button"
      onClick={toggle}
      className="grid size-9 place-items-center rounded-lg border border-border bg-surface text-fg-muted hover:bg-surface-hover hover:text-fg transition"
      aria-label={isDark ? "라이트 모드로 전환" : "다크 모드로 전환"}
    >
      {isDark ? <Sun className="size-4" /> : <Moon className="size-4" />}
    </button>
  );
}
