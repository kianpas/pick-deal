import { Flame } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { formatPrice, formatRelativeTime, splitStoreFromTitle } from "@/lib/format";
import type { DealSummary } from "@/lib/api-types";

interface Props {
  deal: DealSummary;
  showThumbnail?: boolean;
}

/** 종료/품절 뱃지. ACTIVE는 평상시라 null. */
function statusBadge(status: DealSummary["status"]): { label: string; className: string } | null {
  switch (status) {
    case "SOLD_OUT":
      return { label: "품절", className: "bg-surface-2 text-fg-muted" };
    case "EXPIRED":
      return { label: "종료", className: "bg-danger-soft text-danger" };
    default:
      return null;
  }
}

/** 가격 표기: 0원은 "무료" 뱃지, null은 자리 유지용 안내 문구. */
function PriceText({ deal, compact = false }: { deal: DealSummary; compact?: boolean }) {
  if (deal.price === 0) {
    return (
      <span className="inline-flex items-center rounded-md bg-positive-soft px-1.5 py-0.5 text-xs font-semibold text-positive">
        무료
      </span>
    );
  }
  if (deal.price === null) {
    return <span className="text-xs text-fg-subtle">가격 정보 없음</span>;
  }
  return (
    <span className={`font-bold tabular-nums text-price ${compact ? "text-sm" : "text-lg"}`}>
      {formatPrice(deal.price, deal.currency)}
    </span>
  );
}

/**
 * 딜 카드(목록 항목). 백엔드 DealSummary 기준 display-only.
 * 제목의 "[판매처]" 접두사는 칩으로 분리하고, 시각은 상대 표기(근사값 정밀도에 맞춤).
 * 데모 단계 필드(isHot 등)는 값이 있을 때만 렌더한다 — 현재 백엔드는 미제공.
 */
export function DealCard({ deal, showThumbnail = true }: Props) {
  const detailHref = `/deals/${deal.id}`;
  const { store, title } = splitStoreFromTitle(deal.title);
  const badge = statusBadge(deal.status);
  // 종료/품절 딜은 남겨두되 취소선 + 흐림으로 한눈에 구분한다
  const ended = badge !== null;

  if (!showThumbnail) {
    return (
      <article
        className={`flex items-center gap-2 rounded-lg border border-border bg-surface/40 px-3 py-2 transition hover:border-border-strong ${
          ended ? "opacity-60" : ""
        }`}
      >
        <div className="flex min-w-0 flex-1 items-center gap-2 overflow-hidden">
          {badge && (
            <span className={`shrink-0 rounded-md px-1.5 py-0.5 text-xs font-semibold ${badge.className}`}>
              {badge.label}
            </span>
          )}
          {store && (
            <span className="shrink-0 rounded-md bg-surface-2 px-1.5 py-0.5 text-xs font-medium text-fg-muted">
              {store}
            </span>
          )}
          <Link
            href={detailHref}
            className={`min-w-0 flex-1 truncate text-sm font-medium hover:text-brand transition ${
              ended ? "text-fg-muted line-through" : "text-fg"
            }`}
          >
            {title}
          </Link>
          <PriceText deal={deal} compact />
          {deal.discountRate !== null && (
            <span className="shrink-0 text-xs font-semibold text-danger">-{deal.discountRate}%</span>
          )}
        </div>

        <div className="hidden shrink-0 items-center gap-2 text-xs text-fg-muted sm:flex">
          <span>{deal.sourceName}</span>
          <span className="text-fg-subtle">·</span>
          <span suppressHydrationWarning>{formatRelativeTime(deal.postedAt)}</span>
        </div>
      </article>
    );
  }

  return (
    <article
      className={`flex gap-3 rounded-xl border border-border bg-surface/40 p-3 transition hover:border-border-strong sm:p-4 ${
        ended ? "opacity-60" : ""
      }`}
    >
      {/* Thumbnail */}
      <Link
        href={detailHref}
        className="relative size-24 shrink-0 overflow-hidden rounded-lg border border-border bg-surface-2 sm:size-28"
      >
        {deal.thumbnailUrl ? (
          <Image
            src={deal.thumbnailUrl}
            alt={title}
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
            <span className="inline-flex items-center gap-1 rounded-md bg-warning-soft px-1.5 py-0.5 text-xs font-semibold text-warning">
              <Flame className="size-3" />
              핫딜
            </span>
          )}
          {badge && (
            <span className={`rounded-md px-1.5 py-0.5 text-xs font-semibold ${badge.className}`}>
              {badge.label}
            </span>
          )}
          {store && (
            <span className="rounded-md bg-surface-2 px-1.5 py-0.5 text-xs font-medium text-fg-muted">
              {store}
            </span>
          )}
          <span>{deal.sourceName}</span>
          {deal.category && (
            <>
              <span className="text-fg-subtle">·</span>
              <span>{deal.category}</span>
            </>
          )}
          <span className="ml-auto text-fg-subtle" suppressHydrationWarning>
            {formatRelativeTime(deal.postedAt)}
          </span>
        </div>

        <Link
          href={detailHref}
          className={`mt-1.5 line-clamp-2 text-[15px] font-medium hover:text-brand transition sm:text-base ${
            ended ? "text-fg-muted line-through" : "text-fg"
          }`}
        >
          {title}
        </Link>

        <div className="mt-1.5 flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
          <PriceText deal={deal} />
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
    </article>
  );
}
