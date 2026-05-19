package com.pickdeal.preference.application;

import com.pickdeal.common.error.DuplicateResourceException;
import com.pickdeal.common.error.ResourceNotFoundException;
import com.pickdeal.common.response.DeleteResponse;
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

    private final PreferenceKeywordRepository keywordRepository;

    public PreferenceService(PreferenceKeywordRepository keywordRepository) {
        this.keywordRepository = keywordRepository;
    }

    @Transactional(readOnly = true)
    public List<KeywordResponse> findKeywords(KeywordType type) {
        List<PreferenceKeyword> keywords = type == null
                ? keywordRepository.findAllByOrderByTypeAscCreatedAtAsc()
                : keywordRepository.findByTypeOrderByCreatedAtAsc(type);

        return keywords.stream()
                .map(KeywordResponse::from)
                .toList();
    }

    @Transactional
    public KeywordResponse createKeyword(CreateKeywordRequest request) {
        String value = normalize(request.value());

        if (keywordRepository.existsByTypeAndValueIgnoreCase(request.type(), value)) {
            throw new DuplicateResourceException("Keyword already exists: " + value);
        }

        PreferenceKeyword keyword = keywordRepository.save(new PreferenceKeyword(request.type(), value));
        return KeywordResponse.from(keyword);
    }

    @Transactional
    public DeleteResponse deleteKeyword(Long keywordId) {
        PreferenceKeyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new ResourceNotFoundException("Keyword not found: " + keywordId));

        keywordRepository.delete(keyword);
        return new DeleteResponse(keywordId, true);
    }

    private String normalize(String value) {
        return value.trim();
    }
}

