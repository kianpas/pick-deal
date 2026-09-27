package com.pickdeal.deal.dto;

import com.pickdeal.deal.domain.DealCategory;

public record DealCategoryResponse(String code, String name) {
    public static DealCategoryResponse from(DealCategory category) {
        return new DealCategoryResponse(category.name(), category.label());
    }
}
