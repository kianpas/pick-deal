package com.pickdeal.common.response;

public record ErrorResponse(
        String code,
        String message
) {
}

