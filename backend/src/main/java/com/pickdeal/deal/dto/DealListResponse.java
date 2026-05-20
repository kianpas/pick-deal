package com.pickdeal.deal.dto;

import com.pickdeal.common.response.PageMetaResponse;
import java.util.List;

public record DealListResponse(
        List<DealSummaryResponse> items,
        PageMetaResponse meta
) {
}
