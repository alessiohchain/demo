package za.co.csnx.demo.web;

import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import za.co.csnx.engine.activity.ActivityService;
import za.co.csnx.engine.web.dto.ProcessModelEnvelope;
import za.co.csnx.engine.web.dto.ProcessModelHolder;
import za.co.csnx.engine.web.dto.ProcessRequest;
import za.co.csnx.engine.web.dto.ProcessResponse;

/**
 * TEMP test scaffolding (gated {@code csnx.test-services.enabled=true}) — a
 * dedicated Channel-1 echo service DEMO hosts as workflow {@code test.demo} so
 * other modules can call it and BOTH sides log a clearly-named row
 * (service = {@code test.demo}). DELETE after sign-off.
 */
@Service
@ConditionalOnProperty(name = "csnx.test-services.enabled", havingValue = "true")
public class TestEchoActivityService implements ActivityService {

    @Override
    public String workflow() {
        return "test.demo";
    }

    @Override
    public ProcessResponse cmdSearch(ProcessRequest request) {
        return ProcessResponse.ok(workflow(), request.command(),
                Map.of("", ProcessModelHolder.ofModels(List.of(
                        ProcessModelEnvelope.ofData(Map.of("module", "DEMO", "pong", Boolean.TRUE))))));
    }
}
