package com.pickdeal.source.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceVisibilityRepository extends JpaRepository<SourceVisibility, Long> {

    List<SourceVisibility> findByUserId(Long userId);

    Optional<SourceVisibility> findByUserIdAndSourceId(Long userId, Long sourceId);
}
