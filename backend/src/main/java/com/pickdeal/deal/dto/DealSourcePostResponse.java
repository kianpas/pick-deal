package com.pickdeal.deal.dto;

import com.pickdeal.deal.domain.Deal;
import java.time.OffsetDateTime;

/** DealGroup 상세에서 원본을 잃지 않고 보여주는 출처별 게시글. */
public record DealSourcePostResponse(
        Long dealId,
        Long sourceId,
        String sourceName,
        String originalUrl,
        String productUrl,
        Integer commentCount,
        OffsetDateTime postedAt,
        String status
) {

    public static DealSourcePostResponse from(Deal deal) {
        return new DealSourcePostResponse(
                deal.getId(),
                deal.getSource().getId(),
                deal.getSource().getName(),
                deal.getOriginalUrl(),
                deal.getProductUrl(),
                deal.getCommentCount(),
                deal.getPostedAt(),
                deal.getStatus().name()
        );
    }
}
