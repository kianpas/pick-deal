package com.pickdeal.preference.dto;

import com.pickdeal.preference.domain.KeywordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateKeywordRequest(
        @NotNull KeywordType type,

        @NotBlank
        @Size(max = 100)
        String value
) {
}

