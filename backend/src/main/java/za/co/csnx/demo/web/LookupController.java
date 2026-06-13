package za.co.csnx.demo.web;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.csnx.demo.service.LookupService;
import za.co.csnx.engine.web.dto.LookupBundle;
import za.co.csnx.engine.web.dto.LookupInitResponse;

/**
 * GET /api/lookup/init — VVD data for the csnx-ui shell. Pre-login returns the
 * Company VVD only; post-login returns every VVD. Wire shape is csnx-ui's
 * {@code {data, version}}.
 */
@RestController
@RequestMapping("/api/lookup")
public class LookupController {

    private final LookupService lookupService;

    public LookupController(LookupService lookupService) {
        this.lookupService = lookupService;
    }

    @GetMapping("/init")
    public ResponseEntity<LookupInitResponse> init(Authentication authentication) {
        boolean authed = authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
        LookupBundle bundle = authed
                ? lookupService.buildBundleFromPrincipal(authentication.getName())
                : lookupService.buildPreLoginBundle();
        Map<String, Map<String, String>> data = bundle.lookupData() == null
                ? new LinkedHashMap<>()
                : bundle.lookupData();
        long version = bundle.lookupVersion() == null ? 0L : bundle.lookupVersion();
        return ResponseEntity.ok(new LookupInitResponse(data, version));
    }
}
