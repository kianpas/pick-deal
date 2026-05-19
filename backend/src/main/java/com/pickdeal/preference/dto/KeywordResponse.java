package com.pickdeal.preference.dto;

import com.pickdeal.preference.domain.KeywordType;
import com.pickdeal.preference.domain.PreferenceKeyword;
import java.time.OffsetDateTime;

public record KeywordResponse(
        Long id,
        KeywordType type,
        String value,
        OffsetDateTime createdAt
) {

    public static KeywordResponse from(PreferenceKeyword keyword) {
        return new KeywordResponse(
                keyword.getId(),
                keyword.getType(),
                keyword.getValue(),
                keyword.getCreatedAt()
        );
    }
}

