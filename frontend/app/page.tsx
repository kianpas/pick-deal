import { DealFeed } from "@/components/deal/DealFeed";
import { AppShell } from "@/components/layout/AppShell";
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
    <AppShell>
      <DealFeed deals={deals} />
    </AppShell>
  );
}
