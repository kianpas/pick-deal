"use client";

import { useEffect, useRef, useState, useTransition } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import type { ReactNode, RefObject } from "react";
import { countSearchFilters, listLocation } from "@/lib/list-location";
import { ResetSearchFilters } from "@/components/filter/ResetSearchFilters";
import { READ_ONLY } from "@/lib/runtime-config";
import { clearListSnapshot, saveListSnapshot, takeListSnapshot, type ListSnapshot } from "@/lib/list-resume";
import { CategoryGrid } from "./CategoryGrid";
import { DealList } from "./DealList";
import { SortBar } from "./SortBar";
import { getDealCategories, getDeals, type DealListParams } from "@/lib/api";
import { formatRelativeTime } from "@/lib/format";
import type { DealCategory, DealSummary, PageMeta } from "@/lib/api-types";
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
  categories: DealCategory[];
  /** 현재 선택된 카테고리(URL ?category=). */
  activeCategory?: string;
  filters?: ReactNode;
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
 * 보기 설정은 유지하고 필터 변경 시 목록·페이지 상태만 새로 마운트한다.
 * 출처/키워드/카테고리 필터는 백엔드가 서버에서 적용하므로(docs/01 §3.2) 여기서 다시 거르지 않는다.
 */
export function DealFeed(props: Props) {
  const [showThumbnail, setShowThumbnail] = useState(true);
  const [filterPending, startTransition] = useTransition();
  const router = useRouter();
  const searchParams = useSearchParams();
  const endedToggleRef = useRef<HTMLInputElement>(null);
  const restoreToggleFocus = useRef(false);
  useEffect(() => {
    if (!filterPending && restoreToggleFocus.current) {
      endedToggleRef.current?.focus({ preventScroll: true });
      restoreToggleFocus.current = false;
    }
  }, [filterPending]);

  function toggleHideEnded() {
    if (filterPending) return;
    restoreToggleFocus.current = true;
    const next = new URLSearchParams(searchParams);
    if (props.listParams.hideEnded) next.delete("hideEnded");
    else next.set("hideEnded", "true");
    next.delete("page");
    startTransition(() => router.push(next.size ? `/?${next}` : "/", { scroll: false }));
  }
  return (
    <FilteredDealFeed
      key={JSON.stringify(props.listParams)}
      {...props}
      showThumbnail={showThumbnail}
      onToggleThumbnail={() => setShowThumbnail((value) => !value)}
      onRestoreThumbnail={setShowThumbnail}
      filterPending={filterPending}
      onToggleHideEnded={toggleHideEnded}
      endedToggleRef={endedToggleRef}
    />
  );
}

function FilteredDealFeed({ deals, meta, loadFailed, listParams, categories, activeCategory, filters,
  showThumbnail, onToggleThumbnail,
  onRestoreThumbnail,
  filterPending, onToggleHideEnded, endedToggleRef,
}: Props & { showThumbnail: boolean; onToggleThumbnail: () => void; onRestoreThumbnail: (value: boolean) => void; filterPending: boolean; onToggleHideEnded: () => void; endedToggleRef: RefObject<HTMLInputElement | null> }) {
  const searchParams = useSearchParams();
  const listHref = listLocation(`/?${searchParams}`, false);
  const resumeId = searchParams.get("resume");
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
  const [resumeReady, setResumeReady] = useState(false);
  const refreshGeneration = useRef(0);
  const pendingPosition = useRef<ListSnapshot | null>(null);

  useEffect(() => {
    // 라우트 이동이 끝난 프레임에서 같은 탭의 일회성 스냅샷을 복원한다.
    const frame = requestAnimationFrame(() => {
      const saved = takeListSnapshot(listHref, resumeId);
      setResumeReady(true);
      if (!saved || loadFailed) return;
      pendingPosition.current = saved;
      setFirstPageDeals(saved.deals);
      setExtraDeals([]);
      setNextPage(saved.nextPage);
      setHasNext(saved.hasNext);
      onRestoreThumbnail(saved.showThumbnail);
      // 복귀 표식을 현재 이력에서 제거한다. 이후 다른 상품의 브라우저 뒤로 가기와 충돌하지 않는다.
      if (resumeId !== null) window.history.replaceState(window.history.state, "", listHref);
    });
    return () => cancelAnimationFrame(frame);
  }, [listHref, resumeId, loadFailed, onRestoreThumbnail]);

  useEffect(() => {
    const saved = pendingPosition.current;
    if (!saved) return;
    const frame = requestAnimationFrame(() => {
      const card = document.querySelector<HTMLElement>(`[data-deal-id="${saved.selectedDealId}"]`);
      const titleLink = card?.querySelector<HTMLAnchorElement>("[data-detail-title]");
      titleLink?.focus({ preventScroll: true });
      if (window.innerWidth === saved.viewportWidth) window.scrollTo(0, saved.scrollY);
      else card?.scrollIntoView({ block: "start" });
      pendingPosition.current = null;
    });
    return () => cancelAnimationFrame(frame);
  }, [firstPageDeals, showThumbnail]);

  useEffect(() => {
    async function refreshFirstPage() {
      clearListSnapshot();
      pendingPosition.current = null;
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
    if (!resumeReady || loadingMore || refreshing || filterPending) return;
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
  function rememberList(selectedDealId: number) {
    saveListSnapshot({ listHref, deals: allDeals, nextPage, hasNext, selectedDealId,
      scrollY: window.scrollY, viewportWidth: window.innerWidth, showThumbnail });
  }
  const filterCount = countSearchFilters(new URLSearchParams(searchParams));
  const filtered = filterCount > 0;
  const registeredAt = latestRegisteredAt(allDeals);

  return (
    <div className="space-y-4">
      <h1 className="sr-only">핫딜 목록</h1>
      <div className="space-y-3">
        {filters}
        {currentCategories.length > 0 && (
          <CategoryGrid categories={currentCategories} active={activeCategory} />
        )}
      </div>
      <SortBar showThumbnail={showThumbnail} onToggleThumbnail={onToggleThumbnail}
        hideEnded={Boolean(listParams.hideEnded)} onToggleHideEnded={onToggleHideEnded} pending={filterPending} endedToggleRef={endedToggleRef} filterCount={filterCount} />
      <p role="status" className={filterPending ? "text-xs text-fg-muted" : "sr-only"}>{filterPending ? "종료·품절 조건을 적용하고 있어요…" : ""}</p>
      <div aria-busy={filterPending} className={filterPending ? "opacity-60" : ""}>

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
          <p className="text-xs text-fg-muted">잠시 후 다시 시도해 주세요.</p>
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
          <DealList deals={allDeals} showThumbnail={showThumbnail} listHref={listHref} onOpenDetail={rememberList} />
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
                disabled={!resumeReady || loadingMore || refreshing || filterPending}
                className="w-full rounded-xl border border-border bg-surface py-2.5 text-sm font-medium text-fg-muted transition hover:bg-surface-hover hover:text-fg disabled:opacity-50 sm:max-w-xs"
              >
                {loadingMore ? "불러오는 중…" : refreshing ? "목록 갱신 중…" : "더 보기"}
              </button>
            </div>
          )}
          {registeredAt && (
            <p className="text-center text-xs text-fg-subtle" suppressHydrationWarning>
              이 목록의 최근 등록 {formatRelativeTime(registeredAt)}
            </p>
          )}
        </>
      ) : (
        <div className="space-y-3 rounded-xl border border-dashed border-border px-4 py-12 text-center">
          <p className="text-sm text-fg-muted">
            {filtered ? "선택한 검색·필터 조건에 맞는 핫딜이 없어요." : "현재 표시할 핫딜이 없어요."}
          </p>
          {!READ_ONLY && (
            <p className="text-xs leading-relaxed text-fg-muted">
              관심·제외 키워드와 출처 표시 설정도 목록에 적용됩니다.
              <br />키워드 관리와 출처 표시 설정을 확인해 주세요.
            </p>
          )}
          <div className="flex flex-wrap items-center justify-center gap-2">
            {filtered ? <ResetSearchFilters /> : (
              <button type="button" onClick={() => { clearListSnapshot(); window.location.reload(); }}
                className="inline-flex min-h-11 items-center rounded-lg border border-border bg-surface px-4 py-2 text-sm font-medium text-fg-muted transition hover:bg-surface-hover hover:text-fg">
                다시 불러오기
              </button>
            )}
            {!READ_ONLY && (
              <Link href="/settings/keywords" className="inline-flex min-h-11 items-center rounded-lg px-3 py-2 text-sm text-brand">
                키워드 관리
              </Link>
            )}
          </div>
        </div>
      )}
      </div>
    </div>
  );
}
