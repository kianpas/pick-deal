package com.pickdeal.preference.application;

import com.pickdeal.common.error.DuplicateResourceException;
import com.pickdeal.common.error.ResourceNotFoundException;
import com.pickdeal.preference.domain.KeywordType;
import com.pickdeal.preference.domain.PreferenceKeyword;
import com.pickdeal.preference.domain.PreferenceKeywordRepository;
import com.pickdeal.preference.dto.CreateKeywordRequest;
import com.pickdeal.preference.dto.KeywordResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreferenceService {

    private static final Long DEFAULT_USER_ID = 1L;

    private final PreferenceKeywordRepository keywordRepository;

    public PreferenceService(PreferenceKeywordRepository keywordRepository) {
        this.keywordRepository = keywordRepository;
    }

    @Transactional(readOnly = true)
    public List<KeywordResponse> findKeywords(KeywordType type) {
        List<PreferenceKeyword> keywords = type == null
                ? keywordRepository.findByUserIdOrderByTypeAscCreatedAtAsc(DEFAULT_USER_ID)
                : keywordRepository.findByUserIdAndTypeOrderByCreatedAtAsc(DEFAULT_USER_ID, type);

        return keywords.stream()
                .map(KeywordResponse::from)
                .toList();
    }

    @Transactional
    public KeywordResponse createKeyword(CreateKeywordRequest request) {
        String keyword = normalize(request.keyword());

        if (keywordRepository.existsByUserIdAndTypeAndKeywordIgnoreCase(DEFAULT_USER_ID, request.type(), keyword)) {
            throw new DuplicateResourceException("Keyword already exists: " + keyword);
        }

        PreferenceKeyword savedKeyword = keywordRepository.save(new PreferenceKeyword(DEFAULT_USER_ID, request.type(), keyword));
        return KeywordResponse.from(savedKeyword);
    }

    @Transactional
    public void deleteKeyword(Long keywordId) {
        PreferenceKeyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new ResourceNotFoundException("Keyword not found: " + keywordId));

        keywordRepository.delete(keyword);
    }

    private String normalize(String value) {
        return value.trim();
    }
}
