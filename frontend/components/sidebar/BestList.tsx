import { MessageCircle } from "lucide-react";
import { BEST_LIST } from "@/lib/mock-data";

export function BestList() {
  return (
    <section className="rounded-xl border border-border bg-surface/60 p-4">
      <header className="mb-3 flex items-center justify-between">
        <h3 className="text-sm font-semibold">핫딜 BEST</h3>
        <button
          type="button"
          className="text-[11px] text-fg-subtle hover:text-fg transition"
        >
          전체 보기
        </button>
      </header>

      <ul className="space-y-1.5">
        {BEST_LIST.map((item) => (
          <li
            key={item.rank}
            className="flex items-center gap-2.5 rounded px-1 py-1 text-xs hover:bg-surface-hover transition"
          >
            <span
              className={`grid size-4 shrink-0 place-items-center rounded text-[10px] font-medium tabular-nums ${
                item.rank <= 3
                  ? "bg-brand-soft text-brand"
                  : "bg-surface-2 text-fg-subtle"
              }`}
            >
              {item.rank}
            </span>
            <span className="flex-1 truncate text-fg-muted">
              <span className="text-fg-subtle">[{item.shopName}]</span> {item.title}
            </span>
            <span className="inline-flex items-center gap-0.5 text-fg-subtle tabular-nums">
              <MessageCircle className="size-3" />
              {item.commentCount}
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}
