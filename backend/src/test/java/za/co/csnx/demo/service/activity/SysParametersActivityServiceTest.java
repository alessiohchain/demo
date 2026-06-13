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
import za.co.csnx.engine.web.dto.ProcessModelEnvelope;
import za.co.csnx.engine.web.dto.ProcessModelHolder;
import za.co.csnx.engine.web.dto.ProcessRequest;
import za.co.csnx.engine.web.dto.ProcessResponse;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SysParametersActivityServiceTest {

    @Autowired
    private SysParametersActivityService activity;

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
    void cmdSearchReturnsSingletonFormShape() {
        // WSPM's cmd_search returns ONE row (the company's settings) as a
        // form-singleton — componentType=form, .model=envelope, no
        // .models[]. Mirrors CSnx's WaddActivityService_WSPM.systemParameters
        // which ships the parameters via {@code createResponse(model)}.
        ProcessRequest request = buildRequest("cmd_search", Map.of());

        ProcessResponse response = activity.cmdSearch(request);

        assertThat(response.exception()).isNull();
        ProcessModelHolder holder = response.modelHolders().get("");
        assertThat(holder.componentType()).isEqualTo("form");
        assertThat(holder.model()).isNotNull();
        Map<String, Object> data = holder.model().data();
        assertThat(data.get("companyCode")).isEqualTo("WCS");
        // V18 seeds the WCS row with the column DEFAULTs — verify a sample
        // of fields landed:
        assertThat(((Number) data.get("daysPerWeek")).intValue()).isEqualTo(5);
        assertThat(data.get("dateFormat")).isEqualTo("yyyy-MM-dd");
        assertThat(data.get("verificationAlwDuplicates")).isEqualTo("N");
        // models[] should be empty (form, not grid).
        assertThat(holder.models()).isNullOrEmpty();
    }

    @Test
    void cmdUpdatePersistsAndEchoesSavedRow() {
        // Read current state, mutate two fields, save, verify the response
        // echoes the new values + maint_tran. {@code updateSerial} isn't
        // checked here because Hibernate's @Version increment happens at
        // flush (= TX commit), not at save() return — the echoed envelope
        // reflects the pre-flush state. A follow-up cmdSearch (separate TX)
        // would see the bump; that's verified by the subsequent re-read.
        ProcessResponse before = activity.cmdSearch(buildRequest("cmd_search", Map.of()));
        Map<String, Object> beforeData = before.modelHolders().get("").model().data();

        Map<String, Object> edited = new java.util.LinkedHashMap<>(beforeData);
        edited.put("daysPerWeek", 6);
        edited.put("dateFormat", "dd/MM/yyyy");

        ProcessResponse response = activity.cmdUpdate(buildRequest("cmd_update", edited));

        assertThat(response.exception()).isNull();
        ProcessModelHolder holder = response.modelHolders().get("");
        assertThat(holder.componentType()).isEqualTo("form");
        assertThat(holder.model()).isNotNull();
        Map<String, Object> saved = holder.model().data();
        assertThat(((Number) saved.get("daysPerWeek")).intValue()).isEqualTo(6);
        assertThat(saved.get("dateFormat")).isEqualTo("dd/MM/yyyy");
        assertThat(saved.get("maintenanceTran")).isEqualTo("WSPM");

        // Re-read in a fresh transaction — confirms the values persisted.
        ProcessResponse reread = activity.cmdSearch(buildRequest("cmd_search", Map.of()));
        Map<String, Object> rereadData = reread.modelHolders().get("").model().data();
        assertThat(((Number) rereadData.get("daysPerWeek")).intValue()).isEqualTo(6);
        assertThat(rereadData.get("dateFormat")).isEqualTo("dd/MM/yyyy");

        // Cleanup — restore seed values.
        Map<String, Object> reset = new java.util.LinkedHashMap<>(rereadData);
        reset.put("daysPerWeek", 5);
        reset.put("dateFormat", "yyyy-MM-dd");
        activity.cmdUpdate(buildRequest("cmd_update", reset));
    }

    @Test
    void cmdUpdateRejectsInvertedVerificationLengths() {
        ProcessResponse before = activity.cmdSearch(buildRequest("cmd_search", Map.of()));
        Map<String, Object> edited = new java.util.LinkedHashMap<>(
                before.modelHolders().get("").model().data());
        edited.put("verificationMinLength", 20);
        edited.put("verificationMaxLength", 10);

        assertThatThrownBy(() -> activity.cmdUpdate(buildRequest("cmd_update", edited)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Verification max length must be");
    }

    @Test
    void cmdCreateBlocked() {
        assertThatThrownBy(() -> activity.cmdCreate(buildRequest("cmd_create", Map.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("edit only");
    }

    @Test
    void cmdDeleteBlocked() {
        assertThatThrownBy(() -> activity.cmdDelete(buildRequest("cmd_delete", Map.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("edit only");
    }

    @Test
    void cmdCopyBlocked() {
        assertThatThrownBy(() -> activity.cmdCopy(buildRequest("cmd_copy", Map.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("edit only");
    }

    private static ProcessRequest buildRequest(String command, Map<String, Object> data) {
        return new ProcessRequest(
                "sysParameters.maintenance", command,
                Map.of("", new ProcessModelHolder(
                        ProcessModelEnvelope.ofData(data), null, null, null, null, "none", "form")),
                null, null, null, null, null, null, null,
                null, null, null, null,
                null, null, null, null, null, null,
                null, null, null);
    }
}
