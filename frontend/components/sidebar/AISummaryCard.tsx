import { ArrowRight, Bot } from "lucide-react";
import { AI_SUMMARY } from "@/lib/mock-data";

export function AISummaryCard() {
  return (
    <section className="rounded-xl border border-border bg-surface/60 p-4">
      <header className="mb-3 flex items-center gap-2">
        <h3 className="text-sm font-semibold">AI 핫딜 요약</h3>
        <span className="rounded-md border border-brand/30 bg-brand-soft px-1.5 py-0.5 text-[10px] font-medium text-brand">
          Beta
        </span>
      </header>

      <div className="flex gap-3">
        <span className="grid size-10 shrink-0 place-items-center rounded-full bg-brand-soft text-brand">
          <Bot className="size-5" />
        </span>
        <p className="text-sm leading-relaxed text-fg-muted">
          {AI_SUMMARY.body}{" "}
          {AI_SUMMARY.highlights.map((kw, i) => (
            <span key={kw}>
              <span className="font-semibold text-fg">&ldquo;{kw}&rdquo;</span>
              {i < AI_SUMMARY.highlights.length - 1 ? ", " : " "}
            </span>
          ))}
          에요!
        </p>
      </div>

      <button
        type="button"
        className="mt-3 flex w-full items-center justify-center gap-1 rounded-lg border border-border bg-surface-2 px-3 py-2 text-xs text-fg-muted hover:text-fg transition"
      >
        더 많은 인사이트 보기 <ArrowRight className="size-3" />
      </button>
    </section>
  );
}
