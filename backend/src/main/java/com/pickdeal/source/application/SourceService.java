package com.pickdeal.source.application;

import com.pickdeal.common.error.ResourceNotFoundException;
import com.pickdeal.source.domain.Source;
import com.pickdeal.source.domain.SourceRepository;
import com.pickdeal.source.dto.SourceResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourceService {

    private final SourceRepository sourceRepository;

    public SourceService(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @Transactional(readOnly = true)
    public List<SourceResponse> findSources() {
        return sourceRepository.findAllByOrderByIdAsc().stream()
                .map(SourceResponse::from)
                .toList();
    }

    @Transactional
    public SourceResponse updateVisibility(Long sourceId, boolean visible) {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Source not found: " + sourceId));

        source.updateVisibility(visible);
        return SourceResponse.from(source);
    }
}

