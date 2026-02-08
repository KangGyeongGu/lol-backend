package com.lol.backend.modules.shop.service;

import com.lol.backend.modules.shop.entity.GameBan;
import com.lol.backend.modules.shop.entity.GamePick;
import com.lol.backend.modules.shop.repo.GameBanRepository;
import com.lol.backend.modules.shop.repo.GamePickRepository;
import com.lol.backend.state.snapshot.BanPickSnapshotContributor;
import com.lol.backend.state.store.BanPickStateStore;
import com.lol.backend.state.dto.GameBanDto;
import com.lol.backend.state.dto.GamePickDto;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BanPickSnapshotContributorImpl implements BanPickSnapshotContributor {

    private final GameBanRepository gameBanRepository;
    private final GamePickRepository gamePickRepository;
    private final BanPickStateStore banPickStateStore;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void persistBanPickSnapshot(UUID gameId) {
        log.debug("Persisting ban/pick snapshot: gameId={}", gameId);

        // Redis에서 밴 데이터 조회 및 DB 저장
        List<GameBanDto> bans = banPickStateStore.getBans(gameId);
        for (GameBanDto banDto : bans) {
            // 이미 저장되어 있는지 확인 (중복 방지)
            if (!gameBanRepository.existsById(banDto.id())) {
                GameBan gameBan = GameBan.restore(
                        banDto.id(),
                        banDto.gameId(),
                        banDto.userId(),
                        banDto.algorithmId(),
                        banDto.createdAt()
                );
                entityManager.persist(gameBan);
                log.debug("Ban saved to DB: id={}, gameId={}, userId={}", banDto.id(), banDto.gameId(), banDto.userId());
            }
        }

        // Redis에서 픽 데이터 조회 및 DB 저장
        List<GamePickDto> picks = banPickStateStore.getPicks(gameId);
        for (GamePickDto pickDto : picks) {
            // 이미 저장되어 있는지 확인 (중복 방지)
            if (!gamePickRepository.existsById(pickDto.id())) {
                GamePick gamePick = GamePick.restore(
                        pickDto.id(),
                        pickDto.gameId(),
                        pickDto.userId(),
                        pickDto.algorithmId(),
                        pickDto.createdAt()
                );
                entityManager.persist(gamePick);
                log.debug("Pick saved to DB: id={}, gameId={}, userId={}", pickDto.id(), pickDto.gameId(), pickDto.userId());
            }
        }

        log.debug("Ban/pick snapshot persisted successfully: gameId={}", gameId);
    }
}
