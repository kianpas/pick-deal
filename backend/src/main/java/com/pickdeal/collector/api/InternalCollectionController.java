package com.pickdeal.collector.api;

import com.pickdeal.collector.application.RemoteCollectionService;
import com.pickdeal.collector.dto.CollectionRequests;
import com.pickdeal.collector.dto.CollectionResponses;
import com.pickdeal.common.error.BusinessException;
import com.pickdeal.common.error.ErrorCode;
import com.pickdeal.common.response.ApiResponse;
import com.pickdeal.config.CollectorIngressFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(CollectorIngressFilter.ROOT)
public class InternalCollectionController {
    private final RemoteCollectionService service;

    public InternalCollectionController(RemoteCollectionService service) { this.service = service; }

    @PostMapping("/known-external-ids")
    public ApiResponse<CollectionResponses.KnownIds> known(@Valid @RequestBody CollectionRequests.KnownIds body,
            HttpServletRequest request) {
        requireAuthentication(request);
        return ApiResponse.success(service.known(body));
    }

    @PostMapping
    public ApiResponse<CollectionResponses.Accepted> accept(@Valid @RequestBody CollectionRequests.Batch body,
            HttpServletRequest request) {
        requireAuthentication(request);
        return ApiResponse.success(service.accept(body));
    }

    private static void requireAuthentication(HttpServletRequest request) {
        if (!CollectorIngressFilter.isAuthenticated(request)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "수집기 인증이 필요합니다.");
        }
    }
}
