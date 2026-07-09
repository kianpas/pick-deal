package com.pickdeal.source.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceRepository extends JpaRepository<Source, Long> {

    /** 수집 활성화된 출처 목록(등록 순). 출처 목록 화면에 노출할 후보. */
    List<Source> findByActiveTrueOrderByIdAsc();

    /** 코드 키로 출처 조회 — 수집기가 자기 출처를 식별할 때 쓴다. */
    Optional<Source> findByCode(String code);
}
