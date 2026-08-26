import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft, ExternalLink, Flame, MessageCircle } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { ApiError, getDeal } from "@/lib/api";
import type { DealDetail, DealStatus } from "@/lib/api-types";
import { formatFullDateTime, formatPrice, splitStoreFromTitle } from "@/lib/format";

/** 상태 → 배지 라벨·색. ACTIVE는 평상시라 배지를 숨긴다(null). */
function statusBadge(status: DealStatus): { label: string; className: string } | null {
  switch (status) {
    case "SOLD_OUT":
      return { label: "품절", className: "bg-surface-2 text-fg-muted" };
    case "EXPIRED":
      return { label: "종료", className: "bg-danger-soft text-danger" };
    default:
      return null;
  }
}

/** 정보 테이블의 한 행(라벨 + 값). */
function InfoRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex border-b border-border last:border-b-0">
      <div className="w-24 shrink-0 bg-surface-2/60 px-3 py-2.5 text-xs font-medium text-fg-muted sm:w-28 sm:text-sm">
        {label}
      </div>
      <div className="min-w-0 flex-1 px-3 py-2.5 text-sm text-fg">{children}</div>
    </div>
  );
}

export default async function DealDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const dealId = Number(id);
  if (!Number.isInteger(dealId) || dealId <= 0) notFound();

  let deal: DealDetail;
  try {
    deal = await getDeal(dealId);
  } catch (error) {
    // 404 → Next notFound, 그 외(백엔드 다운 등)는 상위 error boundary로 던진다.
    if (error instanceof ApiError && error.status === 404) notFound();
    throw error;
  }

  const badge = statusBadge(deal.status);
  const { store: titleStore, title } = splitStoreFromTitle(deal.title);
  const shopName = deal.shopName ?? titleStore;
  const ended = badge !== null;
  const grouped = deal.sourcePosts.length > 1;

  return (
    <AppShell>
      <div className="mx-auto w-full max-w-3xl">
        {/* 뒤로 */}
        <Link
          href="/"
          className="mb-4 inline-flex items-center gap-1.5 text-sm text-fg-muted transition hover:text-fg"
        >
          <ArrowLeft className="size-4" />
          목록으로
        </Link>

        <article className="space-y-5">
          {/* 헤더: 배지 + 제목 */}
          <header className="space-y-3">
            <div className="flex flex-wrap items-center gap-2 text-xs">
              {deal.isHot && (
                <span className="inline-flex items-center gap-1 rounded-md bg-warning-soft px-2 py-0.5 text-xs font-semibold text-warning">
                  <Flame className="size-3" />
                  핫딜
                </span>
              )}
              {badge && (
                <span className={`rounded-md px-2 py-0.5 text-xs font-semibold ${badge.className}`}>
                  {badge.label}
                </span>
              )}
              {shopName && (
                <span className="rounded-md bg-surface-2 px-2 py-0.5 text-xs font-medium text-fg-muted">
                  {shopName}
                </span>
              )}
              {deal.category && (
                <span className="rounded-md bg-surface-2 px-2 py-0.5 text-xs font-medium text-fg-muted">
                  {deal.category}
                </span>
              )}
            </div>

            <h1
              className={`text-xl font-bold leading-snug sm:text-2xl ${
                ended ? "text-fg-muted line-through" : "text-fg"
              }`}
            >
              {title}
            </h1>

            <div className="flex flex-wrap items-center gap-2 text-xs text-fg-muted">
              <span className="font-medium text-fg">{deal.sourceNames.join(" · ")}</span>
              {grouped && (
                <span className="rounded-md bg-brand-soft px-1.5 py-0.5 font-medium text-brand">
                  출처 {deal.sourcePosts.length}곳
                </span>
              )}
              <span className="text-fg-subtle">·</span>
              <span>{formatFullDateTime(deal.postedAt)}</span>
            </div>
          </header>

          {/* 썸네일 — 시각 앵커라 제목 바로 아래에 둔다 */}
          {deal.thumbnailUrl && (
            <div className="relative overflow-hidden rounded-xl border border-border bg-surface-2">
              <Image
                src={deal.thumbnailUrl}
                alt={title}
                width={768}
                height={432}
                sizes="(max-width: 768px) 100vw, 768px"
                className={`h-auto w-full object-contain ${ended ? "opacity-60" : ""}`}
                unoptimized
              />
            </div>
          )}

          {/* 정보 테이블 — 원문 URL은 노출하지 않는다(하단 CTA와 중복) */}
          <div className="overflow-hidden rounded-xl border border-border bg-surface/40">
            {shopName && <InfoRow label="판매처">{shopName}</InfoRow>}

            <InfoRow label="가격">
              {deal.price === 0 ? (
                <span className="inline-flex items-center rounded-md bg-positive-soft px-2 py-0.5 text-sm font-semibold text-positive">
                  무료
                </span>
              ) : deal.price !== null ? (
                <span className="flex flex-wrap items-baseline gap-2">
                  <span className="font-mono text-lg font-bold tabular-nums text-price">
                    {formatPrice(deal.price, deal.currency)}
                  </span>
                  {deal.originalPrice !== null && (
                    <span className="font-mono text-xs text-fg-subtle line-through tabular-nums">
                      {formatPrice(deal.originalPrice, deal.currency)}
                    </span>
                  )}
                  {deal.discountRate !== null && (
                    <span className="text-sm font-semibold text-danger">-{deal.discountRate}%</span>
                  )}
                </span>
              ) : (
                <span className="text-fg-muted">가격 정보 없음</span>
              )}
            </InfoRow>

            {(deal.freeShipping || deal.shippingNote) && (
              <InfoRow label="배송비/직배">
                {deal.freeShipping ? "무료배송" : (deal.shippingNote ?? "-")}
              </InfoRow>
            )}
          </div>

          {/* 본문 */}
          {deal.description && (
            <div className="whitespace-pre-wrap break-words text-sm leading-relaxed text-fg-muted">
              {deal.description}
            </div>
          )}

          {grouped && (
            <section className="space-y-2" aria-labelledby="source-posts-heading">
              <div className="flex items-baseline justify-between gap-3">
                <h2 id="source-posts-heading" className="text-base font-semibold text-fg">
                  출처별 게시글
                </h2>
                <span className="text-xs text-fg-muted">댓글 수는 출처별 반응입니다</span>
              </div>
              <div className="divide-y divide-border overflow-hidden rounded-xl border border-border bg-surface/40">
                {deal.sourcePosts.map((post) => {
                  const postBadge = statusBadge(post.status);
                  return (
                    <div key={post.dealId} className="flex flex-col gap-2 px-3 py-3 sm:flex-row sm:items-center">
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-center gap-2 text-sm">
                          <span className="font-medium text-fg">{post.sourceName}</span>
                          {postBadge && (
                            <span className={`rounded-md px-1.5 py-0.5 text-xs font-semibold ${postBadge.className}`}>
                              {postBadge.label}
                            </span>
                          )}
                          {post.commentCount !== null && (
                            <span className="inline-flex items-center gap-1 text-xs text-fg-muted" aria-label={`댓글 ${post.commentCount}개`}>
                              <MessageCircle className="size-3" aria-hidden="true" />
                              {post.commentCount}
                            </span>
                          )}
                        </div>
                        <p className="mt-1 text-xs text-fg-muted">{formatFullDateTime(post.postedAt)}</p>
                      </div>
                      <div className="flex shrink-0 gap-2">
                        {post.productUrl && (
                          <a
                            href={post.productUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-surface px-3 py-2 text-xs font-medium text-fg transition hover:border-border-strong"
                          >
                            구매처
                            <ExternalLink className="size-3.5" />
                          </a>
                        )}
                        <a
                          href={post.originalUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-xs font-semibold text-white transition hover:bg-brand-strong"
                        >
                          원문
                          <ExternalLink className="size-3.5" />
                        </a>
                      </div>
                    </div>
                  );
                })}
              </div>
            </section>
          )}

          {/* 단일 Deal은 원문 CTA를 유지하고, 그룹 원문은 출처별 영역에서 제공한다. */}
          {(deal.productUrl || !grouped) && (
            <div className="sticky bottom-0 -mx-1 bg-gradient-to-t from-bg via-bg/95 to-transparent px-1 pb-4 pt-6 sm:static sm:mx-0 sm:bg-none sm:p-0 sm:pt-2">
              <div className="flex flex-col gap-2 sm:flex-row">
              {deal.productUrl && (
                <a
                  href={deal.productUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex w-full items-center justify-center gap-2 rounded-lg bg-brand px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-brand-strong sm:w-auto"
                >
                  {shopName ? `${shopName}에서 보기` : "구매처에서 보기"}
                  <ExternalLink className="size-4" />
                </a>
              )}
              {!grouped && (
                <a
                  href={deal.originalUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={`inline-flex w-full items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold transition sm:w-auto ${
                    deal.productUrl
                      ? "border border-border bg-surface text-fg hover:border-border-strong"
                      : "bg-brand text-white hover:bg-brand-strong"
                  }`}
                >
                  원문에서 보기
                  <ExternalLink className="size-4" />
                </a>
              )}
              </div>
            </div>
          )}
        </article>
      </div>
    </AppShell>
  );
}
