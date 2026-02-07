package com.lol.backend.modules.auth.service;

import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.common.security.JwtTokenProvider;
import com.lol.backend.modules.auth.entity.RefreshToken;
import com.lol.backend.modules.auth.repo.RefreshTokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class AuthTokenProvider {

    private final SecretKey secretKey;
    private final long signupTokenExpiration;
    private final long refreshTokenExpiration;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${auth.signup-token-expiration:300000}") long signupTokenExpiration,
            @Value("${auth.refresh-token-expiration:604800000}") long refreshTokenExpiration,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenRepository refreshTokenRepository) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.signupTokenExpiration = signupTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String createAccessToken(UUID userId) {
        return jwtTokenProvider.createToken(userId.toString());
    }

    public String createSignupToken(String kakaoId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + signupTokenExpiration);

        return Jwts.builder()
                .subject(kakaoId)
                .claim("type", "signup")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String parseSignupToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get("type", String.class);
            if (!"signup".equals(type)) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않은 가입 토큰입니다");
            }

            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않은 가입 토큰입니다");
        }
    }

    public String createRefreshToken(UUID userId) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = sha256(rawToken);

        Instant expiresAt = Instant.now().plusMillis(refreshTokenExpiration);
        RefreshToken refreshToken = new RefreshToken(userId, tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    public void revokeRefreshTokens(UUID userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "해시 생성 실패");
        }
    }
}
