package com.lol.backend.modules.game.repo;

import com.lol.backend.modules.game.entity.JudgeStatus;
import com.lol.backend.modules.game.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    /**
     * 특정 게임의 AC(정답) 제출만 조회한다.
     * @param gameId 게임 ID
     * @param judgeStatus 채점 상태
     * @return AC 제출 목록
     */
    List<Submission> findByGameIdAndJudgeStatus(UUID gameId, JudgeStatus judgeStatus);
}
