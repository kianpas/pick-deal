package com.pickdeal.deal.api;

import com.pickdeal.common.response.ApiResponse;
import com.pickdeal.deal.application.DealService;
import com.pickdeal.deal.dto.CreateDealRequest;
import com.pickdeal.deal.dto.DealDetailResponse;
import com.pickdeal.deal.dto.DealListResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/deals")
public class DealController {

    private final DealService dealService;

    public DealController(DealService dealService) {
        this.dealService = dealService;
    }

    @GetMapping
    public ApiResponse<DealListResponse> findDeals(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) String q
    ) {
        return ApiResponse.success(dealService.findDeals(page, size, sourceId, q));
    }

    @GetMapping("/{dealId}")
    public ApiResponse<DealDetailResponse> findDeal(@PathVariable Long dealId) {
        return ApiResponse.success(dealService.findDeal(dealId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DealDetailResponse> createDeal(@Valid @RequestBody CreateDealRequest request) {
        return ApiResponse.success(dealService.createDeal(request));
    }
}

