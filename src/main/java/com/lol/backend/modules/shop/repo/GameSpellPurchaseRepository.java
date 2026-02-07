package com.lol.backend.modules.shop.repo;

import com.lol.backend.modules.shop.entity.GameSpellPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface GameSpellPurchaseRepository extends JpaRepository<GameSpellPurchase, UUID> {

    @Query("SELECT COALESCE(SUM(p.quantity), 0) FROM GameSpellPurchase p WHERE p.gameId = :gameId AND p.userId = :userId AND p.spellId = :spellId")
    int sumQuantityByGameIdAndUserIdAndSpellId(UUID gameId, UUID userId, UUID spellId);

    // 특정 게임/유저의 스펠 구매 총액
    @Query("SELECT COALESCE(SUM(p.totalPrice), 0) FROM GameSpellPurchase p WHERE p.gameId = :gameId AND p.userId = :userId")
    int sumTotalPriceByGameIdAndUserId(UUID gameId, UUID userId);

    // 특정 게임/유저의 스펠별 구매 수량 집계 (GROUP BY spellId)
    @Query("SELECT p.spellId, SUM(p.quantity) FROM GameSpellPurchase p WHERE p.gameId = :gameId AND p.userId = :userId GROUP BY p.spellId")
    List<Object[]> findSpellQuantitiesByGameIdAndUserId(UUID gameId, UUID userId);

    // 특정 게임/유저의 스펠 총 구매 수량
    @Query("SELECT COALESCE(SUM(p.quantity), 0) FROM GameSpellPurchase p WHERE p.gameId = :gameId AND p.userId = :userId")
    int sumQuantityByGameIdAndUserId(UUID gameId, UUID userId);
}
