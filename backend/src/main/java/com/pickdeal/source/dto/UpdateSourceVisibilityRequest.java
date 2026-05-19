package com.pickdeal.source.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSourceVisibilityRequest(
        @NotNull Boolean visible
) {
}

