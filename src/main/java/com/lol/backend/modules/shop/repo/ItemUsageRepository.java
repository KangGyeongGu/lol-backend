package com.lol.backend.modules.shop.repo;

import com.lol.backend.modules.shop.entity.ItemUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ItemUsageRepository extends JpaRepository<ItemUsage, UUID> {

    // 특정 게임/유저가 사용한 특정 아이템 횟수
    long countByGameIdAndFromUserIdAndItemId(UUID gameId, UUID fromUserId, UUID itemId);

    // 특정 게임/유저의 아이템별 사용 횟수 집계
    @Query("SELECT u.itemId, COUNT(u) FROM ItemUsage u WHERE u.gameId = :gameId AND u.fromUserId = :fromUserId GROUP BY u.itemId")
    List<Object[]> findItemUsageCountsByGameIdAndFromUserId(UUID gameId, UUID fromUserId);
}
