import { LeftSidebar } from "@/components/layout/LeftSidebar";
import { RightSidebar } from "@/components/layout/RightSidebar";
import { TopBar } from "@/components/layout/TopBar";
import { DealFeed } from "@/components/deal/DealFeed";
import { FilterProvider } from "@/components/filter/FilterProvider";
import { DEALS } from "@/lib/mock-data";

export default function Home() {
  return (
    <div className="min-h-screen bg-bg text-fg">
      <TopBar />
      <FilterProvider>
        <div className="flex">
          <LeftSidebar />

          <main className="min-w-0 flex-1 px-3 py-4 sm:px-5 sm:py-5">
            <DealFeed deals={DEALS} />
          </main>

          <RightSidebar />
        </div>
      </FilterProvider>
    </div>
  );
}
