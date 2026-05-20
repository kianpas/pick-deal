package com.pickdeal.deal.dto;

import com.pickdeal.deal.domain.Deal;
import java.time.OffsetDateTime;

public record DealDetailResponse(
        Long id,
        String title,
        String description,
        Long price,
        Long originalPrice,
        Integer discountRate,
        String currency,
        String category,
        String thumbnailUrl,
        String originalUrl,
        Long sourceId,
        String sourceName,
        String externalId,
        OffsetDateTime postedAt,
        OffsetDateTime collectedAt,
        String status
) {

    public static DealDetailResponse from(Deal deal) {
        return new DealDetailResponse(
                deal.getId(),
                deal.getTitle(),
                deal.getDescription(),
                deal.getPrice(),
                deal.getOriginalPrice(),
                deal.getDiscountRate(),
                deal.getCurrency(),
                deal.getCategory(),
                deal.getThumbnailUrl(),
                deal.getOriginalUrl(),
                deal.getSource().getId(),
                deal.getSource().getName(),
                deal.getExternalId(),
                deal.getPostedAt(),
                deal.getCollectedAt(),
                deal.getStatus().name()
        );
    }
}
