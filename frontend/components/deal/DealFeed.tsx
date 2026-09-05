"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { CategoryGrid } from "./CategoryGrid";
import { DealList } from "./DealList";
import { SortBar } from "./SortBar";
import { getDealCategories, getDeals, type DealListParams } from "@/lib/api";
import { formatRelativeTime } from "@/lib/format";
import type { DealSummary, PageMeta } from "@/lib/api-types";
import { SOURCE_VISIBILITY_CHANGED_EVENT } from "@/lib/ui-events";

interface Props {
  /** 첫 페이지 딜 목록(서버에서 fetch). */
  deals: DealSummary[];
  /** 첫 페이지의 페이지 메타. 백엔드 실패 시 null. */
  meta: PageMeta | null;
  /** 첫 페이지 fetch 실패 여부 — 빈 목록과 장애를 구분해 보여준다. */
  loadFailed: boolean;
  /** "더 보기"가 다음 페이지를 요청할 때 쓰는 목록 파라미터(page 제외). */
  listParams: DealListParams;
  /** 백엔드 실데이터 카테고리 목록(빈 배열이면 카테고리 바를 숨긴다). */
  categories: string[];
  /** 현재 선택된 카테고리(URL ?category=). */
  activeCategory?: string;
}

/** 목록에서 가장 최근에 처음 등록된 딜의 시각. 딜이 없으면 null. */
function latestRegisteredAt(deals: DealSummary[]): string | null {
  let max: string | null = null;
  for (const deal of deals) {
    if (deal.collectedAt && (max === null || deal.collectedAt > max)) {
      max = deal.collectedAt;
    }
  }
  return max;
}

/**
 * 딜 목록 영역. 첫 페이지는 서버(page.tsx)가 내려주고,
 * "더 보기"는 같은 필터 조건으로 다음 페이지를 클라이언트에서 이어 붙인다.
 * 필터 변경 시에는 page.tsx가 key를 바꿔 이 컴포넌트를 새로 마운트한다.
 * 출처/키워드/카테고리 필터는 백엔드가 서버에서 적용하므로(docs/01 §3.2) 여기서 다시 거르지 않는다.
 */
export function DealFeed({ deals, meta, loadFailed, listParams, categories, activeCategory }: Props) {
  const [showThumbnail, setShowThumbnail] = useState(true);
  const [firstPageDeals, setFirstPageDeals] = useState(deals);
  const [currentCategories, setCurrentCategories] = useState(categories);
  const [currentLoadFailed, setCurrentLoadFailed] = useState(loadFailed);
  const [extraDeals, setExtraDeals] = useState<DealSummary[]>([]);
  const [nextPage, setNextPage] = useState(1);
  const [hasNext, setHasNext] = useState(meta?.hasNext ?? false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [loadError, setLoadError] = useState(false);
  const [refreshError, setRefreshError] = useState(false);
  const refreshGeneration = useRef(0);

  useEffect(() => {
    async function refreshFirstPage() {
      const generation = ++refreshGeneration.current;
      setRefreshing(true);
      setLoadingMore(false);
      setRefreshError(false);
      setLoadError(false);
      setFirstPageDeals([]);
      setExtraDeals([]);
      setNextPage(1);
      setHasNext(false);

      try {
        const [dealsResult, categoryResult] = await Promise.all([
          getDeals({ ...listParams, page: 0 }),
          getDealCategories(),
        ]);
        if (generation !== refreshGeneration.current) return;

        setFirstPageDeals(dealsResult.items);
        setCurrentCategories(categoryResult);
        setCurrentLoadFailed(false);
        setHasNext(dealsResult.meta.hasNext);
      } catch {
        if (generation === refreshGeneration.current) setRefreshError(true);
      } finally {
        if (generation === refreshGeneration.current) setRefreshing(false);
      }
    }

    window.addEventListener(SOURCE_VISIBILITY_CHANGED_EVENT, refreshFirstPage);
    return () => {
      refreshGeneration.current += 1;
      window.removeEventListener(SOURCE_VISIBILITY_CHANGED_EVENT, refreshFirstPage);
    };
  }, [listParams]);

  async function loadMore() {
    const generation = refreshGeneration.current;
    setLoadingMore(true);
    setLoadError(false);
    try {
      const result = await getDeals({ ...listParams, page: nextPage });
      if (generation !== refreshGeneration.current) return;
      setExtraDeals((prev) => [...prev, ...result.items]);
      setNextPage((page) => page + 1);
      setHasNext(result.meta.hasNext);
    } catch {
      if (generation === refreshGeneration.current) setLoadError(true);
    } finally {
      if (generation === refreshGeneration.current) setLoadingMore(false);
    }
  }

  const allDeals = extraDeals.length > 0 ? [...firstPageDeals, ...extraDeals] : firstPageDeals;
  const filtered = Boolean(listParams.q || listParams.category);
  const registeredAt = latestRegisteredAt(allDeals);

  return (
    <div className="space-y-4">
      <SortBar
        showThumbnail={showThumbnail}
        onToggleThumbnail={() => setShowThumbnail((v) => !v)}
      />
      {currentCategories.length > 0 && (
        <CategoryGrid categories={currentCategories} active={activeCategory} />
      )}

      {refreshing ? (
        <div className="rounded-xl border border-dashed border-border py-12 text-center text-sm text-fg-muted">
          출처 설정을 반영하고 있어요.
        </div>
      ) : refreshError ? (
        <div className="space-y-3 rounded-xl border border-dashed border-danger/40 py-12 text-center">
          <p className="text-sm text-danger">출처 설정은 저장했지만 목록을 새로 불러오지 못했어요.</p>
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="inline-flex rounded-lg border border-border bg-surface px-4 py-2 text-sm font-medium text-fg-muted transition hover:bg-surface-hover hover:text-fg"
          >
            다시 불러오기
          </button>
        </div>
      ) : currentLoadFailed ? (
        <div className="space-y-3 rounded-xl border border-dashed border-danger/40 py-12 text-center">
          <p className="text-sm text-danger">딜 목록을 불러오지 못했어요.</p>
          <p className="text-xs text-fg-muted">백엔드 서버가 실행 중인지 확인한 뒤 다시 시도해 주세요.</p>
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="inline-flex rounded-lg border border-border bg-surface px-4 py-2 text-sm font-medium text-fg-muted transition hover:bg-surface-hover hover:text-fg"
          >
            다시 시도
          </button>
        </div>
      ) : allDeals.length > 0 ? (
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
                disabled={loadingMore || refreshing}
                className="w-full rounded-xl border border-border bg-surface py-2.5 text-sm font-medium text-fg-muted transition hover:bg-surface-hover hover:text-fg disabled:opacity-50 sm:max-w-xs"
              >
                {loadingMore ? "불러오는 중…" : refreshing ? "목록 갱신 중…" : "더 보기"}
              </button>
            </div>
          )}
          {registeredAt && (
            <p className="text-center text-xs text-fg-subtle" suppressHydrationWarning>
              최근 등록 {formatRelativeTime(registeredAt)}
            </p>
          )}
        </>
      ) : filtered ? (
        <div className="space-y-3 rounded-xl border border-dashed border-border py-12 text-center">
          <p className="text-sm text-fg-muted">조건에 맞는 핫딜이 없어요.</p>
          <Link
            href="/"
            className="inline-flex rounded-lg border border-border bg-surface px-4 py-2 text-sm font-medium text-fg-muted transition hover:bg-surface-hover hover:text-fg"
          >
            필터 해제
          </Link>
        </div>
      ) : (
        <div className="rounded-xl border border-dashed border-border py-12 text-center text-sm text-fg-muted">
          아직 수집된 핫딜이 없어요. 수집기가 곧 채워줄 거예요.
        </div>
      )}
    </div>
  );
}
