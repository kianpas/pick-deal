package com.pickdeal.deal.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DealRepository extends JpaRepository<Deal, Long> {

    /**
     * 주어진 상태의 딜 중, 사용자가 숨기지 않은 활성 출처의 딜만 조회한다(docs/01 §3.2의 출처 숨김 규칙).
     * 표시/숨김 설정 행이 없으면 "표시"가 기본이므로, {@code visible = false}인 경우만 제외한다.
     * 출처는 fetch join으로 함께 로딩(목록 응답에 출처명이 필요).
     */
    @Query("""
            select d from Deal d
            join fetch d.source s
            where d.status = :status
              and s.active = true
              and not exists (
                    select sv.id from SourceVisibility sv
                    where sv.userId = :userId
                      and sv.source = s
                      and sv.visible = false
              )
            """)
    List<Deal> findVisibleDealsByStatus(@Param("status") DealStatus status, @Param("userId") Long userId);

    /** 상세 조회용. 출처를 fetch join해 N+1을 피한다. */
    @Query("select d from Deal d join fetch d.source where d.id = :id")
    Optional<Deal> findByIdWithSource(@Param("id") Long id);

    /** 중복 수집 방지용 — ({@code sourceId}, {@code externalId}) 유니크 제약과 짝을 이룬다. */
    boolean existsBySourceIdAndExternalId(Long sourceId, String externalId);
}
