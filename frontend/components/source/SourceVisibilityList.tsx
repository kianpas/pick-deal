"use client";

import { Eye, EyeOff } from "lucide-react";
import { useEffect, useState } from "react";
import { getSources, updateSourceVisibility } from "@/lib/api";
import type { SourceItem } from "@/lib/api-types";
import {
  SOURCE_VISIBILITY_CHANGED_EVENT,
  type SourceVisibilityChangedDetail,
} from "@/lib/ui-events";

export function SourceVisibilityList() {
  const [sources, setSources] = useState<SourceItem[] | null>(null);
  const [sourcesError, setSourcesError] = useState(false);
  const [pendingSourceId, setPendingSourceId] = useState<number | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    getSources()
      .then((data) => {
        if (active) setSources(data);
      })
      .catch(() => {
        if (active) setSourcesError(true);
      });
    return () => {
      active = false;
    };
  }, [reloadKey]);

  useEffect(() => {
    function reflectVisibilityChange(event: Event) {
      const { sourceId, visible } = (
        event as CustomEvent<SourceVisibilityChangedDetail>
      ).detail;
      setSources((previous) =>
        previous?.map((item) =>
          item.id === sourceId ? { ...item, visible } : item,
        ) ?? previous,
      );
    }

    window.addEventListener(SOURCE_VISIBILITY_CHANGED_EVENT, reflectVisibilityChange);
    return () =>
      window.removeEventListener(SOURCE_VISIBILITY_CHANGED_EVENT, reflectVisibilityChange);
  }, []);

  function retry() {
    setSources(null);
    setSourcesError(false);
    setReloadKey((key) => key + 1);
  }

  async function handleToggleSource(source: SourceItem) {
    const nextVisible = !source.visible;
    setSources((previous) =>
      previous?.map((item) =>
        item.id === source.id ? { ...item, visible: nextVisible } : item,
      ) ?? previous,
    );
    setPendingSourceId(source.id);

    try {
      await updateSourceVisibility(source.id, nextVisible);
      window.dispatchEvent(
        new CustomEvent<SourceVisibilityChangedDetail>(SOURCE_VISIBILITY_CHANGED_EVENT, {
          detail: { sourceId: source.id, visible: nextVisible },
        }),
      );
    } catch {
      setSources((previous) =>
        previous?.map((item) =>
          item.id === source.id ? { ...item, visible: source.visible } : item,
        ) ?? previous,
      );
    } finally {
      setPendingSourceId(null);
    }
  }

  if (sourcesError) {
    return (
      <div className="space-y-1 py-1.5">
        <p className="text-xs text-danger">출처를 불러오지 못했어요.</p>
        <button
          type="button"
          onClick={retry}
          className="text-xs text-fg-muted underline transition hover:text-fg"
        >
          다시 시도
        </button>
      </div>
    );
  }

  if (sources === null) {
    return <p className="py-1.5 text-xs text-fg-subtle">불러오는 중…</p>;
  }

  if (sources.length === 0) {
    return <p className="py-1.5 text-xs text-fg-subtle">출처가 없습니다.</p>;
  }

  return (
    <ul className="space-y-0.5">
      {sources.map((source) => (
        <li key={source.id}>
          <button
            type="button"
            onClick={() => handleToggleSource(source)}
            disabled={pendingSourceId !== null}
            aria-pressed={source.visible}
            className={`flex w-full items-center gap-3 rounded-lg px-3 py-1.5 text-sm transition hover:bg-surface disabled:opacity-50 ${
              source.visible ? "text-fg" : "text-fg-subtle"
            }`}
          >
            <span className="flex-1 truncate text-left">{source.name}</span>
            {source.visible ? (
              <Eye className="size-3.5 text-brand" />
            ) : (
              <EyeOff className="size-3.5 text-fg-subtle" />
            )}
          </button>
        </li>
      ))}
    </ul>
  );
}
