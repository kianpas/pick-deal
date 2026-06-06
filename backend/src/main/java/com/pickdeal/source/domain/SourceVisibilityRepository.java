package com.pickdeal.source.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceVisibilityRepository extends JpaRepository<SourceVisibility, Long> {

    /** 사용자의 모든 표시/숨김 설정 — 목록 조회 시 출처별 상태 매핑에 사용. */
    List<SourceVisibility> findByUserId(Long userId);

    /** 특정 출처의 설정 행. 없으면 "표시"가 기본값. */
    Optional<SourceVisibility> findByUserIdAndSourceId(Long userId, Long sourceId);
}
