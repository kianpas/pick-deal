package com.pickdeal.keyword.api;

import com.pickdeal.common.response.ApiResponse;
import com.pickdeal.keyword.application.KeywordService;
import com.pickdeal.keyword.domain.KeywordType;
import com.pickdeal.keyword.dto.CreateKeywordRequest;
import com.pickdeal.keyword.dto.KeywordResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class KeywordController {

    private final KeywordService keywordService;

    @GetMapping
    public ApiResponse<List<KeywordResponse>> findKeywords(@RequestParam(required = false) KeywordType type) {
        return ApiResponse.success(keywordService.findKeywords(type));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<KeywordResponse> createKeyword(@Valid @RequestBody CreateKeywordRequest request) {
        return ApiResponse.success(keywordService.createKeyword(request));
    }

    @DeleteMapping("/{keywordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteKeyword(@PathVariable Long keywordId) {
        keywordService.deleteKeyword(keywordId);
    }
}
