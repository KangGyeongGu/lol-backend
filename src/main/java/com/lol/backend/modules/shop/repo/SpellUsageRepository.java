package com.lol.backend.modules.shop.repo;

import com.lol.backend.modules.shop.entity.SpellUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SpellUsageRepository extends JpaRepository<SpellUsage, UUID> {

    long countByGameIdAndUserIdAndSpellId(UUID gameId, UUID userId, UUID spellId);

    // 특정 게임/유저의 스펠별 사용 횟수 집계
    @Query("SELECT u.spellId, COUNT(u) FROM SpellUsage u WHERE u.gameId = :gameId AND u.userId = :userId GROUP BY u.spellId")
    List<Object[]> findSpellUsageCountsByGameIdAndUserId(UUID gameId, UUID userId);
}
