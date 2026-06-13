package za.co.csnx.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import za.co.csnx.demo.domain.AppUser;
import za.co.csnx.demo.repository.AppUserRepository;

/**
 * Idempotently seeds the {@code wcs} user on application startup. Done as
 * an {@link ApplicationRunner} rather than inside the Flyway migration so
 * the bcrypt hash is produced by the same {@link PasswordEncoder} bean
 * that validates it at login time — no risk of cost-factor drift.
 */
@Configuration
public class AppUserSeeder {

    private static final Logger log = LoggerFactory.getLogger(AppUserSeeder.class);

    /** Dev/test convenience only — a known-password account must never exist in prod. */
    @Bean
    @Profile("!prod")
    public ApplicationRunner seedWcsUser(AppUserRepository appUserRepository,
                                         PasswordEncoder passwordEncoder) {
        return args -> seedIfMissing(appUserRepository, passwordEncoder,
                "WCS", "wcs", "wcs123!", "WCS Test User", "EN", true);
    }

    @Transactional
    public void seedIfMissing(AppUserRepository repo,
                              PasswordEncoder passwordEncoder,
                              String companyCode,
                              String username,
                              String rawPassword,
                              String fullName,
                              String language,
                              boolean wcsUser) {
        if (repo.findByCompanyCodeAndUsername(companyCode, username).isPresent()) {
            return;
        }
        AppUser u = new AppUser();
        u.setCompanyCode(companyCode);
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setFullName(fullName);
        u.setLanguage(language);
        u.setLocked(Boolean.FALSE);
        u.setWcsUser(wcsUser);
        repo.save(u);
        log.info("Seeded app user company={} username={}", companyCode, username);
    }
}
