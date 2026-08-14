package dam.guildmaster.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long maxHours;

    public JwtService(
            @Value("${guildmaster.jwt.secret}") String secret,
            @Value("${guildmaster.jwt.max-hours:16}") long maxHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.maxHours = maxHours;
    }

    public String createToken(Integer userId, String mail, String name, String role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(maxHours * 3600);
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
                .id(jti)
                .subject(String.valueOf(userId))
                .claim("mail", mail)
                .claim("name", name)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getMaxHours() {
        return maxHours;
    }
}
