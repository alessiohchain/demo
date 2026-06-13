package za.co.csnx.demo.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.csnx.demo.repository.AppUserRepository;
import za.co.csnx.demo.security.AppUserDetailsService;
import za.co.csnx.demo.web.dto.AuthResponse;
import za.co.csnx.engine.web.dto.LookupBundle;

/**
 * GET /api/session/bootstrap — engine bootstrap bundle (user / menu / fastpaths
 * / lookups / versions / features) for an already-authenticated session. The
 * SPA arrives holding a platform-issued access token and just needs the same
 * bundle a login would have returned.
 */
@RestController
@RequestMapping("/api/session")
public class SessionBootstrapController {

    private final za.co.csnx.demo.service.LookupService lookupService;
    private final AppUserRepository appUserRepository;

    public SessionBootstrapController(za.co.csnx.demo.service.LookupService lookupService,
                                      AppUserRepository appUserRepository) {
        this.lookupService = lookupService;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/bootstrap")
    public AuthResponse bootstrap() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        LookupBundle bundle = lookupService.buildBundleFromPrincipal(auth.getName());
        return new AuthResponse(null, null, 0,
                bundle.user(), bundle.menu(), bundle.fastpaths(), bundle.lookupData(),
                bundle.metadataVersion(), bundle.lookupVersion(),
                bundle.passwordSettings(), bundle.helpUrl(), bundle.versionInfo(),
                bundle.changePasswordRequired(), bundle.features());
    }

    public record SwitchRequest(String facility, String warehouse) {}

    public record SwitchResponse(LookupBundle.UserBundle user,
                                 java.util.Map<String, java.util.Map<String, String>> lookupData,
                                 Long lookupVersion) {}

    /**
     * Switch the session's working facility/warehouse — persisted on the
     * module-local {@code app_user} profile row, then the user + lookup slice is
     * rebuilt so the SPA can swap its in-memory state.
     */
    @PostMapping("/switch")
    public SwitchResponse switchFacilityWarehouse(@RequestBody SwitchRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String company = AppUserDetailsService.companyOf(auth.getName());
        String username = AppUserDetailsService.usernameOf(auth.getName());
        appUserRepository.findByCompanyCodeAndUsername(company, username).ifPresent(u -> {
            u.setDefaultFacility(request.facility());
            u.setDefaultWarehouse(request.warehouse());
            appUserRepository.save(u);
        });
        LookupBundle bundle = lookupService.buildBundleFromPrincipal(auth.getName());
        return new SwitchResponse(bundle.user(), bundle.lookupData(), bundle.lookupVersion());
    }
}
