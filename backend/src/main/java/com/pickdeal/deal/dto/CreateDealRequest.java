package com.pickdeal.deal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.AssertTrue;
import java.time.OffsetDateTime;

public record CreateDealRequest(
        @NotNull Long sourceId,

        @NotBlank
        @Size(max = 300)
        String title,

        String description,

        @PositiveOrZero @Digits(integer = 19, fraction = 2) java.math.BigDecimal price,

        @PositiveOrZero @Digits(integer = 19, fraction = 2) java.math.BigDecimal originalPrice,

        @PositiveOrZero Integer discountRate,

        @Size(max = 8) @Pattern(regexp = "KRW|USD")
        String currency,

        @Size(max = 50)
        String category,

        @Size(max = 100)
        String shopName,

        @Size(max = 1000)
        String thumbnailUrl,

        @NotBlank
        @Size(max = 1000)
        String originalUrl,

        @Size(max = 2000)
        @Pattern(regexp = "^https?://.*", message = "productUrl must use http or https")
        String productUrl,

        @NotBlank
        @Size(max = 200)
        String externalId,

        @NotNull
        OffsetDateTime postedAt
) {
    public CreateDealRequest(Long sourceId, String title, String description, Number price, Number originalPrice,
            Integer discountRate, String currency, String category, String shopName, String thumbnailUrl,
            String originalUrl, String productUrl, String externalId, OffsetDateTime postedAt) {
        this(sourceId, title, description, price == null ? null : new java.math.BigDecimal(price.toString()),
                originalPrice == null ? null : new java.math.BigDecimal(originalPrice.toString()), discountRate,
                currency, category, shopName, thumbnailUrl, originalUrl, productUrl, externalId, postedAt);
    }

    @AssertTrue(message = "KRW prices must be integers")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isCurrencyPriceCompatible() {
        return "USD".equals(currency) || ((price == null || price.stripTrailingZeros().scale() <= 0)
                && (originalPrice == null || originalPrice.stripTrailingZeros().scale() <= 0));
    }
}
