package za.co.csnx.demo.service.activity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import za.co.csnx.demo.TestcontainersConfiguration;
import za.co.csnx.engine.web.dto.ProcessModelEnvelope;
import za.co.csnx.engine.web.dto.ProcessModelHolder;
import za.co.csnx.engine.web.dto.ProcessRequest;
import za.co.csnx.engine.web.dto.ProcessResponse;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TraderPromptActivityServiceTest {

    @Autowired
    private TraderPromptActivityService activity;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("WCS|wcs", null, List.of()));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void searchWithoutCriteriaReturnsAllSeededTraders() {
        ProcessResponse response = activity.cmdSearch(request("cmd_search", Map.of()));

        assertThat(response.exception()).isNull();
        List<ProcessModelEnvelope> rows = response.modelHolders().get("").models();
        // V14 seeds 8 traders (3 W, 2 S, 3 C).
        assertThat(rows).hasSize(8);
    }

    @Test
    void searchFiltersByTraderType() {
        ProcessResponse response = activity.cmdSearch(request("cmd_search",
                Map.of("traderType", "S")));

        List<ProcessModelEnvelope> rows = response.modelHolders().get("").models();
        assertThat(rows).hasSize(2);
        assertThat(rows).allMatch(r -> "S".equals(r.data().get("traderType")));
    }

    @Test
    void searchFiltersByCodeSubstring() {
        ProcessResponse response = activity.cmdSearch(request("cmd_search",
                Map.of("traderCode", "WH")));

        List<ProcessModelEnvelope> rows = response.modelHolders().get("").models();
        assertThat(rows).hasSizeGreaterThanOrEqualTo(3);
        assertThat(rows).allMatch(r -> ((String) r.data().get("traderCode")).contains("WH"));
    }

    private static ProcessRequest request(String command, Map<String, Object> criteria) {
        return new ProcessRequest(
                "trader.prompt", command,
                Map.of("", new ProcessModelHolder(
                        ProcessModelEnvelope.ofData(criteria), null, null, null, null, "none", "form")),
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null);
    }
}
