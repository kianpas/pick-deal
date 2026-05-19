package com.pickdeal.deal.dto;

import com.pickdeal.deal.domain.Deal;
import com.pickdeal.source.dto.SourceDetailResponse;
import java.time.OffsetDateTime;
import java.util.List;

public record DealDetailResponse(
        Long id,
        String title,
        String description,
        Integer price,
        Integer shippingFee,
        SourceDetailResponse source,
        String originalUrl,
        List<String> matchedInterestKeywords,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static DealDetailResponse from(Deal deal, List<String> matchedInterestKeywords) {
        return new DealDetailResponse(
                deal.getId(),
                deal.getTitle(),
                deal.getDescription(),
                deal.getPrice(),
                deal.getShippingFee(),
                SourceDetailResponse.from(deal.getSource()),
                deal.getOriginalUrl(),
                matchedInterestKeywords,
                deal.getCreatedAt(),
                deal.getUpdatedAt()
        );
    }
}

