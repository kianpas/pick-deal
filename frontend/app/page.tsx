import { LeftSidebar } from "@/components/layout/LeftSidebar";
import { RightSidebar } from "@/components/layout/RightSidebar";
import { TopBar } from "@/components/layout/TopBar";
import { CategoryGrid } from "@/components/deal/CategoryGrid";
import { DealList } from "@/components/deal/DealList";
import { SortBar } from "@/components/deal/SortBar";
import { DEALS } from "@/lib/mock-data";

export default function Home() {
  return (
    <div className="min-h-screen bg-bg text-fg">
      <TopBar />
      <div className="flex">
        <LeftSidebar />

        <main className="min-w-0 flex-1 px-3 py-4 sm:px-5 sm:py-5">
          <div className="space-y-4">
            <SortBar />
            <CategoryGrid />
            <DealList deals={DEALS} />
            <div className="py-6 text-center text-sm text-fg-muted">
              더 많은 핫딜 보기 ↓
            </div>
          </div>
        </main>

        <RightSidebar />
      </div>
    </div>
  );
}
