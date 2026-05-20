package com.pickdeal.preference.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreferenceKeywordRepository extends JpaRepository<PreferenceKeyword, Long> {

    List<PreferenceKeyword> findByUserIdOrderByTypeAscCreatedAtAsc(Long userId);

    List<PreferenceKeyword> findByUserIdAndTypeOrderByCreatedAtAsc(Long userId, KeywordType type);

    boolean existsByUserIdAndTypeAndKeywordIgnoreCase(Long userId, KeywordType type, String keyword);
}
