import { Flame, Layers2, MessageCircle } from "lucide-react";
import { DealThumbnail } from "@/components/deal/DealThumbnail";
import Link from "next/link";
import { formatPrice, formatRelativeTime, splitStoreFromTitle } from "@/lib/format";
import type { DealSummary } from "@/lib/api-types";

interface Props {
  deal: DealSummary;
  showThumbnail?: boolean;
  listHref?: string;
}

function PriceText({ deal, ended }: { deal: DealSummary; ended: boolean }) {
  if (deal.price === null) {
    return <span className="text-xs text-fg-muted">가격 정보 없음</span>;
  }
  return (
    <span className={`text-lg font-bold ${deal.price === 0 ? "font-sans" : "font-mono tabular-nums"} ${ended ? "text-fg-muted" : "text-price"}`}>
      {deal.price === 0 ? "무료" : formatPrice(deal.price, deal.currency)}
    </span>
  );
}

/** 제목 → 가격 → 판매처·반응 순서로 읽는 목록 행. 그룹은 현재 필터 기준 출처 수를 표시한다. */
export function DealCard({ deal, showThumbnail = true, listHref = "/" }: Props) {
  const detailHref = `/deals/${deal.id}?returnTo=${encodeURIComponent(listHref)}`;
  const { store: titleStore, title } = splitStoreFromTitle(deal.title);
  const shopName = deal.shopName ?? titleStore;
  const ended = deal.status !== "ACTIVE";
  const statusLabel = deal.status === "SOLD_OUT" ? "품절" : deal.status === "EXPIRED" ? "종료" : null;
  const grouped = deal.sourceCount > 1;

  return (
    <article className="@container px-3 py-4 transition-colors hover:bg-surface/60 sm:px-4">
      <div className="flex items-start gap-3 sm:gap-4">
        {showThumbnail && (
          <Link href={detailHref} aria-label={`${title} 상세 보기`}
            className="relative mt-1 size-16 shrink-0 overflow-hidden rounded-lg bg-surface-2 @min-[36rem]:size-20">
            <DealThumbnail src={deal.thumbnailUrl} alt={title} />
          </Link>
        )}

        <div className="min-w-0 flex-1">
          <div className="min-w-0 space-y-1">
            <Link href={detailHref}
              className={`min-h-11 content-center wrap-anywhere text-[15px] font-medium leading-relaxed transition-colors hover:text-brand @min-[36rem]:text-base ${ended ? "text-fg-muted line-through" : "text-fg"}`}>
              {title}
            </Link>

            <div className="flex min-w-0 flex-wrap items-baseline gap-x-2 gap-y-1">
              <PriceText deal={deal} ended={ended} />
              {deal.discountRate !== null && (
                <span className="text-xs font-semibold text-danger">-{deal.discountRate}%</span>
              )}
              {deal.originalPrice !== null && (
                <span className="font-mono text-xs text-fg-muted line-through tabular-nums">
                  {formatPrice(deal.originalPrice, deal.currency)}
                </span>
              )}
            </div>

            <div className="flex min-w-0 flex-wrap items-center gap-x-3 gap-y-1 text-xs text-fg-muted wrap-anywhere">
              {statusLabel && <span className="font-semibold">{statusLabel}</span>}
              {deal.isHot && (
                <span className="inline-flex items-center gap-1 font-medium text-warning">
                  <Flame className="size-3" aria-hidden="true" />핫딜
                </span>
              )}
              {shopName && <span className="font-medium">{shopName}</span>}
              {!grouped && <span>{deal.sourceNames.join(" · ")}</span>}
              {deal.commentCount !== null && (
                <span className="inline-flex items-center gap-1" aria-label={`댓글 ${deal.commentCount}개`}>
                  <MessageCircle className="size-3" aria-hidden="true" />{deal.commentCount}
                </span>
              )}
              <span suppressHydrationWarning>{formatRelativeTime(deal.postedAt)}</span>
              {deal.freeShipping && <span>무료배송</span>}
              {deal.shippingNote && <span>{deal.shippingNote}</span>}
            </div>
          </div>

          {grouped && (
            <div className="mt-3 flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1 border-s-2 border-brand ps-2 text-xs wrap-anywhere">
              <span className="inline-flex items-center gap-1.5 font-medium text-brand">
                <Layers2 className="size-3.5 shrink-0" aria-hidden="true" />
                출처 {deal.sourceCount}곳
              </span>
              <span className="text-fg-muted">{deal.sourceNames.join(" · ")}</span>
            </div>
          )}
        </div>
      </div>
    </article>
  );
}
