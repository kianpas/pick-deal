package com.pickdeal.keyword.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    /** 사용자의 전체 키워드 — type 미지정 목록 조회용. */
    List<Keyword> findByUserIdOrderByTypeAscCreatedAtAsc(Long userId);

    /** 특정 타입(관심/제외) 키워드 — 목록 조회 및 딜 필터링에 사용. */
    List<Keyword> findByUserIdAndTypeOrderByCreatedAtAsc(Long userId, KeywordType type);

    /** 중복 등록 방지용(대소문자 무시). */
    boolean existsByUserIdAndTypeAndKeywordIgnoreCase(Long userId, KeywordType type, String keyword);
}
