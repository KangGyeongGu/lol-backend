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
@Table(name = "game_player")
public class GamePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GamePlayerState state;

    @Column(name = "score_before", nullable = false)
    private int scoreBefore;

    @Setter
    @Column(name = "score_after")
    private Integer scoreAfter;

    @Setter
    @Column(name = "score_delta")
    private Integer scoreDelta;

    @Setter
    @Column(name = "final_score_value")
    private Integer finalScoreValue;

    @Setter
    @Column(name = "rank_in_game")
    private Integer rankInGame;

    @Setter
    @Column(name = "solved")
    private Boolean solved;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private MatchResult result;

    @Column(name = "coin_before")
    private Integer coinBefore;

    @Setter
    @Column(name = "coin_delta")
    private Integer coinDelta;

    @Column(name = "exp_before")
    private Double expBefore;

    @Setter
    @Column(name = "exp_delta")
    private Double expDelta;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Setter
    @Column(name = "left_at")
    private Instant leftAt;

    @Setter
    @Column(name = "disconnected_at")
    private Instant disconnectedAt;

    public GamePlayer(UUID gameId, UUID userId, int scoreBefore, int coinBefore, double expBefore) {
        this.gameId = gameId;
        this.userId = userId;
        this.state = GamePlayerState.CONNECTED;
        this.scoreBefore = scoreBefore;
        this.coinBefore = coinBefore;
        this.expBefore = expBefore;
    }

    @PrePersist
    protected void onCreate() {
        this.joinedAt = Instant.now();
    }
}
