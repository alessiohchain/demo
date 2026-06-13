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
import za.co.csnx.demo.domain.ShipmentFlowDetail;
import za.co.csnx.demo.repository.ShipmentFlowDetailRepository;
import za.co.csnx.engine.web.dto.ProcessModelEnvelope;
import za.co.csnx.engine.web.dto.ProcessModelHolder;
import za.co.csnx.engine.web.dto.ProcessRequest;
import za.co.csnx.engine.web.dto.ProcessResponse;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CorporateShipmentFlowDetailsActivityServiceTest {

    @Autowired
    private CorporateShipmentFlowDetailsActivityService activity;
    @Autowired
    private ShipmentFlowDetailRepository detailRepository;

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
    void cmdSearchFiltersByParentHeader() {
        ProcessRequest request = buildRequest("cmd_search", Map.of(),
                parentModel("A1"));

        ProcessResponse response = activity.cmdSearch(request);

        assertThat(response.exception()).isNull();
        List<ProcessModelEnvelope> rows = response.modelHolders().get("").models();
        assertThat(rows).hasSizeGreaterThanOrEqualTo(3);
        assertThat(rows).allMatch(r -> "A1".equals(r.data().get("shipmentFlow")));
    }

    @Test
    void cmdSearchEmptyWithoutParent() {
        ProcessRequest request = buildRequest("cmd_search", Map.of(), null);

        ProcessResponse response = activity.cmdSearch(request);

        assertThat(response.modelHolders().get("").models()).isEmpty();
    }

    @Test
    void cmdCreateAutoAssignsSeqStampsTranAndEchoesSavedRowAsSingleton() {
        ProcessRequest request = buildRequest("cmd_create",
                Map.of(
                        "traderType", "S",
                        "traderCode", "ACME",
                        "transitTime", 1,
                        "processTime", 2),
                parentModel("A1"));

        ProcessResponse response = activity.cmdCreate(request);

        assertThat(response.exception()).isNull();
        ProcessModelHolder holder = response.modelHolders().get("");
        // Contract: saved row goes in modelHolders[""].model (singular).
        // This is what the engine's EntityDialog master-detail branch reads
        // when appending the row to the parent's children grid client-side.
        // Returning under .models[] (plural) would leave the grid stale.
        assertThat(holder.model()).isNotNull();
        Map<String, Object> saved = holder.model().data();
        assertThat(saved.get("shipmentFlow")).isEqualTo("A1");
        assertThat(saved.get("traderCode")).isEqualTo("ACME");
        assertThat(saved.get("traderType")).isEqualTo("S");
        assertThat(saved.get("maintenanceTran")).isEqualTo("CSFD");
        // Seq is max(seeded) + 10, so >= 40 (the seeded max is 30).
        assertThat(((Number) saved.get("shipmentFlowSeq")).intValue()).isGreaterThanOrEqualTo(40);

        // Cleanup
        detailRepository.deleteById(new za.co.csnx.demo.domain.ShipmentFlowDetail.Pk(
                "WCS", "A1", ((Number) saved.get("shipmentFlowId")).longValue()));
    }

    @Test
    void cmdCopyOfExistingRowSucceedsByDroppingSourceId() {
        // Regression: a Copy popup seeds the dialog with the source row's
        // full data, including shipmentFlowId. Without stripping that id,
        // cmd_copy → cmd_create → validateDuplicate rejects with "Record
        // already exists" because the source row's id matches itself.
        var existing = detailRepository
                .findByCompanyCodeAndShipmentFlowOrderByShipmentFlowSeqAsc("WCS", "A1")
                .get(0);

        Map<String, Object> copyDialogData = new java.util.LinkedHashMap<>();
        copyDialogData.put("shipmentFlowId", existing.getShipmentFlowId()); // seeded by Copy
        copyDialogData.put("traderType", "S");
        copyDialogData.put("traderCode", "NORTH"); // user changed the trader
        copyDialogData.put("transitTime", 4);
        copyDialogData.put("processTime", 3);

        ProcessRequest request = buildRequest("cmd_copy", copyDialogData, parentModel("A1"));

        ProcessResponse response = activity.cmdCopy(request);

        assertThat(response.exception()).isNull();
        Map<String, Object> created = response.modelHolders().get("").model().data();
        assertThat(created.get("traderCode")).isEqualTo("NORTH");
        assertThat(((Number) created.get("shipmentFlowId")).longValue())
                .isNotEqualTo(existing.getShipmentFlowId());

        // Cleanup
        detailRepository.deleteById(new za.co.csnx.demo.domain.ShipmentFlowDetail.Pk(
                "WCS", "A1", ((Number) created.get("shipmentFlowId")).longValue()));
    }

    @Test
    void cmdDeleteReturnsParentChildEnvelopeWithRemainingChildren() {
        // Delete keeps the heavier parentChild refresh response because the
        // engine has no client-side "remove deleted rows" append branch — it
        // would otherwise show the deleted rows until a manual Search.

        // Seed a throwaway row we can delete (avoids tampering with the seed
        // rows that other tests depend on).
        ShipmentFlowDetail row = new ShipmentFlowDetail();
        row.setCompanyCode("WCS");
        row.setShipmentFlow("A1");
        row.setShipmentFlowSeq(999);
        row.setTraderType("S");
        row.setTraderCode("ACME");
        row.setTransitTime(0);
        row.setProcessTime(0);
        row.setMaintenanceDate(new java.util.Date());
        row.setMaintenanceTime(new java.util.Date());
        row.setMaintenanceUser("test");
        row.setMaintenanceTran("INSERT");
        ShipmentFlowDetail saved = detailRepository.save(row);

        ProcessRequest request = new ProcessRequest(
                "corporateShipmentFlowDetails.maintenance", "cmd_delete",
                Map.of("", new ProcessModelHolder(
                        ProcessModelEnvelope.ofData(Map.of()), null, null, null, null, "none", "form")),
                null, null, null, null, null, null, null,
                List.of(ProcessModelEnvelope.ofData(Map.of(
                        "shipmentFlow", "A1",
                        "shipmentFlowId", saved.getShipmentFlowId()))),
                null, null, parentModel("A1"),
                null, null, null, null, null, null, null, null, null);

        ProcessResponse response = activity.cmdDelete(request);

        assertThat(response.exception()).isNull();
        ProcessModelHolder holder = response.modelHolders().get("");
        assertThat(holder.componentType()).isEqualTo("parentChild");
        assertThat(holder.model().data()).containsEntry("shipmentFlow", "A1");
        // The deleted row must not appear in the refreshed children list.
        assertThat(holder.models()).noneMatch(env ->
                env.data() != null
                && saved.getShipmentFlowId().equals(((Number) env.data().get("shipmentFlowId")).longValue()));
    }

    @Test
    void cmdUpdateSavesEveryRowFromModelsArrayAndEchoesEachRowId() {
        // Regression: inline-edit two rows then click Update — the
        // toolbar's performInlineUpdate batches every edited row in
        // modelHolders[""].models[]. The base class used to read only
        // the first envelope via singleModelData, so the second row's
        // edits were silently dropped. Now cmdUpdate iterates over the
        // full models[] list and ships back one saved envelope per row.
        ShipmentFlowDetail rowA = new ShipmentFlowDetail();
        rowA.setCompanyCode("WCS");
        rowA.setShipmentFlow("A1");
        rowA.setShipmentFlowSeq(996);
        rowA.setTraderType("S");
        rowA.setTraderCode("ACME");
        rowA.setTransitTime(1);
        rowA.setProcessTime(1);
        rowA.setMaintenanceDate(new java.util.Date());
        rowA.setMaintenanceTime(new java.util.Date());
        rowA.setMaintenanceUser("test");
        rowA.setMaintenanceTran("INSERT");
        ShipmentFlowDetail savedA = detailRepository.save(rowA);

        ShipmentFlowDetail rowB = new ShipmentFlowDetail();
        rowB.setCompanyCode("WCS");
        rowB.setShipmentFlow("A1");
        rowB.setShipmentFlowSeq(995);
        rowB.setTraderType("S");
        rowB.setTraderCode("ACME");
        rowB.setTransitTime(1);
        rowB.setProcessTime(1);
        rowB.setMaintenanceDate(new java.util.Date());
        rowB.setMaintenanceTime(new java.util.Date());
        rowB.setMaintenanceUser("test");
        rowB.setMaintenanceTran("INSERT");
        ShipmentFlowDetail savedB = detailRepository.save(rowB);

        Map<String, Object> editA = new java.util.LinkedHashMap<>();
        editA.put("rowID", 10);
        editA.put("companyCode", "WCS");
        editA.put("shipmentFlow", "A1");
        editA.put("shipmentFlowId", savedA.getShipmentFlowId());
        editA.put("shipmentFlowSeq", savedA.getShipmentFlowSeq());
        editA.put("traderType", "S");
        editA.put("traderCode", "ACME");
        editA.put("transitTime", 11);
        editA.put("processTime", 1);

        Map<String, Object> editB = new java.util.LinkedHashMap<>();
        editB.put("rowID", 20);
        editB.put("companyCode", "WCS");
        editB.put("shipmentFlow", "A1");
        editB.put("shipmentFlowId", savedB.getShipmentFlowId());
        editB.put("shipmentFlowSeq", savedB.getShipmentFlowSeq());
        editB.put("traderType", "S");
        editB.put("traderCode", "ACME");
        editB.put("transitTime", 1);
        editB.put("processTime", 22);

        ProcessModelHolder holderIn = new ProcessModelHolder(
                ProcessModelEnvelope.ofData(editA),
                List.of(ProcessModelEnvelope.ofData(editA), ProcessModelEnvelope.ofData(editB)),
                null, null, null, "none", "grid");
        ProcessRequest request = new ProcessRequest(
                "corporateShipmentFlowDetails.maintenance", "cmd_update",
                Map.of("", holderIn),
                null, null, null, null, null, null, null,
                null, null, null, parentModel("A1"),
                null, null, null, null, null, null, null, null, null);

        ProcessResponse response = activity.cmdUpdate(request);

        assertThat(response.exception()).isNull();
        ProcessModelHolder holder = response.modelHolders().get("");
        assertThat(holder.componentType()).isEqualTo("grid");
        assertThat(holder.actionType()).isEqualTo("update");
        assertThat(holder.models()).hasSize(2);

        // Both rows persisted with their edits.
        ShipmentFlowDetail reloadedA = detailRepository
                .findById(new ShipmentFlowDetail.Pk("WCS", "A1", savedA.getShipmentFlowId()))
                .orElseThrow();
        ShipmentFlowDetail reloadedB = detailRepository
                .findById(new ShipmentFlowDetail.Pk("WCS", "A1", savedB.getShipmentFlowId()))
                .orElseThrow();
        assertThat(reloadedA.getTransitTime()).isEqualTo(11);
        assertThat(reloadedB.getProcessTime()).isEqualTo(22);

        // rowID echoed onto each response envelope so the engine's
        // mergeRowsByKey can replace each grid row in-place.
        assertThat(holder.models().get(0).data().get("rowID")).isEqualTo(10);
        assertThat(holder.models().get(1).data().get("rowID")).isEqualTo(20);

        // Cleanup
        detailRepository.deleteById(new ShipmentFlowDetail.Pk("WCS", "A1", savedA.getShipmentFlowId()));
        detailRepository.deleteById(new ShipmentFlowDetail.Pk("WCS", "A1", savedB.getShipmentFlowId()));
    }

    @Test
    void cmdUpdateReturnsGridActionUpdateEnvelopeWithRowIdEcho() {
        // Inline-edit Update sends the row's data including {@code rowID}
        // (stamped client-side at grid-load). The response must echo that
        // rowID back so the engine's {@code mergeRowsByKey} can replace
        // the row in-place. The shape must be
        // {componentType: "grid", actionType: "update", models: [saved]}
        // — NOT a singleton .model form-shape (that path leaks the saved
        // child into the master form on master-detail screens like CSFD).
        ShipmentFlowDetail seed = new ShipmentFlowDetail();
        seed.setCompanyCode("WCS");
        seed.setShipmentFlow("A1");
        seed.setShipmentFlowSeq(997);
        seed.setTraderType("S");
        seed.setTraderCode("ACME");
        seed.setTransitTime(1);
        seed.setProcessTime(1);
        seed.setMaintenanceDate(new java.util.Date());
        seed.setMaintenanceTime(new java.util.Date());
        seed.setMaintenanceUser("test");
        seed.setMaintenanceTran("INSERT");
        ShipmentFlowDetail saved = detailRepository.save(seed);

        Map<String, Object> editedRow = new java.util.LinkedHashMap<>();
        editedRow.put("rowID", 42); // client-stamped grid row id
        editedRow.put("companyCode", "WCS");
        editedRow.put("shipmentFlow", "A1");
        editedRow.put("shipmentFlowId", saved.getShipmentFlowId());
        editedRow.put("shipmentFlowSeq", saved.getShipmentFlowSeq());
        editedRow.put("traderType", "S");
        editedRow.put("traderCode", "ACME");
        editedRow.put("transitTime", 7); // user edit
        editedRow.put("processTime", 1);

        ProcessRequest request = buildRequest("cmd_update", editedRow, parentModel("A1"));

        ProcessResponse response = activity.cmdUpdate(request);

        assertThat(response.exception()).isNull();
        ProcessModelHolder holder = response.modelHolders().get("");
        assertThat(holder.componentType()).isEqualTo("grid");
        assertThat(holder.actionType()).isEqualTo("update");
        assertThat(holder.model()).isNull();
        assertThat(holder.models()).hasSize(1);
        Map<String, Object> echoed = holder.models().get(0).data();
        assertThat(echoed.get("rowID")).isEqualTo(42);
        assertThat(((Number) echoed.get("transitTime")).intValue()).isEqualTo(7);
        assertThat(echoed.get("maintenanceTran")).isEqualTo("UPDATE");

        // Cleanup
        detailRepository.deleteById(new ShipmentFlowDetail.Pk(
                "WCS", "A1", saved.getShipmentFlowId()));
    }

    @Test
    void cmdDeleteWithoutParentModelFallsBackToSelectedModelsForShipmentFlow() {
        // Regression: the toolbar's performDelete is a specialised path
        // that skips the {@code parentData: true} flag, so cmd_delete
        // arrives without {@code parentModel}. The activity must read
        // {@code shipmentFlow} from the selected rows' own data (it's part
        // of the composite PK) so {@code refreshedChildren} can re-query
        // and ship a parentChild envelope back. Without this fallback the
        // grid stays stale after delete.
        ShipmentFlowDetail row = new ShipmentFlowDetail();
        row.setCompanyCode("WCS");
        row.setShipmentFlow("A1");
        row.setShipmentFlowSeq(998);
        row.setTraderType("S");
        row.setTraderCode("ACME");
        row.setTransitTime(0);
        row.setProcessTime(0);
        row.setMaintenanceDate(new java.util.Date());
        row.setMaintenanceTime(new java.util.Date());
        row.setMaintenanceUser("test");
        row.setMaintenanceTran("INSERT");
        ShipmentFlowDetail saved = detailRepository.save(row);

        ProcessRequest request = new ProcessRequest(
                "corporateShipmentFlowDetails.maintenance", "cmd_delete",
                Map.of("", new ProcessModelHolder(
                        ProcessModelEnvelope.ofData(Map.of()), null, null, null, null, "none", "form")),
                null, null, null, null, null, null, null,
                List.of(ProcessModelEnvelope.ofData(Map.of(
                        "shipmentFlow", "A1",
                        "shipmentFlowId", saved.getShipmentFlowId()))),
                null, null,
                // No parentModel — mirrors the toolbar's performDelete shape.
                null,
                null, null, null, null, null, null, null, null, null);

        ProcessResponse response = activity.cmdDelete(request);

        assertThat(response.exception()).isNull();
        ProcessModelHolder holder = response.modelHolders().get("");
        assertThat(holder.componentType()).isEqualTo("parentChild");
        // Children list re-queried for shipmentFlow A1 (taken from the
        // deleted row's data), with the deleted row absent.
        assertThat(holder.models()).noneMatch(env ->
                env.data() != null
                && saved.getShipmentFlowId().equals(((Number) env.data().get("shipmentFlowId")).longValue()));
        assertThat(holder.models()).allMatch(env ->
                env.data() != null && "A1".equals(env.data().get("shipmentFlow")));
    }

    @Test
    void cmdCreateRejectsUnknownTrader() {
        ProcessRequest request = buildRequest("cmd_create",
                Map.of(
                        "traderType", "S",
                        "traderCode", "NOT-A-TRADER",
                        "transitTime", 0,
                        "processTime", 0),
                parentModel("A1"));

        assertThatThrownBy(() -> activity.cmdCreate(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Trader 'NOT-A-TRADER'");
    }

    private static ProcessRequest buildRequest(String command,
                                               Map<String, Object> data,
                                               ProcessModelHolder parent) {
        return new ProcessRequest(
                "corporateShipmentFlowDetails.maintenance", command,
                Map.of("", new ProcessModelHolder(
                        ProcessModelEnvelope.ofData(data), null, null, null, null, "none", "form")),
                null, null, null, null, null, null, null,
                null, null, null, parent,
                null, null, null, null, null, null, null, null, null);
    }

    private static ProcessModelHolder parentModel(String shipmentFlow) {
        return new ProcessModelHolder(
                ProcessModelEnvelope.ofData(Map.of("shipmentFlow", shipmentFlow)),
                null, null, null, null, "none", "form");
    }
}
