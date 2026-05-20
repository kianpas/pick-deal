package com.pickdeal.deal.api;

import com.pickdeal.common.response.ApiResponse;
import com.pickdeal.deal.application.DealService;
import com.pickdeal.deal.dto.CreateDealRequest;
import com.pickdeal.deal.dto.DealDetailResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/deals")
public class InternalDealController {

    private final DealService dealService;

    public InternalDealController(DealService dealService) {
        this.dealService = dealService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DealDetailResponse> createDeal(@Valid @RequestBody CreateDealRequest request) {
        return ApiResponse.success(dealService.createDeal(request));
    }
}
