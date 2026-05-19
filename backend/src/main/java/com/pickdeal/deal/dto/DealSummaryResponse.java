package com.pickdeal.deal.dto;

import com.pickdeal.deal.domain.Deal;
import com.pickdeal.source.dto.SourceSummaryResponse;
import java.time.OffsetDateTime;
import java.util.List;

public record DealSummaryResponse(
        Long id,
        String title,
        Integer price,
        Integer shippingFee,
        SourceSummaryResponse source,
        String originalUrl,
        List<String> matchedInterestKeywords,
        OffsetDateTime createdAt
) {

    public static DealSummaryResponse from(Deal deal, List<String> matchedInterestKeywords) {
        return new DealSummaryResponse(
                deal.getId(),
                deal.getTitle(),
                deal.getPrice(),
                deal.getShippingFee(),
                SourceSummaryResponse.from(deal.getSource()),
                deal.getOriginalUrl(),
                matchedInterestKeywords,
                deal.getCreatedAt()
        );
    }
}

