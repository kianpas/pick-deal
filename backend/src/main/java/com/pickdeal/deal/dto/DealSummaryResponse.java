package com.pickdeal.deal.dto;

import com.pickdeal.deal.domain.Deal;
import java.time.OffsetDateTime;
import java.util.List;

public record DealSummaryResponse(
        Long id,
        String title,
        Long price,
        Long originalPrice,
        Integer discountRate,
        String currency,
        String category,
        String shopName,
        Integer commentCount,
        String thumbnailUrl,
        Long sourceId,
        String sourceName,
        Long groupId,
        int sourceCount,
        List<String> sourceNames,
        OffsetDateTime postedAt,
        OffsetDateTime collectedAt,
        String status
) {

    public static DealSummaryResponse from(Deal deal) {
        return from(deal, List.of(deal), deal.getStatus().name(), deal.getPostedAt(), deal.getCollectedAt());
    }

    public static DealSummaryResponse from(
            Deal representative,
            List<Deal> members,
            String status,
            OffsetDateTime postedAt,
            OffsetDateTime collectedAt
    ) {
        return new DealSummaryResponse(
                representative.getId(),
                representative.getTitle(),
                representative.getPrice(),
                representative.getOriginalPrice(),
                representative.getDiscountRate(),
                representative.getCurrency(),
                representative.getCategory(),
                representative.getShopName(),
                representative.getCommentCount(),
                representative.getThumbnailUrl(),
                representative.getSource().getId(),
                representative.getSource().getName(),
                representative.getDealGroup() == null ? null : representative.getDealGroup().getId(),
                members.size(),
                members.stream()
                        .map(member -> member.getSource().getName())
                        .distinct()
                        .sorted()
                        .toList(),
                postedAt,
                collectedAt,
                status
        );
    }
}
