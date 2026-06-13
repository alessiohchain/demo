package za.co.csnx.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import za.co.csnx.demo.TestcontainersConfiguration;
import za.co.csnx.demo.domain.ShipmentFlowDetail;
import za.co.csnx.demo.domain.ShipmentFlowHeader;
import za.co.csnx.engine.common.AuditingConfig;

/**
 * Verifies the custom JPA finders against a real Postgres (Testcontainers)
 * so the composite-PK + JSON dialect paths are exercised.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// A @DataJpaTest slice doesn't load the engine's @EnableJpaAuditing config, so
// pull it in explicitly — otherwise @CreatedDate/@LastModifiedDate stay null and
// the NOT NULL created_at/updated_at columns reject the insert.
@Import({TestcontainersConfiguration.class, AuditingConfig.class})
class ShipmentFlowDetailRepositoryTest {

    @Autowired
    private ShipmentFlowDetailRepository repository;

    @Autowired
    private ShipmentFlowHeaderRepository headerRepository;

    @Test
    void findsByCompanyAndFlowOrderedBySeq() {
        saveHeader("WCS", "TST1");
        saveHeader("WCS", "OTHER");
        save("WCS", "TST1", 30, "W", "WH-101");
        save("WCS", "TST1", 10, "W", "WH-510");
        save("WCS", "TST1", 20, "C", "C-001");
        save("WCS", "OTHER", 10, "W", "WH-101");

        var rows = repository
                .findByCompanyCodeAndShipmentFlowOrderByShipmentFlowSeqAsc("WCS", "TST1");

        assertThat(rows).extracting(ShipmentFlowDetail::getShipmentFlowSeq)
                .containsExactly(10, 20, 30);
        assertThat(rows).noneMatch(r -> "OTHER".equals(r.getShipmentFlow()));
    }

    @Test
    void findsMaxSeqForHeader() {
        saveHeader("WCS", "MAX1");
        save("WCS", "MAX1", 10, "W", "WH-101");
        save("WCS", "MAX1", 30, "W", "WH-510");
        save("WCS", "MAX1", 20, "C", "C-001");

        var max = repository.findMaxShipmentFlowSeq("WCS", "MAX1");

        assertThat(max).contains(30);
    }

    @Test
    void maxSeqEmptyWhenNoRows() {
        var max = repository.findMaxShipmentFlowSeq("WCS", "NONE");
        assertThat(max).isEmpty();
    }

    private void saveHeader(String company, String flow) {
        if (headerRepository.findById(new ShipmentFlowHeader.Pk(company, flow)).isPresent()) {
            return;
        }
        ShipmentFlowHeader h = new ShipmentFlowHeader();
        h.setCompanyCode(company);
        h.setShipmentFlow(flow);
        h.setFlowDescription("test header " + flow);
        h.setMaintenanceDate(Date.from(Instant.now()));
        h.setMaintenanceTime(Date.from(Instant.now()));
        h.setMaintenanceUser("test");
        h.setMaintenanceTran("INSERT");
        headerRepository.save(h);
    }

    private void save(String company, String flow, int seq, String traderType, String traderCode) {
        ShipmentFlowDetail d = new ShipmentFlowDetail();
        d.setCompanyCode(company);
        d.setShipmentFlow(flow);
        d.setShipmentFlowSeq(seq);
        d.setTraderType(traderType);
        d.setTraderCode(traderCode);
        d.setTransitTime(0);
        d.setProcessTime(0);
        d.setMaintenanceDate(Date.from(Instant.now()));
        d.setMaintenanceTime(Date.from(Instant.now()));
        d.setMaintenanceUser("test");
        d.setMaintenanceTran("INSERT");
        repository.save(d);
    }
}
