import type { ReactNode } from "react";
import { LeftSidebar } from "@/components/layout/LeftSidebar";
import { TopBar } from "@/components/layout/TopBar";
import { FilterProvider } from "@/components/filter/FilterProvider";
import { MobileSourceDrawer } from "@/components/source/MobileSourceDrawer";
import { READ_ONLY } from "@/lib/runtime-config";

/**
 * 공통 화면 셸: 너비를 제한한 상단바 + 왼쪽 필터 + 본문(children).
 * 홈(목록)과 상세가 동일한 레이아웃을 공유하도록 추출했다.
 * children은 서버에서 렌더된 노드를 그대로 받아 client인 FilterProvider 안으로 전달한다.
 */
export function AppShell({ children, filters }: { children: ReactNode; filters?: ReactNode }) {
  return (
    <div className="min-h-screen bg-bg text-fg">
      <TopBar />
      <FilterProvider>
        <div className="mx-auto flex w-full max-w-[75rem]">
          <LeftSidebar filters={filters} />

          <main id="main-content" tabIndex={-1} className="min-w-0 flex-1 scroll-mt-32 px-4 py-4 sm:px-5 sm:py-5 md:scroll-mt-20">
            {!READ_ONLY && <MobileSourceDrawer />}
            {children}
          </main>

        </div>
      </FilterProvider>
    </div>
  );
}
