package com.pickdeal.deal.dto;

import com.pickdeal.deal.domain.Deal;
import java.time.OffsetDateTime;

public record DealSummaryResponse(
        Long id,
        String title,
        Long price,
        Long originalPrice,
        Integer discountRate,
        String currency,
        String category,
        String thumbnailUrl,
        Long sourceId,
        String sourceName,
        OffsetDateTime postedAt,
        OffsetDateTime collectedAt,
        String status
) {

    public static DealSummaryResponse from(Deal deal) {
        return new DealSummaryResponse(
                deal.getId(),
                deal.getTitle(),
                deal.getPrice(),
                deal.getOriginalPrice(),
                deal.getDiscountRate(),
                deal.getCurrency(),
                deal.getCategory(),
                deal.getThumbnailUrl(),
                deal.getSource().getId(),
                deal.getSource().getName(),
                deal.getPostedAt(),
                deal.getCollectedAt(),
                deal.getStatus().name()
        );
    }
}
