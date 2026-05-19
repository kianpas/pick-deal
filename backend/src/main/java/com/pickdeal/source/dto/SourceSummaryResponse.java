package com.pickdeal.source.dto;

import com.pickdeal.source.domain.Source;

public record SourceSummaryResponse(
        Long id,
        String name
) {

    public static SourceSummaryResponse from(Source source) {
        return new SourceSummaryResponse(source.getId(), source.getName());
    }
}

