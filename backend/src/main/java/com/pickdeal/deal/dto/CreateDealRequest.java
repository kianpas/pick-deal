package com.pickdeal.deal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record CreateDealRequest(
        @NotNull Long sourceId,

        @NotBlank
        @Size(max = 300)
        String title,

        String description,

        @PositiveOrZero Long price,

        @PositiveOrZero Long originalPrice,

        @PositiveOrZero Integer discountRate,

        @Size(max = 8)
        String currency,

        @Size(max = 50)
        String category,

        @Size(max = 1000)
        String thumbnailUrl,

        @NotBlank
        @Size(max = 1000)
        String originalUrl,

        @NotBlank
        @Size(max = 200)
        String externalId,

        @NotNull
        OffsetDateTime postedAt
) {
}
