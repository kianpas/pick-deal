import { DealThumbnail } from "@/components/deal/DealThumbnail";
import { DealShareButton } from "@/components/deal/DealShareButton";
import { SourcePostComparison } from "@/components/deal/SourcePostComparison";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft, ExternalLink, Flame } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { ApiError, getDeal } from "@/lib/api";
import type { DealDetail, DealStatus } from "@/lib/api-types";
import { formatFullDateTime, formatPrice, splitStoreFromTitle } from "@/lib/format";
import { listLocation } from "@/lib/list-location";

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
      <dt className="w-24 shrink-0 px-3 py-2.5 text-xs font-medium text-fg-muted sm:w-28 sm:text-sm">
        {label}
      </dt>
      <dd className="min-w-0 flex-1 wrap-anywhere px-3 py-2.5 text-sm text-fg">{children}</dd>
    </div>
  );
}

export default async function DealDetailPage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ returnTo?: string | string[] }>;
}) {
  const { returnTo } = await searchParams;
  const listHref = listLocation(typeof returnTo === "string" ? returnTo : undefined);
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
        <div className="mb-4 flex flex-wrap items-start justify-between gap-x-4 gap-y-2">
          <Link
            href={listHref}
            className="inline-flex min-h-11 items-center gap-1.5 text-sm text-fg-muted transition hover:text-fg"
          >
            <ArrowLeft className="size-4" aria-hidden="true" />
            목록으로
          </Link>
          <DealShareButton dealId={deal.id} />
        </div>

        <article className="space-y-5 pb-[max(1rem,env(safe-area-inset-bottom))]">
          {/* 헤더: 배지 + 제목 */}
          <header className="space-y-3">
            <div className="flex flex-wrap items-center gap-2 wrap-anywhere text-xs">
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
              {deal.categoryName && (
                <span className="rounded-md bg-surface-2 px-2 py-0.5 text-xs font-medium text-fg-muted">
                  {deal.categoryName}
                </span>
              )}
            </div>

            <h1
              className={`wrap-anywhere text-xl font-bold leading-snug sm:text-2xl ${
                ended ? "text-fg-muted line-through" : "text-fg"
              }`}
            >
              {title}
            </h1>

            <div className="flex flex-wrap items-center gap-2 text-xs text-fg-muted">
              <span className="font-medium text-fg">{deal.sourceName}{grouped ? " 게시글 기준" : ""}</span>
              {grouped && (
                <span className="rounded-md bg-brand-soft px-1.5 py-0.5 font-medium text-brand">
                  출처 {deal.sourcePosts.length}곳
                </span>
              )}
              <span className="text-fg-subtle">·</span>
              <span>{formatFullDateTime(deal.postedAt)}</span>
            </div>
          </header>

          <div className="flex items-center gap-4 border-y border-border py-4">
            {deal.thumbnailUrl && (
              <div className="size-24 shrink-0 overflow-hidden rounded-lg border border-border bg-surface-2">
                <DealThumbnail src={deal.thumbnailUrl} alt={title} detail ended={ended} />
              </div>
            )}
            <div className="min-w-0 space-y-1">
              <p className="text-xs text-fg-muted">가격 · {deal.sourceName} 게시글 기준</p>
              {deal.price === 0 ? (
                <span className="inline-flex items-center rounded-md bg-positive-soft px-2 py-0.5 text-sm font-semibold text-positive">
                  무료
                </span>
              ) : deal.price !== null ? (
                <span className="flex flex-wrap items-baseline gap-2">
                  <span className="wrap-anywhere font-sans text-2xl font-bold tabular-nums text-price">
                    {formatPrice(deal.price, deal.currency)}
                  </span>
                  {deal.originalPrice !== null && (
                    <span className="font-sans text-xs text-fg-subtle line-through tabular-nums">
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
            </div>
          </div>

          {grouped && <SourcePostComparison posts={deal.sourcePosts} currentDealId={deal.id} />}

          {/* 단일 Deal은 원문 CTA를 유지하고, 그룹 원문은 출처별 영역에서 제공한다. */}
          {(deal.productUrl || !grouped) && (
            <div className="pb-1">
              <div className="flex flex-col gap-2 sm:flex-row">
              {deal.productUrl && (
                <a
                  href={deal.productUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex min-h-11 min-w-0 w-full items-center justify-center gap-2 rounded-lg bg-brand-strong px-4 py-2.5 text-sm font-semibold text-white transition hover:brightness-110 sm:w-auto"
                >
                  <span className="min-w-0 wrap-anywhere">{shopName ? `${shopName}에서 보기` : "구매처에서 보기"}</span>
                  <ExternalLink className="size-4 shrink-0" />
                </a>
              )}
              {!grouped && (
                <a
                  href={deal.originalUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={`inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold transition sm:w-auto ${
                    deal.productUrl
                      ? "border border-border bg-surface text-fg hover:border-border-strong"
                      : "bg-brand-strong text-white hover:brightness-110"
                  }`}
                >
                  원문에서 보기
                  <ExternalLink className="size-4" />
                </a>
              )}
              </div>
            </div>
          )}

          {/* 저장된 부가 정보 */}
          <dl className="rounded-lg border border-border">
            {shopName && <InfoRow label="판매처">{shopName}</InfoRow>}
            <InfoRow label="수집 시각">{formatFullDateTime(deal.collectedAt)}</InfoRow>
            {!grouped && deal.commentCount !== null && <InfoRow label="댓글">{deal.commentCount}개 <span className="text-xs text-fg-muted">(수집 시점)</span></InfoRow>}
            {deal.category && <InfoRow label="출처 분류"><span className="text-xs text-fg-muted">{deal.category}</span></InfoRow>}
            {(deal.freeShipping || deal.shippingNote) && (
              <InfoRow label="배송비/직배">
                {deal.freeShipping ? "무료배송" : (deal.shippingNote ?? "-")}
              </InfoRow>
            )}
          </dl>

          {/* 본문 */}
          {deal.description && (
            <section className="space-y-2" aria-labelledby="description-heading">
              <h2 id="description-heading" className="text-base font-semibold">상품 설명</h2>
              <p className="whitespace-pre-wrap wrap-anywhere text-sm leading-relaxed text-fg-muted">{deal.description}</p>
            </section>
          )}

        </article>
      </div>
    </AppShell>
  );
}
