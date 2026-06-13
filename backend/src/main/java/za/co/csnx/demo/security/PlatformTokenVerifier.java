package za.co.csnx.demo.security;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Verifies access tokens minted by the platform IdP (RS256, validated against
 * the platform's JWKS). Demo runs as a relying party: it never mints these
 * tokens, only checks signature + issuer + audience and extracts the platform
 * claims ({@code sub} = {@code "CPY|user"}, {@code grants} = {@code {fastpath:
 * M|R|N}}).
 *
 * <p>The decoder is built lazily on first use so the module still boots when
 * the platform is down — platform-token logins just fail until it's back.
 */
@Component
public class PlatformTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(PlatformTokenVerifier.class);

    private final String issuer;
    private final String jwksUri;
    private final String audience;

    private volatile JwtDecoder decoder;

    public PlatformTokenVerifier(
            @Value("${app.security.platform.issuer:http://localhost:8090}") String issuer,
            @Value("${app.security.platform.jwks-uri:}") String jwksUri,
            @Value("${app.security.platform.audience:demo-module}") String audience) {
        this.issuer = issuer;
        this.jwksUri = jwksUri == null || jwksUri.isBlank() ? issuer + "/oauth2/jwks" : jwksUri;
        this.audience = audience;
    }

    /** Decoded platform token: composite principal + module-scoped grants. */
    public record PlatformPrincipal(String subject, Map<String, String> grants) {}

    public Optional<PlatformPrincipal> verify(String token) {
        try {
            Jwt jwt = decoder().decode(token);
            return Optional.of(new PlatformPrincipal(jwt.getSubject(), grantsOf(jwt)));
        } catch (Exception e) {
            log.debug("Platform token rejected: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static Map<String, String> grantsOf(Jwt jwt) {
        Map<String, Object> raw = jwt.getClaimAsMap("grants");
        if (raw == null) {
            return Map.of();
        }
        Map<String, String> grants = new LinkedHashMap<>();
        raw.forEach((fastpath, right) -> grants.put(fastpath, String.valueOf(right)));
        return Map.copyOf(grants);
    }

    private JwtDecoder decoder() {
        JwtDecoder local = decoder;
        if (local == null) {
            synchronized (this) {
                if (decoder == null) {
                    NimbusJwtDecoder built = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
                    OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
                    OAuth2TokenValidator<Jwt> withAudience = new JwtClaimValidator<List<String>>(
                            "aud", aud -> aud != null && aud.contains(audience));
                    built.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
                    decoder = built;
                }
                local = decoder;
            }
        }
        return local;
    }
}
