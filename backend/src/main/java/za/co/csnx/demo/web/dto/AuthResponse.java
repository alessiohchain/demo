package za.co.csnx.demo.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import za.co.csnx.engine.web.dto.LookupBundle;

/**
 * Post-login bundle envelope. With the central IdP, the module no longer mints
 * tokens — {@code accessToken} is null and the SPA arrives already holding a
 * platform-issued bearer token; this carries the engine bootstrap (user / menu
 * / fastpaths / lookupData / versions / features) returned by
 * {@code GET /api/session/bootstrap}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String tokenType,
        long accessTokenExpiresInSec,
        LookupBundle.UserBundle user,
        java.util.List<LookupBundle.MenuNode> menu,
        java.util.Map<String, String> fastpaths,
        java.util.Map<String, java.util.Map<String, String>> lookupData,
        Long metadataVersion,
        Long lookupVersion,
        LookupBundle.PasswordSettings passwordSettings,
        String helpUrl,
        String versionInfo,
        Boolean changePasswordRequired,
        LookupBundle.Features features) {

    public static AuthResponse withBundle(String token, long accessTokenExpiresInSec,
                                          LookupBundle bundle) {
        return new AuthResponse(token, "Bearer", accessTokenExpiresInSec,
                bundle.user(), bundle.menu(), bundle.fastpaths(), bundle.lookupData(),
                bundle.metadataVersion(), bundle.lookupVersion(),
                bundle.passwordSettings(), bundle.helpUrl(), bundle.versionInfo(),
                bundle.changePasswordRequired(), bundle.features());
    }
}
