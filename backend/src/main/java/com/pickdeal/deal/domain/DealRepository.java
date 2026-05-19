package com.pickdeal.deal.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DealRepository extends JpaRepository<Deal, Long> {

    @Query("select d from Deal d join fetch d.source where d.status = :status and d.source.visible = true")
    List<Deal> findVisibleDealsByStatus(@Param("status") DealStatus status);

    @Query("select d from Deal d join fetch d.source where d.id = :id")
    Optional<Deal> findByIdWithSource(@Param("id") Long id);

    boolean existsBySourceIdAndOriginalUrl(Long sourceId, String originalUrl);
}

