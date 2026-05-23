import { KEYWORD_TAGS } from "@/lib/mock-data";

export function KeywordCloud() {
  return (
    <section className="rounded-xl border border-border bg-surface/60 p-4">
      <header className="mb-3 flex items-center justify-between">
        <h3 className="text-sm font-semibold">인기 키워드</h3>
        <button
          type="button"
          className="text-[11px] text-fg-subtle hover:text-fg transition"
        >
          전체 보기
        </button>
      </header>

      <div className="flex flex-wrap gap-1.5">
        {KEYWORD_TAGS.map((k) => (
          <button
            key={k.keyword}
            type="button"
            className="inline-flex items-center gap-1 rounded-full border border-border bg-surface-2 px-2.5 py-1 text-xs text-fg-muted hover:border-brand/50 hover:text-fg transition"
          >
            <span className="text-fg-subtle"># </span>
            <span>{k.keyword}</span>
            <span className="text-fg-subtle tabular-nums">{k.count}</span>
          </button>
        ))}
      </div>
    </section>
  );
}
