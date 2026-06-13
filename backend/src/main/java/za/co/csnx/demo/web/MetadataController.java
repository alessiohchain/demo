package za.co.csnx.demo.web;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.csnx.engine.common.EntityNotFoundException;
import za.co.csnx.engine.registry.PlatformMetadataSource;
import za.co.csnx.engine.security.GrantEnforcer;
import za.co.csnx.engine.web.dto.MetadataHolder;

/**
 * GET /api/metadata?workflow=&lt;id&gt; — returns the {@link MetadataHolder}
 * payload for a workflow, from the central metadata store via
 * {@link PlatformMetadataSource}.
 */
@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    private final PlatformMetadataSource metadataSource;
    private final GrantEnforcer grantEnforcer;

    public MetadataController(PlatformMetadataSource metadataSource,
                              GrantEnforcer grantEnforcer) {
        this.metadataSource = metadataSource;
        this.grantEnforcer = grantEnforcer;
    }

    @GetMapping
    public ResponseEntity<Object> getScreen(@RequestParam("workflow") String workflow) {
        Map<String, Object> payload = metadataSource.screenPayload(workflow)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No metadata registered for workflow '" + workflow + "'"));
        metadataSource.fastpathOf(workflow)
                .filter(fp -> fp != null && !fp.isBlank())
                .ifPresent(grantEnforcer::requireRead);
        return ResponseEntity.ok(payload);
    }
}
