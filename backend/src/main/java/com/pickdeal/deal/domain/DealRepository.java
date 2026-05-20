package com.pickdeal.deal.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DealRepository extends JpaRepository<Deal, Long> {

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

    @Query("select d from Deal d join fetch d.source where d.id = :id")
    Optional<Deal> findByIdWithSource(@Param("id") Long id);

    boolean existsBySourceIdAndExternalId(Long sourceId, String externalId);
}
