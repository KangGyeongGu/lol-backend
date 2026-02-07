package com.lol.backend.modules.game.entity;

/**
 * JudgeStatus Enum.
 * DATA_MODEL.md 5.0 공통 Enum 기준.
 */
public enum JudgeStatus {
    AC,  // Accepted
    WA,  // Wrong Answer
    TLE, // Time Limit Exceeded
    MLE, // Memory Limit Exceeded
    CE,  // Compilation Error
    RE   // Runtime Error
}
