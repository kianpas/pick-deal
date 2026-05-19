package com.pickdeal.common.response;

public record DeleteResponse(
        Long id,
        boolean deleted
) {
}

