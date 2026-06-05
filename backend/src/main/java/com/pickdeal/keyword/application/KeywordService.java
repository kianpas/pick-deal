package com.pickdeal.keyword.application;

import com.pickdeal.common.error.DuplicateResourceException;
import com.pickdeal.common.error.ResourceNotFoundException;
import com.pickdeal.keyword.domain.KeywordType;
import com.pickdeal.keyword.domain.Keyword;
import com.pickdeal.keyword.domain.KeywordRepository;
import com.pickdeal.keyword.dto.CreateKeywordRequest;
import com.pickdeal.keyword.dto.KeywordResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KeywordService {

    private static final Long DEFAULT_USER_ID = 1L;

    private final KeywordRepository keywordRepository;

    public KeywordService(KeywordRepository keywordRepository) {
        this.keywordRepository = keywordRepository;
    }

    @Transactional(readOnly = true)
    public List<KeywordResponse> findKeywords(KeywordType type) {
        List<Keyword> keywords = type == null
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

        Keyword savedKeyword = keywordRepository.save(new Keyword(DEFAULT_USER_ID, request.type(), keyword));
        return KeywordResponse.from(savedKeyword);
    }

    @Transactional
    public void deleteKeyword(Long keywordId) {
        Keyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new ResourceNotFoundException("Keyword not found: " + keywordId));

        keywordRepository.delete(keyword);
    }

    private String normalize(String value) {
        return value.trim();
    }
}
