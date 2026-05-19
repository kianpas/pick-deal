package com.pickdeal.deal.dto;

import java.util.List;

public record DealListResponse(
        List<DealSummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}

