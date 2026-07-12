"use client";

import { useState } from "react";
import { CategoryGrid } from "./CategoryGrid";
import { DealList } from "./DealList";
import { SortBar } from "./SortBar";
import { getDeals, type DealListParams } from "@/lib/api";
import type { DealSummary, PageMeta } from "@/lib/api-types";

interface Props {
  /** 첫 페이지 딜 목록(서버에서 fetch). */
  deals: DealSummary[];
  /** 첫 페이지의 페이지 메타. 백엔드 실패 시 null. */
  meta: PageMeta | null;
  /** "더 보기"가 다음 페이지를 요청할 때 쓰는 목록 파라미터(page 제외). */
  listParams: DealListParams;
  /** 백엔드 실데이터 카테고리 목록(빈 배열이면 카테고리 바를 숨긴다). */
  categories: string[];
  /** 현재 선택된 카테고리(URL ?category=). */
  activeCategory?: string;
}

/**
 * 딜 목록 영역. 첫 페이지는 서버(page.tsx)가 내려주고,
 * "더 보기"는 같은 필터 조건으로 다음 페이지를 클라이언트에서 이어 붙인다.
 * 필터 변경 시에는 page.tsx가 key를 바꿔 이 컴포넌트를 새로 마운트한다.
 * 출처/키워드/카테고리 필터는 백엔드가 서버에서 적용하므로(docs/01 §3.2) 여기서 다시 거르지 않는다.
 */
export function DealFeed({ deals, meta, listParams, categories, activeCategory }: Props) {
  const [showThumbnail, setShowThumbnail] = useState(true);
  const [extraDeals, setExtraDeals] = useState<DealSummary[]>([]);
  const [nextPage, setNextPage] = useState(1);
  const [hasNext, setHasNext] = useState(meta?.hasNext ?? false);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState(false);

  async function loadMore() {
    setLoading(true);
    setLoadError(false);
    try {
      const result = await getDeals({ ...listParams, page: nextPage });
      setExtraDeals((prev) => [...prev, ...result.items]);
      setNextPage((page) => page + 1);
      setHasNext(result.meta.hasNext);
    } catch {
      setLoadError(true);
    } finally {
      setLoading(false);
    }
  }

  const allDeals = extraDeals.length > 0 ? [...deals, ...extraDeals] : deals;

  return (
    <div className="space-y-4">
      <SortBar
        showThumbnail={showThumbnail}
        onToggleThumbnail={() => setShowThumbnail((v) => !v)}
      />
      {categories.length > 0 && (
        <CategoryGrid categories={categories} active={activeCategory} />
      )}
      {allDeals.length > 0 ? (
        <>
          <DealList deals={allDeals} showThumbnail={showThumbnail} />
          {hasNext && (
            <div className="flex flex-col items-center gap-2 pt-1">
              {loadError && (
                <p className="text-xs text-danger">
                  목록을 더 불러오지 못했어요. 다시 시도해 주세요.
                </p>
              )}
              <button
                type="button"
                onClick={loadMore}
                disabled={loading}
                className="w-full rounded-xl border border-border bg-surface py-2.5 text-sm font-medium text-fg-muted transition hover:bg-surface-hover hover:text-fg disabled:opacity-50 sm:max-w-xs"
              >
                {loading ? "불러오는 중…" : "더 보기"}
              </button>
            </div>
          )}
        </>
      ) : (
        <div className="rounded-xl border border-dashed border-border py-12 text-center text-sm text-fg-muted">
          표시할 핫딜이 없어요.
        </div>
      )}
    </div>
  );
}
