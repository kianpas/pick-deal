package com.pickdeal.deal.dto;

import com.pickdeal.deal.domain.Deal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

public record DealDetailResponse(
        Long id,
        String title,
        String description,
        Long price,
        Long originalPrice,
        Integer discountRate,
        String currency,
        String category,
        String shopName,
        Integer commentCount,
        String thumbnailUrl,
        String originalUrl,
        String productUrl,
        Long sourceId,
        String sourceName,
        Long groupId,
        int sourceCount,
        List<String> sourceNames,
        List<DealSourcePostResponse> sourcePosts,
        String externalId,
        OffsetDateTime postedAt,
        OffsetDateTime collectedAt,
        String status
) {

    public static DealDetailResponse from(Deal deal) {
        return from(deal, List.of(deal));
    }

    public static DealDetailResponse from(Deal deal, List<Deal> sourceDeals) {
        return new DealDetailResponse(
                deal.getId(),
                deal.getTitle(),
                deal.getDescription(),
                deal.getPrice(),
                deal.getOriginalPrice(),
                deal.getDiscountRate(),
                deal.getCurrency(),
                deal.getCategory(),
                deal.getShopName(),
                deal.getCommentCount(),
                deal.getThumbnailUrl(),
                deal.getOriginalUrl(),
                deal.getProductUrl(),
                deal.getSource().getId(),
                deal.getSource().getName(),
                deal.getDealGroup() == null ? null : deal.getDealGroup().getId(),
                sourceDeals.size(),
                sourceDeals.stream()
                        .map(sourceDeal -> sourceDeal.getSource().getName())
                        .distinct()
                        .sorted()
                        .toList(),
                sourceDeals.stream()
                        .sorted(Comparator.comparing(Deal::getPostedAt).reversed())
                        .map(DealSourcePostResponse::from)
                        .toList(),
                deal.getExternalId(),
                deal.getPostedAt(),
                deal.getCollectedAt(),
                deal.getStatus().name()
        );
    }
}
