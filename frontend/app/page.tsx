import { DealFeed } from "@/components/deal/DealFeed";
import { AppShell } from "@/components/layout/AppShell";
import { getDeals } from "@/lib/api";
import type { DealSummary } from "@/lib/api-types";

/**
 * 홈(딜 목록). 정렬/검색 상태는 URL 쿼리(?sort=, ?q=)가 SSOT다 —
 * SortBar/검색창은 URL만 바꾸고, 여기서 읽어 백엔드 SSR fetch에 반영한다.
 */
export default async function Home({
  searchParams,
}: {
  searchParams: Promise<{ sort?: string; q?: string }>;
}) {
  const { sort, q } = await searchParams;
  // 허용된 정렬 값만 통과시킨다(그 외/누락은 백엔드 기본인 최신순).
  const sortOption = sort === "discount" ? "discount" : undefined;

  let deals: DealSummary[] = [];
  try {
    deals = (await getDeals({ size: 50, sort: sortOption, q })).items;
  } catch (error) {
    // 백엔드 미기동 등으로 실패하면 빈 목록으로 렌더(화면은 빈 상태 표시).
    console.error("딜 목록을 불러오지 못했습니다:", error);
  }

  return (
    <AppShell>
      <DealFeed deals={deals} />
    </AppShell>
  );
}
