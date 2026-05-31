"use client";

import { useEffect, useState } from "react";

export type Theme = "light" | "dark";

const STORAGE_KEY = "theme";

function readSystemTheme(): Theme {
  if (typeof window === "undefined") return "light";
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

function readStoredTheme(): Theme | null {
  if (typeof window === "undefined") return null;
  try {
    const v = window.localStorage.getItem(STORAGE_KEY);
    return v === "dark" || v === "light" ? v : null;
  } catch {
    return null;
  }
}

function applyTheme(theme: Theme) {
  const root = document.documentElement;
  root.classList.toggle("dark", theme === "dark");
}

/**
 * 테마 훅.
 * - 초기 값은 layout.tsx의 inline script에서 이미 적용된 상태이므로 mount 후 동기화한다.
 * - 시스템 테마가 변경되고 사용자가 명시적 선택을 안 했다면 자동 추종한다.
 */
export function useTheme() {
  const [theme, setThemeState] = useState<Theme | null>(null);

  // mount: 현재 적용된 테마 동기화
  useEffect(() => {
    const isDark = document.documentElement.classList.contains("dark");
    // layout.tsx 인라인 스크립트가 hydration 전에 DOM에 이미 테마를 적용하므로,
    // mount 시점에 DOM을 읽어 React state를 동기화하는 것은 의도된 패턴.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setThemeState(isDark ? "dark" : "light");

    // 사용자 선택이 없을 때만 시스템 추종
    const mq = window.matchMedia("(prefers-color-scheme: dark)");
    const handler = (e: MediaQueryListEvent) => {
      if (readStoredTheme() === null) {
        const next: Theme = e.matches ? "dark" : "light";
        applyTheme(next);
        setThemeState(next);
      }
    };
    mq.addEventListener("change", handler);
    return () => mq.removeEventListener("change", handler);
  }, []);

  const setTheme = (next: Theme) => {
    applyTheme(next);
    setThemeState(next);
    try {
      window.localStorage.setItem(STORAGE_KEY, next);
    } catch {
      /* storage 차단 환경 무시 */
    }
  };

  const toggle = () => setTheme(theme === "dark" ? "light" : "dark");

  return { theme, setTheme, toggle };
}

export const themeInitScript = `
(function() {
  try {
    var stored = localStorage.getItem('${STORAGE_KEY}');
    var system = window.matchMedia('(prefers-color-scheme: dark)').matches;
    var dark = stored ? stored === 'dark' : system;
    if (dark) document.documentElement.classList.add('dark');
  } catch (_) {}
})();
`;

export { readSystemTheme, readStoredTheme };
