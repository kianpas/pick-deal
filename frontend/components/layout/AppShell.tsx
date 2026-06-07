import type { ReactNode } from "react";
import { LeftSidebar } from "@/components/layout/LeftSidebar";
import { RightSidebar } from "@/components/layout/RightSidebar";
import { TopBar } from "@/components/layout/TopBar";
import { FilterProvider } from "@/components/filter/FilterProvider";

/**
 * 공통 화면 셸: 상단바 + 좌/우 사이드바 + 본문(children).
 * 홈(목록)과 상세가 동일한 레이아웃을 공유하도록 추출했다.
 * children은 서버에서 렌더된 노드를 그대로 받아 client인 FilterProvider 안으로 전달한다.
 */
export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen bg-bg text-fg">
      <TopBar />
      <FilterProvider>
        <div className="flex">
          <LeftSidebar />

          <main className="min-w-0 flex-1 px-3 py-4 sm:px-5 sm:py-5">
            {children}
          </main>

          <RightSidebar />
        </div>
      </FilterProvider>
    </div>
  );
}
