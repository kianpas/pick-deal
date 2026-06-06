package com.pickdeal.source.api;

import com.pickdeal.common.response.ApiResponse;
import com.pickdeal.source.application.SourceService;
import com.pickdeal.source.dto.SourceResponse;
import com.pickdeal.source.dto.UpdateSourceVisibilityRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sources")
@RequiredArgsConstructor
public class SourceController {

    private final SourceService sourceService;

    @GetMapping
    public ApiResponse<List<SourceResponse>> findSources() {
        return ApiResponse.success(sourceService.findSources());
    }

    @PatchMapping("/{sourceId}/visibility")
    public ApiResponse<SourceResponse> updateVisibility(
            @PathVariable Long sourceId,
            @Valid @RequestBody UpdateSourceVisibilityRequest request
    ) {
        return ApiResponse.success(sourceService.updateVisibility(sourceId, request.visible()));
    }
}
