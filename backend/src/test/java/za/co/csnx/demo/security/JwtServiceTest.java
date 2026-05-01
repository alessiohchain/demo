package za.co.csnx.demo.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes());

    private final JwtProperties props = new JwtProperties(
            "demo-test", SECRET, Duration.ofHours(1), Duration.ofDays(7));

    private final JwtService service = new JwtService(props);

    @Test
    void issuesAndParsesAccessToken() {
        String token = service.issueAccess("alice@example.com");

        assertThat(service.parse(token, JwtService.TYP_ACCESS))
                .isPresent()
                .hasValueSatisfying(c -> assertThat(c.getSubject()).isEqualTo("alice@example.com"));
    }

    @Test
    void rejectsAccessTokenWhenAskedForRefreshType() {
        String access = service.issueAccess("alice@example.com");
        assertThat(service.parse(access, JwtService.TYP_REFRESH)).isEmpty();
    }

    @Test
    void rejectsTamperedToken() {
        String token = service.issueAccess("alice@example.com");
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertThat(service.parse(tampered, JwtService.TYP_ACCESS)).isEmpty();
    }
}
