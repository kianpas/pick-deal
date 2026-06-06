"use client";

import { Disc3, Moon, Sun } from "lucide-react";
import { useTheme, type Theme } from "@/lib/theme";

interface Props {
  /** "icon": 아이콘만 (헤더용) | "row": 사이드바형 가로 행 */
  variant?: "icon" | "row";
}

/**
 * 각 테마가 표시할 아이콘과 "다음으로 전환" 라벨.
 * 클릭하면 light → dark → cassette → light 순으로 순환한다.
 */
const THEME_UI: Record<Theme, { icon: typeof Sun; nextLabel: string }> = {
  light: { icon: Sun, nextLabel: "다크 모드" },
  dark: { icon: Moon, nextLabel: "카세트 모드" },
  cassette: { icon: Disc3, nextLabel: "라이트 모드" },
};

export function ThemeToggle({ variant = "icon" }: Props) {
  const { theme, toggle } = useTheme();

  if (theme === null) {
    return variant === "icon" ? (
      <div className="size-9" aria-hidden />
    ) : (
      <div className="h-9" aria-hidden />
    );
  }

  const { icon: Icon, nextLabel } = THEME_UI[theme];

  if (variant === "row") {
    return (
      <button
        type="button"
        onClick={toggle}
        className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-sm text-fg-muted hover:bg-surface hover:text-fg transition"
        aria-label="테마 전환"
      >
        <Icon className="size-4" />
        <span>{nextLabel}</span>
      </button>
    );
  }

  return (
    <button
      type="button"
      onClick={toggle}
      className="grid size-9 place-items-center rounded-lg border border-border bg-surface text-fg-muted hover:bg-surface-hover hover:text-fg transition"
      aria-label={`${nextLabel}로 전환`}
    >
      <Icon className="size-4" />
    </button>
  );
}
