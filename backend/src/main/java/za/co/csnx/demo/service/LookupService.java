package za.co.csnx.demo.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.csnx.demo.domain.AppUser;
import za.co.csnx.demo.repository.AppUserRepository;
import za.co.csnx.demo.security.AppUserDetailsService;
import za.co.csnx.engine.web.dto.LookupBundle;

/**
 * Builds the {@link LookupBundle} the UI engine consumes on bootstrap and via
 * {@code GET /api/lookup/init}. Menu / fastpaths / VVDs / versions / config all
 * come from the central metadata store via {@code PlatformMetadataSource} (the
 * in-memory manifest — the platform's stored copy, or the classpath fallback
 * when the platform is unreachable). The module-local {@code app_user} row is a
 * profile cache for the user's defaults (facility / warehouse / language).
 */
@Service
@Transactional(readOnly = true)
public class LookupService {

    private static final LookupBundle.PasswordSettings DEFAULT_PASSWORD_SETTINGS =
            new LookupBundle.PasswordSettings(true, 8, false, false);

    private final AppUserRepository appUserRepository;
    private final za.co.csnx.engine.registry.PlatformMetadataSource metadataSource;
    private final za.co.csnx.engine.security.GrantEnforcer grantEnforcer;

    /** Header version label — a real version (default 1.0), NOT the module code.
     *  The module switcher already identifies the module. */
    @org.springframework.beans.factory.annotation.Value("${csnx.module.version:1.0}")
    private String moduleVersion;

    public LookupService(AppUserRepository appUserRepository,
                         za.co.csnx.engine.registry.PlatformMetadataSource metadataSource,
                         za.co.csnx.engine.security.GrantEnforcer grantEnforcer) {
        this.appUserRepository = appUserRepository;
        this.metadataSource = metadataSource;
        this.grantEnforcer = grantEnforcer;
    }

    /** Pre-login bundle: Company VVD + password settings only. */
    public LookupBundle buildPreLoginBundle() {
        Map<String, Map<String, String>> companyOnly = new LinkedHashMap<>();
        Map<String, String> companies = buildLookupData().get("Company");
        if (companies != null) {
            companyOnly.put("Company", companies);
        }
        return new LookupBundle(
                null, null, null,
                companyOnly,
                null, null,
                DEFAULT_PASSWORD_SETTINGS,
                null, null, null, null);
    }

    /** Post-login bundle: full menu / fastpaths / VVDs / user / versions / features. */
    public LookupBundle buildBundle(String companyCode, String username) {
        return new LookupBundle(
                buildUser(companyCode, username),
                buildMenu(),
                buildFastpaths(),
                buildLookupData(),
                metadataSource.metadataVersion(),
                metadataSource.lookupVersion(),
                DEFAULT_PASSWORD_SETTINGS,
                metadataSource.moduleConfig().helpUrl(),
                moduleVersion,
                Boolean.FALSE,
                buildFeatures());
    }

    /** Per-module feature flags + landing/branding, from this module's live
     *  config (the platform's module row, kept fresh by the registrar;
     *  classpath defaults when the platform is unreachable). */
    private LookupBundle.Features buildFeatures() {
        var c = metadataSource.moduleConfig();
        return new LookupBundle.Features(
                c.smartNavigationEnabled(), c.dashboardEnabled(),
                c.smartReportsEnabled(), c.smartCaptureEnabled(), c.schedulingEnabled(),
                c.defaultFastpath(), c.defaultMenu(), c.tileIcon(), c.tileColor(),
                c.fwChooser());
    }

    /** Convenience for callers that have a single combined principal string. */
    public LookupBundle buildBundleFromPrincipal(String principal) {
        if (principal == null) {
            return buildPreLoginBundle();
        }
        return buildBundle(
                AppUserDetailsService.companyOf(principal),
                AppUserDetailsService.usernameOf(principal));
    }

    private Map<String, String> buildFastpaths() {
        return grantEnforcer.filterFastpaths(metadataSource.fastpaths());
    }

    private LookupBundle.UserBundle buildUser(String companyCode, String username) {
        if (username == null) {
            return null;
        }
        AppUser user = appUserRepository
                .findByCompanyCodeAndUsername(companyCode, username)
                .orElse(null);
        if (user == null) {
            return new LookupBundle.UserBundle(
                    username, null, null, username, companyCode,
                    null, null, "EN", null, false);
        }
        return new LookupBundle.UserBundle(
                user.getUsername(),
                null, null,
                user.getFullName(),
                user.getCompanyCode(),
                user.getDefaultWarehouse(),
                user.getDefaultFacility(),
                user.getLanguage(),
                null,
                false);
    }

    private List<LookupBundle.MenuNode> buildMenu() {
        return grantEnforcer.filterMenu(metadataSource.menu());
    }

    private Map<String, Map<String, String>> buildLookupData() {
        return new LinkedHashMap<>(metadataSource.lookupData());
    }
}
