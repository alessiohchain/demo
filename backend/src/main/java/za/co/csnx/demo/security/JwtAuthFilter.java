package za.co.csnx.demo.security;
import za.co.csnx.engine.security.GrantEnforcer;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates /api/** requests with platform-issued RS256 access tokens
 * (the only accepted credential — module-local minting is retired; sign-in
 * happens on the central IdP). The token's module-scoped grants ride along on
 * the Authentication details for {@link GrantEnforcer}.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> PUBLIC_PATHS = List.of(
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/actuator/info");
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final PlatformTokenVerifier platformTokenVerifier;

    public JwtAuthFilter(PlatformTokenVerifier platformTokenVerifier) {
        this.platformTokenVerifier = platformTokenVerifier;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PUBLIC_PATHS.stream().anyMatch(p -> PATH_MATCHER.match(p, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        extractToken(request).ifPresent(token -> {
            Optional<PlatformTokenVerifier.PlatformPrincipal> platform =
                    platformTokenVerifier.verify(token);
            if (platform.isPresent()) {
                authenticate(platform.get());
                return;
            }
            log.debug("Bearer token rejected for {} {}", request.getMethod(), request.getRequestURI());
        });
        chain.doFilter(request, response);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        return Optional.of(header.substring(BEARER_PREFIX.length()));
    }

    private void authenticate(PlatformTokenVerifier.PlatformPrincipal principal) {
        var auth = new UsernamePasswordAuthenticationToken(principal.subject(), null, List.of());
        auth.setDetails(new GrantEnforcer.ModuleGrants(principal.grants()));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
