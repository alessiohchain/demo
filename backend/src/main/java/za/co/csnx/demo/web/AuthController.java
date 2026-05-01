package za.co.csnx.demo.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.csnx.demo.security.JwtService;
import za.co.csnx.demo.service.AuthService;
import za.co.csnx.demo.web.dto.AuthResponse;
import za.co.csnx.demo.web.dto.LoginRequest;
import za.co.csnx.demo.web.dto.RegisterRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "refreshToken";

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AuthService.Tokens tokens = authService.login(request);
        addRefreshCookie(response, tokens.refreshToken(), tokens.refreshTtlSeconds());
        return ResponseEntity.ok(AuthResponse.bearer(tokens.accessToken(), tokens.accessTtlSeconds()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
                                                HttpServletResponse response) {
        if (refreshToken == null) {
            throw new BadCredentialsException("Missing refresh token");
        }
        var subject = jwtService.parse(refreshToken, JwtService.TYP_REFRESH)
                .map(c -> c.getSubject())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        AuthService.Tokens tokens = authService.refresh(subject);
        addRefreshCookie(response, tokens.refreshToken(), tokens.refreshTtlSeconds());
        return ResponseEntity.ok(AuthResponse.bearer(tokens.accessToken(), tokens.accessTtlSeconds()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.noContent().build();
    }

    private void addRefreshCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge((int) maxAgeSeconds);
        response.addCookie(cookie);
    }
}
