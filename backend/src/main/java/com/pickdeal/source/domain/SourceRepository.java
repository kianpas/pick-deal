package com.pickdeal.source.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceRepository extends JpaRepository<Source, Long> {

    List<Source> findAllByOrderByIdAsc();
}

