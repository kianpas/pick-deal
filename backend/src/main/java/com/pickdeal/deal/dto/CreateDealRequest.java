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

        @PositiveOrZero Integer price,

        @PositiveOrZero Integer shippingFee,

        @NotBlank
        @Size(max = 1000)
        String originalUrl,

        @Size(max = 200)
        String originalId,

        OffsetDateTime postedAt
) {
}

