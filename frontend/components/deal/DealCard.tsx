"use client";

import { Bookmark, ChevronDown, ChevronUp, Flame, MessageCircle, MoreHorizontal } from "lucide-react";
import Image from "next/image";
import { useState } from "react";
import { ShopIcon } from "@/components/common/ShopIcon";
import { formatPrice } from "@/lib/format";
import type { Deal } from "@/lib/types";

interface Props {
  deal: Deal;
  showThumbnail?: boolean;
}

type VoteState = "up" | "down" | null;

export function DealCard({ deal, showThumbnail = true }: Props) {
  const [vote, setVote] = useState<VoteState>(null);
  const [bookmarked, setBookmarked] = useState(false);

  const netVotes =
    deal.voteCount + (vote === "up" ? 1 : vote === "down" ? -1 : 0);

  if (!showThumbnail) {
    return (
      <article className="flex items-center gap-2 rounded-lg border border-border bg-surface/40 px-3 py-2 transition hover:border-border-strong">
        {/* Vote — compact inline */}
        <div className="flex shrink-0 items-center gap-1">
          <button
            type="button"
            aria-label="추천"
            onClick={() => setVote((v) => (v === "up" ? null : "up"))}
            className={`grid size-5 place-items-center rounded transition ${
              vote === "up" ? "text-brand" : "text-fg-subtle hover:text-fg"
            }`}
          >
            <ChevronUp className="size-4" />
          </button>
          <span className="w-6 text-center text-xs font-semibold tabular-nums text-fg">
            {netVotes}
          </span>
          <button
            type="button"
            aria-label="비추천"
            onClick={() => setVote((v) => (v === "down" ? null : "down"))}
            className={`grid size-5 place-items-center rounded transition ${
              vote === "down" ? "text-danger" : "text-fg-subtle hover:text-fg"
            }`}
          >
            <ChevronDown className="size-4" />
          </button>
        </div>

        {/* Body — single row */}
        <div className="flex min-w-0 flex-1 items-center gap-2 overflow-hidden">
          {deal.isHot && (
            <span className="inline-flex shrink-0 items-center gap-0.5 rounded bg-brand-soft px-1 py-0.5 text-[10px] font-semibold text-brand">
              <Flame className="size-2.5" />
              핫딜
            </span>
          )}
          <a
            href={deal.originalUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="min-w-0 flex-1 truncate text-sm font-medium text-fg hover:text-brand transition"
          >
            {deal.title}
          </a>
          <span className="shrink-0 text-sm font-bold tabular-nums text-fg">
            {formatPrice(deal.price, deal.currency)}
          </span>
          <span className="shrink-0 text-xs font-semibold text-danger">
            -{deal.discountPercent}%
          </span>
        </div>

        {/* Right meta */}
        <div className="hidden shrink-0 items-center gap-2 text-xs text-fg-muted sm:flex">
          <span>{deal.shopName}</span>
          <span className="text-fg-subtle">·</span>
          <span>{deal.postedAt}</span>
          <button
            type="button"
            onClick={() => setBookmarked((b) => !b)}
            aria-label="북마크"
            className="text-fg-subtle hover:text-fg transition"
          >
            <Bookmark
              className={`size-3.5 ${bookmarked ? "fill-brand text-brand" : ""}`}
            />
          </button>
        </div>
      </article>
    );
  }

  return (
    <article className="flex gap-3 rounded-xl border border-border bg-surface/40 p-3 transition hover:border-border-strong sm:p-4">
      {/* Vote column */}
      <div className="flex w-9 shrink-0 flex-col items-center gap-0.5 pt-1">
        <button
          type="button"
          aria-label="추천"
          onClick={() => setVote((v) => (v === "up" ? null : "up"))}
          className={`grid size-7 place-items-center rounded transition ${
            vote === "up"
              ? "text-brand"
              : "text-fg-subtle hover:bg-surface hover:text-fg"
          }`}
        >
          <ChevronUp className="size-5" />
        </button>
        <span className="text-sm font-semibold tabular-nums text-fg">{netVotes}</span>
        <button
          type="button"
          aria-label="비추천"
          onClick={() => setVote((v) => (v === "down" ? null : "down"))}
          className={`grid size-7 place-items-center rounded transition ${
            vote === "down"
              ? "text-danger"
              : "text-fg-subtle hover:bg-surface hover:text-fg"
          }`}
        >
          <ChevronDown className="size-5" />
        </button>
      </div>

      {/* Thumbnail */}
      <a
        href={deal.originalUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="relative size-24 shrink-0 overflow-hidden rounded-lg border border-border bg-surface-2 sm:size-28"
      >
        <Image
          src={deal.thumbnail}
          alt={deal.title}
          fill
          sizes="(max-width: 640px) 96px, 112px"
          className="object-cover"
          unoptimized
        />
      </a>

      {/* Body */}
      <div className="flex min-w-0 flex-1 flex-col">
        {/* Top row: badges + meta */}
        <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-fg-muted">
          {deal.isHot && (
            <span className="inline-flex items-center gap-1 rounded-md bg-brand-soft px-1.5 py-0.5 text-[11px] font-semibold text-brand">
              <Flame className="size-3" />
              핫딜
            </span>
          )}
          <span>{deal.shopName}</span>
          <span className="text-fg-subtle">·</span>
          <span>{deal.category}</span>
        </div>

        {/* Title */}
        <a
          href={deal.originalUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="mt-1.5 line-clamp-2 text-[15px] font-medium text-fg hover:text-brand transition sm:text-base"
        >
          {deal.title}
        </a>

        {/* Price */}
        <div className="mt-1.5 flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
          <span className="text-lg font-bold tabular-nums text-fg">
            {formatPrice(deal.price, deal.currency)}
          </span>
          <span className="text-xs text-fg-subtle line-through tabular-nums">
            {formatPrice(deal.originalPrice, deal.currency)}
          </span>
          <span className="text-sm font-semibold text-danger">
            -{deal.discountPercent}%
          </span>
        </div>

        {/* Shipping */}
        <div className="mt-1.5 flex items-center gap-2 text-xs text-fg-muted">
          {deal.freeShipping && <span>무료배송</span>}
          {deal.shippingNote && (
            <span className="inline-flex items-center gap-1">
              <ShopIcon id={deal.shop} size={14} />
              {deal.shippingNote}
            </span>
          )}
        </div>
      </div>

      {/* Right meta column */}
      <div className="hidden w-24 shrink-0 flex-col items-end justify-between text-xs text-fg-muted sm:flex">
        <div className="text-fg-subtle">{deal.postedAt}</div>
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center gap-0.5 text-fg-muted">
            <MessageCircle className="size-3.5" />
            {deal.commentCount}
          </span>
          <button
            type="button"
            onClick={() => setBookmarked((b) => !b)}
            aria-label="북마크"
            className="text-fg-subtle hover:text-fg transition"
          >
            <Bookmark
              className={`size-3.5 ${bookmarked ? "fill-brand text-brand" : ""}`}
            />
          </button>
          <button
            type="button"
            aria-label="더보기"
            className="text-fg-subtle hover:text-fg transition"
          >
            <MoreHorizontal className="size-3.5" />
          </button>
        </div>
      </div>
    </article>
  );
}
