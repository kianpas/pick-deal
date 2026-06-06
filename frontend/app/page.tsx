import { LeftSidebar } from "@/components/layout/LeftSidebar";
import { RightSidebar } from "@/components/layout/RightSidebar";
import { TopBar } from "@/components/layout/TopBar";
import { DealFeed } from "@/components/deal/DealFeed";
import { FilterProvider } from "@/components/filter/FilterProvider";
import { getDeals } from "@/lib/api";
import type { DealSummary } from "@/lib/api-types";

export default async function Home() {
  let deals: DealSummary[] = [];
  try {
    deals = (await getDeals({ size: 50 })).items;
  } catch (error) {
    // 백엔드 미기동 등으로 실패하면 빈 목록으로 렌더(화면은 빈 상태 표시).
    console.error("딜 목록을 불러오지 못했습니다:", error);
  }

  return (
    <div className="min-h-screen bg-bg text-fg">
      <TopBar />
      <FilterProvider>
        <div className="flex">
          <LeftSidebar />

          <main className="min-w-0 flex-1 px-3 py-4 sm:px-5 sm:py-5">
            <DealFeed deals={deals} />
          </main>

          <RightSidebar />
        </div>
      </FilterProvider>
    </div>
  );
}
