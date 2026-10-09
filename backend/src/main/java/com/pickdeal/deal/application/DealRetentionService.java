package com.pickdeal.deal.application;

import com.pickdeal.deal.domain.DealGroupRepository;
import com.pickdeal.deal.domain.DealRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시 시각이 보관 기간을 넘은 Deal을 삭제한다. 그룹은 모든 구성원이 기준보다 오래됐을 때만
 * 그룹째 삭제하고, 세 단계를 한 트랜잭션에서 실행해 일부만 지워진 상태를 남기지 않는다.
 */
@Service
@RequiredArgsConstructor
public class DealRetentionService {

    private final DealRepository dealRepository;
    private final DealGroupRepository dealGroupRepository;

    @Transactional
    public Result purgeOlderThan(OffsetDateTime cutoff) {
        dealRepository.detachExpiredGroups(cutoff);
        int deletedGroups = dealGroupRepository.deleteGroupsWithoutMembers();
        int deletedDeals = dealRepository.deleteExpiredUngrouped(cutoff);
        return new Result(deletedDeals, deletedGroups);
    }

    public record Result(int deletedDeals, int deletedGroups) {
    }
}
