package com.pickdeal.keyword.dto;

import com.pickdeal.keyword.domain.KeywordType;
import com.pickdeal.keyword.domain.Keyword;
import java.time.OffsetDateTime;

public record KeywordResponse(
        Long id,
        String keyword,
        KeywordType type,
        OffsetDateTime createdAt
) {

    public static KeywordResponse from(Keyword keyword) {
        return new KeywordResponse(
                keyword.getId(),
                keyword.getKeyword(),
                keyword.getType(),
                keyword.getCreatedAt()
        );
    }
}
