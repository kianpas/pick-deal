package com.pickdeal.collector.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;

public final class CollectionRequests {
    private CollectionRequests() {}

    public record KnownIds(
            @NotBlank @Size(max = 50) String sourceCode,
            @NotNull @Size(min = 1, max = 150) List<@NotBlank @Pattern(regexp = "[1-9][0-9]{0,199}") String> externalIds) {}

    public record Batch(
            @NotBlank @Size(max = 50) String sourceCode,
            @NotNull @Size(min = 1, max = 150) List<@NotNull @Valid Item> deals) {}

    public record Item(
            @NotBlank @Pattern(regexp = "[1-9][0-9]{0,199}") String externalId,
            @NotBlank @Size(max = 1000) String originalUrl,
            @Size(max = 100) String shopName,
            @NotBlank @Size(max = 300) String title,
            @PositiveOrZero Long price,
            @Size(max = 50) String category,
            @PositiveOrZero Integer commentCount,
            @Size(max = 1000) String thumbnailUrl,
            Boolean ended,
            OffsetDateTime postedAt,
            @Size(max = 2000) String productUrl) {}
}
