package za.co.csnx.demo.service.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import za.co.csnx.engine.common.BusinessException;
import za.co.csnx.demo.repository.ShipmentFlowDetailRepository;
import za.co.csnx.demo.repository.ShipmentFlowHeaderRepository;
import za.co.csnx.engine.web.dto.ProcessModelEnvelope;
import za.co.csnx.engine.web.dto.ProcessModelHolder;
import za.co.csnx.engine.web.dto.ProcessRequest;
import za.co.csnx.engine.web.dto.ProcessResponse;

/**
 * Exercises {@link CorporateShipmentFlowActivityService} end-to-end against
 * a real Postgres so the parent-child cmd_details envelope round-trips
 * faithfully (master-form data + populated children with hydrated trader
 * names).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CorporateShipmentFlowActivityServiceTest {

    @Autowired
    private CorporateShipmentFlowActivityService activity;
    @Autowired
    private ShipmentFlowHeaderRepository headerRepository;
    @Autowired
    private ShipmentFlowDetailRepository detailRepository;

    @BeforeEach
    void authenticate() {
        // AbstractCrudActivityService.currentCompanyCode() reads the principal
        // name as "<company>|<username>".
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("WCS|wcs", null, List.of()));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cmdSearchReturnsSeededHeaders() {
        ProcessResponse response = activity.cmdSearch(emptySearchRequest());

        assertThat(response.exception()).isNull();
        List<ProcessModelEnvelope> rows = response.modelHolders().get("").models();
        assertThat(rows).extracting(e -> e.data().get("shipmentFlow"))
                .contains("A1", "STORE1");
    }

    @Test
    void cmdDetailsReturnsParentChildEnvelopeWithHydratedTraderNames() {
        ProcessRequest request = new ProcessRequest(
                "corporateShipmentFlows", "cmd_details",
                null, null, null, null, null, null, null, null,
                List.of(ProcessModelEnvelope.ofData(Map.of(
                        "shipmentFlow", "A1",
                        "flowDescription", "Main"))),
                null, null, null, null, null, null, null, null, null, null, null, null);

        ProcessResponse response = activity.cmdCustom(request);

        assertThat(response.exception()).isNull();
        assertThat(response.changePage()).isTrue();
        assertThat(response.workflow()).isEqualTo("corporateShipmentFlowDetails.maintenance");

        ProcessModelHolder holder = response.modelHolders().get("");
        assertThat(holder.componentType()).isEqualTo("parentChild");
        assertThat(holder.model().data())
                .containsEntry("shipmentFlow", "A1")
                .containsEntry("flowDescription", "Main");
        assertThat(holder.models()).hasSize(3);
        assertThat(holder.models())
                .extracting(e -> e.data().get("traderName"))
                .contains("Warehouse 101 — Cork", "Warehouse 510 — Dublin", "Customer One Retail");
    }

    @Test
    void cmdDetailsRejectsUnknownCommand() {
        ProcessRequest request = new ProcessRequest(
                "corporateShipmentFlows", "cmd_unknown_verb",
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null);

        assertThatThrownBy(() -> activity.cmdCustom(request))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static ProcessRequest emptySearchRequest() {
        return new ProcessRequest(
                "corporateShipmentFlows", "cmd_search",
                Map.of("", new ProcessModelHolder(
                        ProcessModelEnvelope.ofData(Map.of()), null, null, null, null, "none", "form")),
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null);
    }
}
