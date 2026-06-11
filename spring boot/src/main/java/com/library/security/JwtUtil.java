package com.library.security;

import com.library.cache.RedisCacheHelper;
import com.library.entity.admin.AdminUser;
import com.library.entity.admin.SuperAdmin;
import com.library.entity.admin.TokenBlacklist;
import com.library.entity.user.NormalUser;
import com.library.repository.admin.TokenBlacklistRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final String CACHE_BLACKLIST = "blacklist";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Autowired
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Autowired
    private RedisCacheHelper cache;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractUserType(String token) {
        return extractClaim(token, claims -> claims.get("userType", String.class));
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        String userType = determineUserType(userDetails);
        Long userId = extractUserId(userDetails);

        claims.put("userType", userType);
        claims.put("userId", userId);

        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public Boolean validateToken(String token) {
        return !isTokenExpired(token);
    }

    /** 增：加入黑名单后同步写入缓存 */
    public void addToBlacklist(String token, String username) {
        try {
            if (token == null) {
                return;
            }

            if (username == null) {
                try {
                    username = extractUsername(token);
                } catch (Exception e) {
                    username = "UNKNOWN";
                }
            }

            Date expireDate = extractExpiration(token);
            LocalDateTime expireDateTime = expireDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            TokenBlacklist blacklist = new TokenBlacklist();
            blacklist.setToken(token);
            blacklist.setUsername(username != null ? username : "UNKNOWN");
            blacklist.setExpireTime(expireDateTime);
            tokenBlacklistRepository.save(blacklist);
            cache.put(CACHE_BLACKLIST, token, Boolean.TRUE);
        } catch (Exception ignored) {
        }
    }

    /** 查：是否在黑名单 — 先缓存后数据库（仅缓存 true） */
    public boolean isTokenBlacklisted(String token) {
        Boolean cached = cache.get(CACHE_BLACKLIST, token);
        if (Boolean.TRUE.equals(cached)) {
            return true;
        }
        boolean blacklisted = tokenBlacklistRepository.existsByToken(token);
        if (blacklisted) {
            cache.put(CACHE_BLACKLIST, token, Boolean.TRUE);
        }
        return blacklisted;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredTokens() {
        tokenBlacklistRepository.deleteByExpireTimeBefore(LocalDateTime.now());
    }

    private String determineUserType(UserDetails userDetails) {
        if (userDetails instanceof SuperAdmin) {
            return "SUPER_ADMIN";
        } else if (userDetails instanceof AdminUser) {
            return "ADMIN";
        } else if (userDetails instanceof NormalUser) {
            return "READER";
        }
        return "READER";
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails instanceof SuperAdmin) {
            return ((SuperAdmin) userDetails).getId();
        } else if (userDetails instanceof AdminUser) {
            return ((AdminUser) userDetails).getId();
        } else if (userDetails instanceof NormalUser) {
            return ((NormalUser) userDetails).getId();
        }
        return null;
    }
}
