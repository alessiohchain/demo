package za.co.csnx.demo.ai;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import za.co.csnx.engine.ai.spi.TenantContextProvider;
import za.co.csnx.demo.security.AppUserDetailsService;

/**
 * Demo's {@link TenantContextProvider}: resolves the company code from demo's
 * multi-company user model so the shared AI engine scopes searches to the
 * caller's company (for entities that carry a {@code companyCode} column).
 * Overrides the engine's default via component scan.
 */
@Component
public class DemoTenantContextProvider implements TenantContextProvider {

    @Override
    public String currentCompanyCode() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : AppUserDetailsService.companyOf(auth.getName());
    }

    @Override
    public String currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }
}
