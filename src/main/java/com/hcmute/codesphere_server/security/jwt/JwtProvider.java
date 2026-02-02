package com.hcmute.codesphere_server.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class JwtProvider {

    @Value("${app.jwt.secret:CodeSphere2025SecretKeyForJWTTokenGenerationMustBeAtLeast32CharactersLong}")
    private String SECRET;

    @Value("${app.jwt.expiration:86400000}")
    private long EXPIRATION;

    private javax.crypto.SecretKey getSigningKey() {
        // Đảm bảo secret key đủ dài (ít nhất 32 ký tự = 256 bits)
        byte[] keyBytes = SECRET.getBytes();
        if (keyBytes.length < 32) {
            // Nếu quá ngắn, pad với zeros (hoặc throw exception)
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
            return Keys.hmacShaKeyFor(paddedKey);
        }
        // Nếu quá dài, chỉ lấy 32 bytes đầu
        if (keyBytes.length > 32) {
            byte[] truncatedKey = new byte[32];
            System.arraycopy(keyBytes, 0, truncatedKey, 0, 32);
            return Keys.hmacShaKeyFor(truncatedKey);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String userId, String email, String role) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("email", email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            throw new RuntimeException("Token không hợp lệ", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
