package com.pickdeal.source.dto;

import com.pickdeal.source.domain.Source;

public record SourceDetailResponse(
        Long id,
        String name,
        String baseUrl
) {

    public static SourceDetailResponse from(Source source) {
        return new SourceDetailResponse(source.getId(), source.getName(), source.getBaseUrl());
    }
}

