import { Flame, MessageCircle } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { formatPostedAt, formatPrice } from "@/lib/format";
import type { DealSummary } from "@/lib/api-types";

interface Props {
  deal: DealSummary;
  showThumbnail?: boolean;
}

/**
 * 딜 카드(목록 항목). 백엔드 DealSummary 기준 display-only.
 * 데모 단계 필드(isHot/shippingNote/commentCount 등)는 값이 있을 때만 렌더한다 — 현재 백엔드는 미제공.
 * 카드 링크는 상세 라우트(/deals/{id})로 향한다(상세 화면은 후속 단계).
 */
export function DealCard({ deal, showThumbnail = true }: Props) {
  const detailHref = `/deals/${deal.id}`;
  const priceText = deal.price !== null ? formatPrice(deal.price, deal.currency) : null;

  if (!showThumbnail) {
    return (
      <article className="flex items-center gap-2 rounded-lg border border-border bg-surface/40 px-3 py-2 transition hover:border-border-strong">
        <div className="flex min-w-0 flex-1 items-center gap-2 overflow-hidden">
          {deal.isHot && (
            <span className="inline-flex shrink-0 items-center gap-0.5 rounded bg-brand-soft px-1 py-0.5 text-[10px] font-semibold text-brand">
              <Flame className="size-2.5" />
              핫딜
            </span>
          )}
          <Link
            href={detailHref}
            className="min-w-0 flex-1 truncate text-sm font-medium text-fg hover:text-brand transition"
          >
            {deal.title}
          </Link>
          {priceText && (
            <span className="shrink-0 text-sm font-bold tabular-nums text-fg">{priceText}</span>
          )}
          {deal.discountRate !== null && (
            <span className="shrink-0 text-xs font-semibold text-danger">-{deal.discountRate}%</span>
          )}
        </div>

        <div className="hidden shrink-0 items-center gap-2 text-xs text-fg-muted sm:flex">
          <span>{deal.sourceName}</span>
          <span className="text-fg-subtle">·</span>
          <span>{formatPostedAt(deal.postedAt)}</span>
        </div>
      </article>
    );
  }

  return (
    <article className="flex gap-3 rounded-xl border border-border bg-surface/40 p-3 transition hover:border-border-strong sm:p-4">
      {/* Thumbnail */}
      <Link
        href={detailHref}
        className="relative size-24 shrink-0 overflow-hidden rounded-lg border border-border bg-surface-2 sm:size-28"
      >
        {deal.thumbnailUrl ? (
          <Image
            src={deal.thumbnailUrl}
            alt={deal.title}
            fill
            sizes="(max-width: 640px) 96px, 112px"
            className="object-cover"
            unoptimized
          />
        ) : (
          <span className="grid size-full place-items-center text-xs text-fg-subtle">이미지 없음</span>
        )}
      </Link>

      {/* Body */}
      <div className="flex min-w-0 flex-1 flex-col">
        <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-fg-muted">
          {deal.isHot && (
            <span className="inline-flex items-center gap-1 rounded-md bg-brand-soft px-1.5 py-0.5 text-[11px] font-semibold text-brand">
              <Flame className="size-3" />
              핫딜
            </span>
          )}
          <span>{deal.sourceName}</span>
          {deal.category && (
            <>
              <span className="text-fg-subtle">·</span>
              <span>{deal.category}</span>
            </>
          )}
        </div>

        <Link
          href={detailHref}
          className="mt-1.5 line-clamp-2 text-[15px] font-medium text-fg hover:text-brand transition sm:text-base"
        >
          {deal.title}
        </Link>

        <div className="mt-1.5 flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
          {priceText && (
            <span className="text-lg font-bold tabular-nums text-fg">{priceText}</span>
          )}
          {deal.originalPrice !== null && (
            <span className="text-xs text-fg-subtle line-through tabular-nums">
              {formatPrice(deal.originalPrice, deal.currency)}
            </span>
          )}
          {deal.discountRate !== null && (
            <span className="text-sm font-semibold text-danger">-{deal.discountRate}%</span>
          )}
        </div>

        {(deal.freeShipping || deal.shippingNote) && (
          <div className="mt-1.5 flex items-center gap-2 text-xs text-fg-muted">
            {deal.freeShipping && <span>무료배송</span>}
            {deal.shippingNote && <span>{deal.shippingNote}</span>}
          </div>
        )}
      </div>

      {/* Right meta */}
      <div className="hidden w-24 shrink-0 flex-col items-end justify-between text-xs text-fg-muted sm:flex">
        <div className="text-fg-subtle">{formatPostedAt(deal.postedAt)}</div>
        {deal.commentCount !== undefined && (
          <span className="inline-flex items-center gap-0.5 text-fg-muted">
            <MessageCircle className="size-3.5" />
            {deal.commentCount}
          </span>
        )}
      </div>
    </article>
  );
}
