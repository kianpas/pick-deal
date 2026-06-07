import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft, ExternalLink, Flame } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { ApiError, getDeal } from "@/lib/api";
import type { DealDetail, DealStatus } from "@/lib/api-types";
import { formatFullDateTime, formatPrice } from "@/lib/format";

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
  const priceText = deal.price !== null ? formatPrice(deal.price, deal.currency) : null;

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
                <span className="inline-flex items-center gap-1 rounded-md bg-brand-soft px-2 py-0.5 text-[11px] font-semibold text-brand">
                  <Flame className="size-3" />
                  핫딜
                </span>
              )}
              {badge && (
                <span className={`rounded-md px-2 py-0.5 text-[11px] font-semibold ${badge.className}`}>
                  {badge.label}
                </span>
              )}
              {deal.category && (
                <span className="rounded-md bg-surface-2 px-2 py-0.5 text-[11px] font-medium text-fg-muted">
                  {deal.category}
                </span>
              )}
            </div>

            <h1 className="text-xl font-bold leading-snug text-fg sm:text-2xl">{deal.title}</h1>

            <div className="flex flex-wrap items-center gap-2 text-xs text-fg-muted">
              <span className="font-medium text-fg">{deal.sourceName}</span>
              <span className="text-fg-subtle">·</span>
              <span>{formatFullDateTime(deal.postedAt)}</span>
            </div>
          </header>

          {/* 정보 테이블 */}
          <div className="overflow-hidden rounded-xl border border-border bg-surface/40">
            <InfoRow label="링크">
              <a
                href={deal.originalUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1 break-all text-brand transition hover:underline"
              >
                {deal.originalUrl}
                <ExternalLink className="size-3.5 shrink-0" />
              </a>
            </InfoRow>

            {deal.shop && <InfoRow label="판매처">{deal.shop}</InfoRow>}

            <InfoRow label="가격">
              {priceText ? (
                <span className="flex flex-wrap items-baseline gap-2">
                  <span className="text-lg font-bold tabular-nums text-price">{priceText}</span>
                  {deal.originalPrice !== null && (
                    <span className="text-xs text-fg-subtle line-through tabular-nums">
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

          {/* 썸네일 */}
          {deal.thumbnailUrl && (
            <div className="relative overflow-hidden rounded-xl border border-border bg-surface-2">
              <Image
                src={deal.thumbnailUrl}
                alt={deal.title}
                width={768}
                height={432}
                sizes="(max-width: 768px) 100vw, 768px"
                className="h-auto w-full object-contain"
                unoptimized
              />
            </div>
          )}

          {/* 본문 */}
          {deal.description && (
            <div className="whitespace-pre-wrap break-words text-sm leading-relaxed text-fg-muted">
              {deal.description}
            </div>
          )}

          {/* 원문 보기 CTA */}
          <div className="pt-2">
            <a
              href={deal.originalUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-brand-strong"
            >
              원문에서 보기
              <ExternalLink className="size-4" />
            </a>
          </div>
        </article>
      </div>
    </AppShell>
  );
}
