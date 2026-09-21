package com.pickdeal.deal.api;

import com.pickdeal.common.response.ApiResponse;
import com.pickdeal.deal.application.DealService;
import com.pickdeal.deal.dto.DealDetailResponse;
import com.pickdeal.deal.dto.DealListResponse;
import com.pickdeal.deal.dto.DealSummaryResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/deals")
@RequiredArgsConstructor
public class DealController {

    private final DealService dealService;

    @GetMapping
    public ApiResponse<List<DealSummaryResponse>> findDeals(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) List<Long> sourceId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam MultiValueMap<String, String> queryParams
    ) {
        // 판매처 이름에 쉼표가 있어도 분리하지 않고 반복 파라미터만 읽는다.
        DealListResponse response = dealService.findDeals(page, size, sort, sourceId, category, q, queryParams.get("shopName"));
        return ApiResponse.success(response.items(), response.meta());
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> findCategories() {
        return ApiResponse.success(dealService.findCategories());
    }

    @GetMapping("/shops")
    public ApiResponse<List<String>> findShops() {
        return ApiResponse.success(dealService.findShops());
    }

    @GetMapping("/{dealId}")
    public ApiResponse<DealDetailResponse> findDeal(@PathVariable Long dealId) {
        return ApiResponse.success(dealService.findDeal(dealId));
    }

}
