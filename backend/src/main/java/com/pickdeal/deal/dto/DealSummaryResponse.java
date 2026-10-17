package com.pickdeal.deal.dto;

import com.pickdeal.deal.domain.Deal;
import com.pickdeal.deal.domain.DealCategory;
import java.time.OffsetDateTime;
import java.util.List;

public record DealSummaryResponse(
        Long id,
        String title,
        java.math.BigDecimal price,
        java.math.BigDecimal originalPrice,
        Integer discountRate,
        String currency,
        String category,
        String categoryCode,
        String categoryName,
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
        String status,
        /** 그룹 구성원 중 하나라도 포함한 관심 키워드(등록 순). 관심 키워드가 없으면 빈 목록. */
        List<String> matchedKeywords
) {

    public static DealSummaryResponse from(Deal deal) {
        return from(deal, List.of(deal), deal.getStatus().name(), deal.getPostedAt(), deal.getCollectedAt(), List.of());
    }

    public static DealSummaryResponse from(
            Deal representative,
            List<Deal> members,
            String status,
            OffsetDateTime postedAt,
            OffsetDateTime collectedAt,
            List<String> matchedKeywords
    ) {
        return new DealSummaryResponse(
                representative.getId(),
                representative.getTitle(),
                representative.getPrice(),
                representative.getOriginalPrice(),
                representative.getDiscountRate(),
                representative.getCurrency(),
                representative.getCategory(),
                DealCategory.from(representative).name(),
                DealCategory.from(representative).label(),
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
                status,
                List.copyOf(matchedKeywords)
        );
    }
}
