import { DealFeed } from "@/components/deal/DealFeed";
import { AppShell } from "@/components/layout/AppShell";
import { getDealCategories, getDeals, type DealListParams } from "@/lib/api";
import type { DealSummary, PageMeta } from "@/lib/api-types";

const PAGE_SIZE = 20;

/**
 * 홈(딜 목록). 정렬/검색/카테고리 상태는 URL 쿼리(?sort=, ?q=, ?category=)가 SSOT다 —
 * SortBar/검색창/CategoryGrid는 URL만 바꾸고, 여기서 읽어 백엔드 SSR fetch에 반영한다.
 * 첫 페이지만 SSR로 내려주고, 이후 페이지는 DealFeed의 "더 보기"가 이어 붙인다.
 */
export default async function Home({
  searchParams,
}: {
  searchParams: Promise<{ sort?: string; q?: string; category?: string }>;
}) {
  const { sort, q, category } = await searchParams;
  // 허용된 정렬 값만 통과시킨다(그 외/누락은 백엔드 기본인 최신순).
  const sortOption = sort === "discount" ? ("discount" as const) : undefined;
  const listParams: DealListParams = { size: PAGE_SIZE, sort: sortOption, q, category };

  let deals: DealSummary[] = [];
  let meta: PageMeta | null = null;
  let categories: string[] = [];
  let loadFailed = false;
  try {
    const [dealsResult, categoriesResult] = await Promise.all([
      getDeals(listParams),
      getDealCategories(),
    ]);
    deals = dealsResult.items;
    meta = dealsResult.meta;
    categories = categoriesResult;
  } catch (error) {
    // 백엔드 미기동 등 — 빈 데이터와 구분해 에러 상태로 렌더한다.
    loadFailed = true;
    console.error("딜 목록을 불러오지 못했습니다:", error);
  }

  return (
    <AppShell>
      <DealFeed
        // 필터가 바뀌면 "더 보기"로 쌓인 상태를 버리고 새로 시작한다
        key={`${sortOption ?? "latest"}|${q ?? ""}|${category ?? ""}`}
        deals={deals}
        meta={meta}
        loadFailed={loadFailed}
        listParams={listParams}
        categories={categories}
        activeCategory={category}
      />
    </AppShell>
  );
}
