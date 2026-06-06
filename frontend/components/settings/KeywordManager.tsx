"use client";

import { Plus, X } from "lucide-react";
import { useState } from "react";
import { ApiError, createKeyword, deleteKeyword } from "@/lib/api";
import type { KeywordItem, KeywordType } from "@/lib/api-types";

/**
 * 관심/제외 키워드 관리(추가·삭제). 백엔드 DB가 SSOT다.
 * 초기 목록은 서버(page.tsx)에서 받아 props로 주입하고, 이후 변경은 API 호출 + 로컬 상태로 반영한다.
 *
 * - 관심(INTEREST): 하나 이상 등록되면 이를 포함하는 딜만 노출
 * - 제외(EXCLUDE): 포함되면 딜을 숨김 (docs/01 §3.2)
 */
export function KeywordManager({ initialKeywords }: { initialKeywords: KeywordItem[] }) {
  const [keywords, setKeywords] = useState<KeywordItem[]>(initialKeywords);
  const [input, setInput] = useState("");
  const [type, setType] = useState<KeywordType>("INTEREST");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const interest = keywords.filter((k) => k.type === "INTEREST");
  const exclude = keywords.filter((k) => k.type === "EXCLUDE");

  async function handleAdd(e: React.FormEvent) {
    e.preventDefault();
    const keyword = input.trim();
    if (!keyword || busy) return;

    setBusy(true);
    setError(null);
    try {
      const created = await createKeyword({ keyword, type });
      setKeywords((prev) => [...prev, created]);
      setInput("");
    } catch (err) {
      setError(
        err instanceof ApiError ? err.message : "키워드를 추가하지 못했습니다.",
      );
    } finally {
      setBusy(false);
    }
  }

  async function handleDelete(id: number) {
    // 낙관적 제거(실패 시 롤백)
    const removed = keywords.find((k) => k.id === id);
    setKeywords((prev) => prev.filter((k) => k.id !== id));
    try {
      await deleteKeyword(id);
    } catch {
      if (removed) setKeywords((prev) => [...prev, removed]);
      setError("키워드를 삭제하지 못했습니다.");
    }
  }

  return (
    <div className="space-y-6">
      {/* 추가 폼 */}
      <form onSubmit={handleAdd} className="space-y-2">
        <div className="flex gap-2">
          {/* 타입 선택 (관심/제외) */}
          <div className="inline-flex shrink-0 rounded-lg border border-border bg-surface p-0.5">
            {(["INTEREST", "EXCLUDE"] as const).map((t) => (
              <button
                key={t}
                type="button"
                onClick={() => setType(t)}
                aria-pressed={type === t}
                className={`rounded-md px-3 py-1.5 text-sm font-medium transition ${
                  type === t
                    ? t === "INTEREST"
                      ? "bg-brand text-white"
                      : "bg-danger text-white"
                    : "text-fg-muted hover:text-fg"
                }`}
              >
                {t === "INTEREST" ? "관심" : "제외"}
              </button>
            ))}
          </div>

          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder={type === "INTEREST" ? "포함할 키워드" : "제외할 키워드"}
            className="min-w-0 flex-1 rounded-lg border border-border bg-surface px-3 py-2 text-sm outline-none focus:border-border-strong"
          />

          <button
            type="submit"
            disabled={busy || input.trim() === ""}
            className="inline-flex shrink-0 items-center gap-1 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white transition hover:bg-brand-strong disabled:opacity-50"
          >
            <Plus className="size-4" />
            추가
          </button>
        </div>

        {error && <p className="text-xs text-danger">{error}</p>}
      </form>

      {/* 관심 키워드 */}
      <KeywordSection
        title="관심 키워드"
        hint="하나 이상 등록하면 이 단어가 포함된 딜만 보여요."
        items={interest}
        accent="brand"
        onDelete={handleDelete}
      />

      {/* 제외 키워드 */}
      <KeywordSection
        title="제외 키워드"
        hint="이 단어가 포함된 딜은 목록에서 숨겨요."
        items={exclude}
        accent="danger"
        onDelete={handleDelete}
      />
    </div>
  );
}

function KeywordSection({
  title,
  hint,
  items,
  accent,
  onDelete,
}: {
  title: string;
  hint: string;
  items: KeywordItem[];
  accent: "brand" | "danger";
  onDelete: (id: number) => void;
}) {
  const chipClass =
    accent === "brand"
      ? "border-brand/30 bg-brand-soft text-brand"
      : "border-danger/30 bg-danger-soft text-danger";

  return (
    <section className="space-y-2">
      <div>
        <h2 className="text-sm font-semibold text-fg">{title}</h2>
        <p className="text-xs text-fg-subtle">{hint}</p>
      </div>

      {items.length === 0 ? (
        <p className="text-sm text-fg-subtle">등록된 키워드가 없습니다.</p>
      ) : (
        <ul className="flex flex-wrap gap-2">
          {items.map((k) => (
            <li key={k.id}>
              <span
                className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-sm ${chipClass}`}
              >
                {k.keyword}
                <button
                  type="button"
                  onClick={() => onDelete(k.id)}
                  aria-label={`${k.keyword} 삭제`}
                  className="grid size-4 place-items-center rounded-full transition hover:bg-black/10"
                >
                  <X className="size-3" />
                </button>
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
