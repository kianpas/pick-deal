package com.pickdeal.source.application;

import com.pickdeal.common.error.ResourceNotFoundException;
import com.pickdeal.source.domain.Source;
import com.pickdeal.source.domain.SourceRepository;
import com.pickdeal.source.domain.SourceVisibility;
import com.pickdeal.source.domain.SourceVisibilityRepository;
import com.pickdeal.source.dto.SourceResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SourceService {

    private static final Long DEFAULT_USER_ID = 1L;

    private final SourceRepository sourceRepository;
    private final SourceVisibilityRepository sourceVisibilityRepository;

    @Transactional(readOnly = true)
    public List<SourceResponse> findSources() {
        Map<Long, SourceVisibility> visibilityBySourceId = sourceVisibilityRepository.findByUserId(DEFAULT_USER_ID).stream()
                .collect(Collectors.toMap(visibility -> visibility.getSource().getId(), Function.identity()));

        return sourceRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(source -> SourceResponse.from(source, visibleFor(source, visibilityBySourceId)))
                .toList();
    }

    @Transactional
    public SourceResponse updateVisibility(Long sourceId, boolean visible) {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Source not found: " + sourceId));

        SourceVisibility sourceVisibility = sourceVisibilityRepository.findByUserIdAndSourceId(DEFAULT_USER_ID, sourceId)
                .orElseGet(() -> new SourceVisibility(DEFAULT_USER_ID, source, true));
        sourceVisibility.updateVisible(visible);
        sourceVisibilityRepository.save(sourceVisibility);

        return SourceResponse.from(source, visible);
    }

    private boolean visibleFor(Source source, Map<Long, SourceVisibility> visibilityBySourceId) {
        SourceVisibility sourceVisibility = visibilityBySourceId.get(source.getId());
        return sourceVisibility == null || sourceVisibility.isVisible();
    }
}
