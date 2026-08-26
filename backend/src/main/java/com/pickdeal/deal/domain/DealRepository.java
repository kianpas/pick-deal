package com.pickdeal.deal.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DealRepository extends JpaRepository<Deal, Long> {

    /**
     * 사용자가 숨기지 않은 활성 출처의 딜을 조회한다(docs/01 §3.2의 출처 숨김 규칙).
     * 종료/품절 딜도 포함한다 — 목록에서 상태 뱃지로 구분해 보여주는 게 관례라 조용히 숨기지 않는다.
     * 표시/숨김 설정 행이 없으면 "표시"가 기본이므로, {@code visible = false}인 경우만 제외한다.
     * 출처는 fetch join으로 함께 로딩(목록 응답에 출처명이 필요).
     */
    @Query("""
            select d from Deal d
            join fetch d.source s
            left join fetch d.dealGroup g
            left join fetch g.representativeDeal
            where s.active = true
              and not exists (
                    select sv.id from SourceVisibility sv
                    where sv.userId = :userId
                      and sv.source = s
                      and sv.visible = false
              )
            """)
    List<Deal> findVisibleDeals(@Param("userId") Long userId);

    /** 상세 조회용. 출처를 fetch join해 N+1을 피한다. */
    @Query("select d from Deal d join fetch d.source left join fetch d.dealGroup where d.id = :id")
    Optional<Deal> findByIdWithSource(@Param("id") Long id);

    /** 상세 화면의 교차 출처 원문 목록용. 출처 표시 설정과 무관하게 그룹 원본을 모두 보존해 보여준다. */
    @Query("select d from Deal d join fetch d.source where d.dealGroup.id = :groupId")
    List<Deal> findByDealGroupIdWithSource(@Param("groupId") Long groupId);

    /** 중복 수집 방지용 — ({@code sourceId}, {@code externalId}) 유니크 제약과 짝을 이룬다. */
    boolean existsBySourceIdAndExternalId(Long sourceId, String externalId);

    /** 출처의 최초 수집 여부 판별용 — 딜이 하나라도 있으면 증분 수집한다. */
    boolean existsBySourceId(Long sourceId);

    /** 재수집 시 기존 딜 갱신용 조회. */
    Optional<Deal> findBySourceIdAndExternalId(Long sourceId, String externalId);

    /** 정규화 제목이 정확히 같은 다른 출처 Deal을 1차 후보로 찾는다. */
    @Query("""
            select d from Deal d
            left join fetch d.dealGroup
            where d.source.id <> :sourceId
              and d.titleNormHash = :titleNormHash
            """)
    List<Deal> findCrossSourceCandidates(
            @Param("sourceId") Long sourceId,
            @Param("titleNormHash") String titleNormHash
    );
}
