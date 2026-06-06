"use client";

import { useEffect, useState } from "react";

export type Theme = "light" | "dark" | "cassette";

const STORAGE_KEY = "theme";
const THEMES: Theme[] = ["light", "dark", "cassette"];

function isTheme(v: unknown): v is Theme {
  return v === "light" || v === "dark" || v === "cassette";
}

function readSystemTheme(): Theme {
  if (typeof window === "undefined") return "light";
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

function readStoredTheme(): Theme | null {
  if (typeof window === "undefined") return null;
  try {
    const v = window.localStorage.getItem(STORAGE_KEY);
    return isTheme(v) ? v : null;
  } catch {
    return null;
  }
}

function readAppliedTheme(): Theme {
  const root = document.documentElement;
  if (root.classList.contains("cassette")) return "cassette";
  if (root.classList.contains("dark")) return "dark";
  return "light";
}

function applyTheme(theme: Theme) {
  // 라이트는 클래스 없음(기본 토큰), 나머지는 해당 클래스 하나만 켠다.
  const root = document.documentElement;
  root.classList.toggle("dark", theme === "dark");
  root.classList.toggle("cassette", theme === "cassette");
}

/**
 * 테마 훅. light → dark → cassette → light 순으로 순환한다.
 * - 초기 값은 layout.tsx의 inline script에서 이미 적용된 상태이므로 mount 후 동기화한다.
 * - 시스템 테마 변경은 사용자가 명시적 선택을 안 했을 때만 추종한다(light/dark만).
 */
export function useTheme() {
  const [theme, setThemeState] = useState<Theme | null>(null);

  // mount: 현재 적용된 테마 동기화
  useEffect(() => {
    // layout.tsx 인라인 스크립트가 hydration 전에 DOM에 이미 테마를 적용하므로,
    // mount 시점에 DOM을 읽어 React state를 동기화하는 것은 의도된 패턴.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setThemeState(readAppliedTheme());

    // 사용자 선택이 없을 때만 시스템 추종(다크/라이트)
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

  /** 다음 테마로 순환. */
  const toggle = () => {
    const current = theme ?? readAppliedTheme();
    const next = THEMES[(THEMES.indexOf(current) + 1) % THEMES.length];
    setTheme(next);
  };

  return { theme, setTheme, toggle };
}

export const themeInitScript = `
(function() {
  try {
    var stored = localStorage.getItem('${STORAGE_KEY}');
    var root = document.documentElement;
    if (stored === 'cassette') { root.classList.add('cassette'); return; }
    if (stored === 'dark') { root.classList.add('dark'); return; }
    if (stored === 'light') return;
    if (window.matchMedia('(prefers-color-scheme: dark)').matches) root.classList.add('dark');
  } catch (_) {}
})();
`;

export { readSystemTheme, readStoredTheme };
