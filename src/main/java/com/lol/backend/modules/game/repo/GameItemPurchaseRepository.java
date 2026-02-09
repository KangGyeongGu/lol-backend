package com.lol.backend.modules.game.repo;

import com.lol.backend.modules.game.entity.GameItemPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface GameItemPurchaseRepository extends JpaRepository<GameItemPurchase, UUID> {

    // 특정 게임/유저의 아이템 구매 총액
    @Query("SELECT COALESCE(SUM(p.totalPrice), 0) FROM GameItemPurchase p WHERE p.gameId = :gameId AND p.userId = :userId")
    int sumTotalPriceByGameIdAndUserId(UUID gameId, UUID userId);

    // 특정 게임/유저의 아이템별 구매 수량 집계 (GROUP BY itemId)
    @Query("SELECT p.itemId, SUM(p.quantity) FROM GameItemPurchase p WHERE p.gameId = :gameId AND p.userId = :userId GROUP BY p.itemId")
    List<Object[]> findItemQuantitiesByGameIdAndUserId(UUID gameId, UUID userId);

    // 특정 게임/유저의 아이템 총 구매 수량
    @Query("SELECT COALESCE(SUM(p.quantity), 0) FROM GameItemPurchase p WHERE p.gameId = :gameId AND p.userId = :userId")
    int sumQuantityByGameIdAndUserId(UUID gameId, UUID userId);
}
