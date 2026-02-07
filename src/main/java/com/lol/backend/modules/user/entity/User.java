package com.lol.backend.modules.user.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "kakao_id", nullable = false, unique = true)
    private String kakaoId;

    @Column(nullable = false, unique = true, length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Language language;

    @Column(nullable = false, length = 30)
    private String tier = "Iron";

    @Column(nullable = false)
    private int score = 0;

    @Column(nullable = false)
    private double exp = 0.0;

    @Column(nullable = false)
    private int coin = 1000;

    @Column(name = "active_game_id")
    private UUID activeGameId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    private User(String kakaoId, String nickname, Language language) {
        this.kakaoId = kakaoId;
        this.nickname = nickname;
        this.language = language;
        this.tier = calculateTier(0);
    }

    public static User create(String kakaoId, String nickname, Language language) {
        return new User(kakaoId, nickname, language);
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        this.tier = calculateTier(this.score);
    }

    /**
     * 점수 기반 티어 계산.
     * Iron: 0~299, Bronze V~I: 300~799, Silver V~I: 800~1299,
     * Gold V~I: 1300~1799, Platinum V~I: 1800~2299, Diamond V~I: 2300~2799,
     * Master: 2800~2999, Grandmaster: 3000~3199, Challenger: 3200+
     */
    public static String calculateTier(int score) {
        if (score < 300) return "Iron";
        if (score >= 3200) return "Challenger";
        if (score >= 3000) return "Grandmaster";
        if (score >= 2800) return "Master";

        String[] tierNames = {"Bronze", "Silver", "Gold", "Platinum", "Diamond"};
        String[] divisions = {"V", "IV", "III", "II", "I"};

        int tierIndex = (score - 300) / 500;
        int divisionIndex = ((score - 300) % 500) / 100;

        return tierNames[tierIndex] + " " + divisions[divisionIndex];
    }

    // Getters
    public UUID getId() { return id; }
    public String getKakaoId() { return kakaoId; }
    public String getNickname() { return nickname; }
    public Language getLanguage() { return language; }
    public String getTier() { return tier; }
    public int getScore() { return score; }
    public double getExp() { return exp; }
    public int getCoin() { return coin; }
    public UUID getActiveGameId() { return activeGameId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters for cross-module use
    public void setActiveGameId(UUID activeGameId) {
        this.activeGameId = activeGameId;
    }
}
