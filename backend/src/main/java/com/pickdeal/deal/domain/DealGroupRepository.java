package com.pickdeal.deal.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DealGroupRepository extends JpaRepository<DealGroup, Long> {

    /** 보관 기간 정리 2단계: 연결된 구성원이 없는 그룹을 삭제한다. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "delete from deal_group where not exists (select 1 from deal d where d.group_id = deal_group.id)",
            nativeQuery = true)
    int deleteGroupsWithoutMembers();
}
