import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { TopBar } from "@/components/layout/TopBar";
import { KeywordManager } from "@/components/settings/KeywordManager";
import { getKeywords } from "@/lib/api";
import type { KeywordItem } from "@/lib/api-types";

/**
 * 키워드 관리 화면(/settings/keywords).
 * 초기 목록은 SSR로 받아 클라이언트 매니저에 주입한다(추가·삭제 상호작용은 클라이언트).
 */
export default async function KeywordSettingsPage() {
  let keywords: KeywordItem[] = [];
  try {
    keywords = await getKeywords();
  } catch (error) {
    // 백엔드 미기동 등 → 빈 목록으로 시작(추가 시도 시 에러 메시지로 안내).
    console.error("키워드를 불러오지 못했습니다:", error);
  }

  return (
    <div className="min-h-screen bg-bg text-fg">
      <TopBar />

      <main className="mx-auto w-full max-w-2xl px-4 py-5 sm:px-6 sm:py-6">
        <Link
          href="/"
          className="mb-4 inline-flex items-center gap-1.5 text-sm text-fg-muted transition hover:text-fg"
        >
          <ArrowLeft className="size-4" />
          목록으로
        </Link>

        <header className="mb-6">
          <h1 className="text-xl font-bold text-fg sm:text-2xl">키워드 관리</h1>
          <p className="mt-1 text-sm text-fg-muted">
            관심 키워드와 제외 키워드로 핫딜 목록을 내 취향에 맞게 거릅니다.
          </p>
        </header>

        <KeywordManager initialKeywords={keywords} />
      </main>
    </div>
  );
}
