package com.pickdeal.source.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceRepository extends JpaRepository<Source, Long> {

    /** 수집 활성화된 출처 목록(등록 순). 출처 목록 화면에 노출할 후보. */
    List<Source> findByActiveTrueOrderByIdAsc();
}
