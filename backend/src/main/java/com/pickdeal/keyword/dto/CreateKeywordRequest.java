package com.pickdeal.keyword.dto;

import com.pickdeal.keyword.domain.KeywordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateKeywordRequest(
        @NotNull KeywordType type,

        @NotBlank
        @Size(max = 50)
        String keyword
) {
}
