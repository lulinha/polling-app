package com.example.polls.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationInMs}")
    private int jwtExpirationInMs;

    // 将 jwtSecret 转换为 SecretKey
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(Long.toString(userPrincipal.getId())) // 设置主题
                .issuedAt(now) // 设置签发时间
                .expiration(expiryDate) // 设置过期时间
                .signWith(getSigningKey(), Jwts.SIG.HS512) // 使用 HMAC-SHA512 签名
                .compact(); // 生成 JWT 字符串
    }

    public Long getUserIdFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey()) // 设置签名密钥
                .build()
                .parseSignedClaims(token) // 解析并验证 JWT
                .getPayload(); // 获取 Claims
        return Long.parseLong(claims.getSubject()); // 返回用户 ID
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey()) // 设置签名密钥
                .build()
                .parseSignedClaims(authToken); // 解析并验证 JWT
            return true;
        } catch (JwtException ex) {
            logger.error("JWT validation error: {}", ex.getMessage());
            return false;
        }
    }
}