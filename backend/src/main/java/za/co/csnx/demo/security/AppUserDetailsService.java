package za.co.csnx.demo.security;

/**
 * Composite-principal helpers — the {@code "<company>|<username>"} string the
 * platform IdP stamps as the JWT subject. The UserDetailsService implementation
 * is gone with module-local login; only the static parsing/composing helpers
 * remain (used by LookupService and the session controllers).
 */
public final class AppUserDetailsService {

    public static final String SEPARATOR = "|";

    private AppUserDetailsService() {
    }

    /** Compose the principal name used everywhere downstream. */
    public static String principalOf(String companyCode, String username) {
        return companyCode + SEPARATOR + username;
    }

    /** Pull the username portion out of a {@code "<company>|<username>"} principal. */
    public static String usernameOf(String principal) {
        if (principal == null) return null;
        int sep = principal.indexOf(SEPARATOR);
        return sep < 0 ? principal : principal.substring(sep + 1);
    }

    /** Pull the company-code portion out of a {@code "<company>|<username>"} principal. */
    public static String companyOf(String principal) {
        if (principal == null) return null;
        int sep = principal.indexOf(SEPARATOR);
        return sep < 0 ? null : principal.substring(0, sep);
    }
}
