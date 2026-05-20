package com.pickdeal.source.dto;

import com.pickdeal.source.domain.Source;

public record SourceResponse(
        Long id,
        String name,
        String baseUrl,
        boolean visible
) {

    public static SourceResponse from(Source source, boolean visible) {
        return new SourceResponse(
                source.getId(),
                source.getName(),
                source.getBaseUrl(),
                visible
        );
    }
}
