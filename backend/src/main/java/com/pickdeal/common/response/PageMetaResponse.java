package com.pickdeal.common.response;

public record PageMetaResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
