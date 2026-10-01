import { ExternalLink } from "lucide-react";
import type { DealSourcePost, DealStatus } from "@/lib/api-types";
import { formatFullDateTime, formatPrice } from "@/lib/format";

const states: Record<DealStatus, { label: string; style: string }> = {
  ACTIVE: { label: "진행 중", style: "bg-positive-soft text-positive" },
  SOLD_OUT: { label: "품절", style: "bg-surface-2 text-fg-muted" },
  EXPIRED: { label: "종료", style: "bg-danger-soft text-danger" },
};

export function SourcePostComparison({ posts, currentDealId }: {
  posts: DealSourcePost[];
  currentDealId: number;
}) {
  return (
    <section className="space-y-2" aria-labelledby="source-posts-heading">
      <h2 id="source-posts-heading" className="text-base font-semibold">출처별 비교</h2>
      <p className="text-xs leading-relaxed text-fg-muted">저장된 게시글 기준입니다. 실제 판매 상태와 다를 수 있으며, 댓글 수는 출처별 반응입니다.</p>
      <ul className="divide-y divide-border rounded-xl border border-border bg-surface/40">
        {posts.map((post) => {
          const state = states[post.status];
          return (
            <li key={post.dealId} className="grid min-w-0 gap-2 px-3 py-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:gap-x-4">
              <div className="min-w-0 space-y-1.5">
                <div className="flex flex-wrap items-center gap-2 text-sm">
                  <span className="font-semibold">{post.sourceName}</span>
                  <span className={`rounded-md px-1.5 py-0.5 text-xs font-medium ${state.style}`}>{state.label}</span>
                  {post.dealId === currentDealId && <span className="text-xs text-fg-muted">현재 상세</span>}
                </div>
                <p className="wrap-anywhere text-sm leading-snug">{post.title}</p>
                <p className="wrap-anywhere text-xs text-fg-muted">판매처: {post.shopName ?? "미확인"}</p>
                <div className="flex flex-wrap gap-x-3 gap-y-1 text-xs text-fg-muted">
                  <time dateTime={post.postedAt}>{formatFullDateTime(post.postedAt)}</time>
                  <span>댓글 <span className="font-mono tabular-nums">{post.commentCount === null ? "미확인" : `${post.commentCount}개`}</span></span>
                </div>
              </div>
              <div className="flex min-w-0 flex-wrap items-center justify-between gap-2 sm:flex-col sm:items-end sm:justify-center">
                <p className={`wrap-anywhere text-sm font-semibold ${post.price === null ? "text-fg-muted" : "font-mono tabular-nums text-price"}`}>
                  {post.price === null ? "가격 정보 없음" : post.price === 0 ? "무료" : formatPrice(post.price, post.currency)}
                </p>
                <div className="flex flex-wrap gap-2">
                  {post.productUrl && <a href={post.productUrl} target="_blank" rel="noopener noreferrer" aria-label={`${post.sourceName} 게시글의 구매처 보기 (새 탭)`} className="inline-flex min-h-11 items-center gap-1.5 rounded-lg border border-border px-3 text-xs font-medium transition hover:border-border-strong">구매처<ExternalLink className="size-3.5" aria-hidden="true" /></a>}
                  <a href={post.originalUrl} target="_blank" rel="noopener noreferrer" aria-label={`${post.sourceName} 원문 보기 (새 탭)`} className="inline-flex min-h-11 items-center gap-1.5 rounded-lg bg-brand-strong px-3 text-xs font-semibold text-white transition hover:brightness-110">원문<ExternalLink className="size-3.5" aria-hidden="true" /></a>
                </div>
              </div>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
