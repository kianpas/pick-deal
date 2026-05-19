package com.pickdeal.source.dto;

import com.pickdeal.source.domain.Source;
import java.time.OffsetDateTime;

public record SourceResponse(
        Long id,
        String name,
        String baseUrl,
        String description,
        boolean visible,
        OffsetDateTime createdAt
) {

    public static SourceResponse from(Source source) {
        return new SourceResponse(
                source.getId(),
                source.getName(),
                source.getBaseUrl(),
                source.getDescription(),
                source.isVisible(),
                source.getCreatedAt()
        );
    }
}

