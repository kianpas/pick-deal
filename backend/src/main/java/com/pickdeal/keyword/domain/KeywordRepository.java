package com.pickdeal.keyword.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    List<Keyword> findByUserIdOrderByTypeAscCreatedAtAsc(Long userId);

    List<Keyword> findByUserIdAndTypeOrderByCreatedAtAsc(Long userId, KeywordType type);

    boolean existsByUserIdAndTypeAndKeywordIgnoreCase(Long userId, KeywordType type, String keyword);
}
