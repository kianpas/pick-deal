"use client";

import {
  Book,
  BookOpen,
  ChevronDown,
  Dumbbell,
  Grid3x3,
  Monitor,
  Shirt,
  Smartphone,
  Soup,
  Tv,
  Utensils,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { useState } from "react";
import { CATEGORIES } from "@/lib/mock-data";
import type { CategoryId } from "@/lib/types";

const ICONS: Record<string, LucideIcon> = {
  grid: Grid3x3,
  tv: Tv,
  smartphone: Smartphone,
  monitor: Monitor,
  utensils: Utensils,
  soup: Soup,
  shirt: Shirt,
  dumbbell: Dumbbell,
  book: Book,
};

export function CategoryGrid() {
  const [active, setActive] = useState<CategoryId>("all");

  return (
    <div className="flex items-end gap-2 overflow-x-auto scrollbar-hide border-b border-border">
      {CATEGORIES.map((c) => {
        const Icon = ICONS[c.icon] ?? BookOpen;
        const isActive = c.id === active;
        return (
          <button
            key={c.id}
            type="button"
            onClick={() => setActive(c.id)}
            className={`group flex shrink-0 flex-col items-center gap-1.5 px-3 pb-2.5 pt-3 transition ${
              isActive ? "text-brand" : "text-fg-muted hover:text-fg"
            }`}
          >
            <span
              className={`grid size-10 place-items-center rounded-full transition ${
                isActive
                  ? "bg-brand-soft"
                  : "bg-surface group-hover:bg-surface-hover"
              }`}
            >
              <Icon className="size-5" />
            </span>
            <span className="text-xs font-medium">{c.name}</span>
            <span
              className={`h-0.5 w-full rounded-full transition ${
                isActive ? "bg-brand" : "bg-transparent"
              }`}
            />
          </button>
        );
      })}

      <button
        type="button"
        className="group flex shrink-0 flex-col items-center gap-1.5 px-3 pb-2.5 pt-3 text-fg-muted hover:text-fg transition"
      >
        <span className="grid size-10 place-items-center rounded-full bg-surface group-hover:bg-surface-hover transition">
          <ChevronDown className="size-5" />
        </span>
        <span className="text-xs font-medium">더보기</span>
        <span className="h-0.5 w-full bg-transparent" />
      </button>
    </div>
  );
}
