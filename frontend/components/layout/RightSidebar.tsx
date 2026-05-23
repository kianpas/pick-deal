import { AISummaryCard } from "@/components/sidebar/AISummaryCard";
import { BestList } from "@/components/sidebar/BestList";
import { KeywordCloud } from "@/components/sidebar/KeywordCloud";

export function RightSidebar() {
  return (
    <aside className="sticky top-16 hidden h-[calc(100vh-4rem)] w-[320px] shrink-0 overflow-y-auto scrollbar-thin border-l border-border px-5 py-5 space-y-4 xl:block">
      <AISummaryCard />
      <BestList />
      <KeywordCloud />
    </aside>
  );
}
