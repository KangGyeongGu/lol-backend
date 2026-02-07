package com.lol.backend.modules.game.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "game")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_id", nullable = false, unique = true)
    private UUID roomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false, length = 20)
    private GameType gameType;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameStage stage;

    @Setter
    @Column(name = "stage_started_at")
    private Instant stageStartedAt;

    @Setter
    @Column(name = "stage_deadline_at")
    private Instant stageDeadlineAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Setter
    @Column(name = "finished_at")
    private Instant finishedAt;

    @Setter
    @Column(name = "final_algorithm_id")
    private UUID finalAlgorithmId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Game(UUID roomId, GameType gameType) {
        this.roomId = roomId;
        this.gameType = gameType;
        this.stage = GameStage.LOBBY;
        this.startedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Business Logic Methods

    /**
     * 게임을 다음 stage로 전이한다.
     * @param nextStage 전이할 다음 stage
     * @param deadlineAt stage 마감 시간 (null 가능)
     */
    public void transitionTo(GameStage nextStage, Instant deadlineAt) {
        this.stage = nextStage;
        this.stageStartedAt = Instant.now();
        this.stageDeadlineAt = deadlineAt;
    }

    /**
     * 현재 stage의 남은 시간을 밀리초로 계산한다.
     * @return 남은 시간(ms). deadline이 없거나 이미 지난 경우 0 반환.
     */
    public long calculateRemainingMs() {
        if (stageDeadlineAt == null) {
            return 0L;
        }
        long remaining = stageDeadlineAt.toEpochMilli() - Instant.now().toEpochMilli();
        return Math.max(0L, remaining);
    }

    /**
     * 게임이 종료되었는지 확인한다.
     * @return 게임 종료 여부
     */
    public boolean isFinished() {
        return stage == GameStage.FINISHED;
    }

    /**
     * 게임을 종료 처리한다.
     */
    public void finishGame() {
        this.stage = GameStage.FINISHED;
        this.finishedAt = Instant.now();
    }
}
