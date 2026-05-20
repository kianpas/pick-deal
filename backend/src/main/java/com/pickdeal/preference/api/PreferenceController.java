package com.pickdeal.preference.api;

import com.pickdeal.common.response.ApiResponse;
import com.pickdeal.preference.application.PreferenceService;
import com.pickdeal.preference.domain.KeywordType;
import com.pickdeal.preference.dto.CreateKeywordRequest;
import com.pickdeal.preference.dto.KeywordResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/keywords")
public class PreferenceController {

    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public ApiResponse<List<KeywordResponse>> findKeywords(@RequestParam(required = false) KeywordType type) {
        return ApiResponse.success(preferenceService.findKeywords(type));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<KeywordResponse> createKeyword(@Valid @RequestBody CreateKeywordRequest request) {
        return ApiResponse.success(preferenceService.createKeyword(request));
    }

    @DeleteMapping("/{keywordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteKeyword(@PathVariable Long keywordId) {
        preferenceService.deleteKeyword(keywordId);
    }
}
