package za.co.csnx.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    static final String CLAIM_TYP = "typ";
    public static final String TYP_ACCESS = "access";
    public static final String TYP_REFRESH = "refresh";

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(props.secret()));
    }

    public String issueAccess(String subject) {
        return issue(subject, TYP_ACCESS, props.accessTokenTtl().toMillis());
    }

    public String issueRefresh(String subject) {
        return issue(subject, TYP_REFRESH, props.refreshTokenTtl().toMillis());
    }

    public long accessTokenTtlSeconds() {
        return props.accessTokenTtl().toSeconds();
    }

    public long refreshTokenTtlSeconds() {
        return props.refreshTokenTtl().toSeconds();
    }

    private String issue(String subject, String typ, long ttlMillis) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(props.issuer())
                .subject(subject)
                .claim(CLAIM_TYP, typ)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttlMillis)))
                .signWith(key)
                .compact();
    }

    public Optional<Claims> parse(String token, String expectedTyp) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(props.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expectedTyp.equals(claims.get(CLAIM_TYP, String.class))) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
