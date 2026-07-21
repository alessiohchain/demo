package za.co.csnx.demo.web;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.csnx.engine.client.ServiceProcessClient;
import za.co.csnx.engine.web.dto.ProcessRequest;
import za.co.csnx.engine.web.dto.ProcessResponse;

/**
 * TEMP test scaffolding (gated {@code csnx.test-services.enabled=true}) — fires a
 * genuine Channel-1 ({@code /api/service/process}) call from DEMO to another
 * module so both sides log to their own {@code service_call_log} (OUT on DEMO,
 * IN on the callee). Default target is the platform (DEMO's SERVICE_TARGET_URL).
 * {@code POST /api/test/call?target=platform&workflow=event.outboxLog}.
 * DELETE this + the {@code /api/test/**} permits after sign-off.
 */
@RestController
@RequestMapping("/api/test/call")
@ConditionalOnProperty(name = "csnx.test-services.enabled", havingValue = "true")
public class TestCallController {

    private static final Logger log = LoggerFactory.getLogger(TestCallController.class);

    private final ObjectProvider<ServiceProcessClient> clientProvider;

    public TestCallController(ObjectProvider<ServiceProcessClient> clientProvider) {
        this.clientProvider = clientProvider;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> call(
            @RequestParam(defaultValue = "platform") String target,
            @RequestParam(required = false) String workflow) {
        ServiceProcessClient client = clientProvider.getIfAvailable();
        if (client == null) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "ServiceProcessClient not available — set csnx.engine.service-client.enabled=true"));
        }
        String wf = (workflow == null || workflow.isBlank()) ? "test." + target : workflow;
        try {
            // Explicit target so a non-platform callee can be addressed too.
            String url = switch (target) {
                case "platform" -> "http://host.docker.internal:8090";
                case "pom" -> "http://host.docker.internal:8080";
                default -> target;
            };
            ProcessResponse r = client.call(url, "WCS",
                    ProcessRequest.builder(wf, "cmd_search").build());
            boolean ok = r == null || r.exception() == null;
            log.info("[test.call] DEMO -> {} ({}) ok={}", target, wf, ok);
            return ResponseEntity.ok(Map.of("target", target, "workflow", wf, "ok", ok));
        } catch (RuntimeException ex) {
            log.warn("[test.call] DEMO -> {} ({}) failed: {}", target, wf, ex.toString());
            return ResponseEntity.ok(Map.of("target", target, "workflow", wf,
                    "ok", false, "error", ex.toString()));
        }
    }
}
