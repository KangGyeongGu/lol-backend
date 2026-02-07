package com.lol.backend.modules.game.entity;

import com.lol.backend.modules.user.entity.Language;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * SUBMISSION 엔티티.
 * DATA_MODEL.md 5.12 기준.
 * storage: persistent (코드 제출 기록 보관)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "submission")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Language language;

    @Column(name = "source_code", nullable = false, columnDefinition = "TEXT")
    private String sourceCode;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "submitted_elapsed_ms", nullable = false)
    private int submittedElapsedMs;

    @Column(name = "exec_time_ms", nullable = false)
    private int execTimeMs;

    @Column(name = "memory_kb", nullable = false)
    private int memoryKb;

    @Enumerated(EnumType.STRING)
    @Column(name = "judge_status", nullable = false, length = 10)
    private JudgeStatus judgeStatus;

    @Column(name = "judge_detail_json", columnDefinition = "JSONB")
    private String judgeDetailJson;

    @Column(name = "score_value")
    private Integer scoreValue;

    public Submission(UUID gameId, UUID userId, Language language, String sourceCode,
                      int submittedElapsedMs, int execTimeMs, int memoryKb,
                      JudgeStatus judgeStatus, String judgeDetailJson, Integer scoreValue) {
        this.gameId = gameId;
        this.userId = userId;
        this.language = language;
        this.sourceCode = sourceCode;
        this.submittedElapsedMs = submittedElapsedMs;
        this.execTimeMs = execTimeMs;
        this.memoryKb = memoryKb;
        this.judgeStatus = judgeStatus;
        this.judgeDetailJson = judgeDetailJson;
        this.scoreValue = scoreValue;
    }

    @PrePersist
    protected void onCreate() {
        this.submittedAt = Instant.now();
    }
}
