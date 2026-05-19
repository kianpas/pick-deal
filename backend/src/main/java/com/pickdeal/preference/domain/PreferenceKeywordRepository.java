package com.pickdeal.preference.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreferenceKeywordRepository extends JpaRepository<PreferenceKeyword, Long> {

    List<PreferenceKeyword> findAllByOrderByTypeAscCreatedAtAsc();

    List<PreferenceKeyword> findByTypeOrderByCreatedAtAsc(KeywordType type);

    boolean existsByTypeAndValueIgnoreCase(KeywordType type, String value);
}

