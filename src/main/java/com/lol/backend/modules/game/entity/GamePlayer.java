package com.lol.backend.modules.game.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_player")
public class GamePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GamePlayerState state;

    @Column(name = "score_before", nullable = false)
    private int scoreBefore;

    @Column(name = "score_after")
    private Integer scoreAfter;

    @Column(name = "score_delta")
    private Integer scoreDelta;

    @Column(name = "final_score_value")
    private Integer finalScoreValue;

    @Column(name = "rank_in_game")
    private Integer rankInGame;

    @Column(name = "solved")
    private Boolean solved;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private MatchResult result;

    @Column(name = "coin_delta")
    private Integer coinDelta;

    @Column(name = "exp_delta")
    private Double expDelta;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Column(name = "disconnected_at")
    private Instant disconnectedAt;

    protected GamePlayer() {}

    public GamePlayer(UUID gameId, UUID userId, int scoreBefore) {
        this.gameId = gameId;
        this.userId = userId;
        this.state = GamePlayerState.CONNECTED;
        this.scoreBefore = scoreBefore;
    }

    @PrePersist
    protected void onCreate() {
        this.joinedAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getGameId() { return gameId; }
    public UUID getUserId() { return userId; }
    public GamePlayerState getState() { return state; }
    public int getScoreBefore() { return scoreBefore; }
    public Integer getScoreAfter() { return scoreAfter; }
    public Integer getScoreDelta() { return scoreDelta; }
    public Integer getFinalScoreValue() { return finalScoreValue; }
    public Integer getRankInGame() { return rankInGame; }
    public Boolean getSolved() { return solved; }
    public MatchResult getResult() { return result; }
    public Integer getCoinDelta() { return coinDelta; }
    public Double getExpDelta() { return expDelta; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getLeftAt() { return leftAt; }
    public Instant getDisconnectedAt() { return disconnectedAt; }
}
