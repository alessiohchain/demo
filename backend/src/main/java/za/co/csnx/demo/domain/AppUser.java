package za.co.csnx.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import za.co.csnx.engine.common.BaseEntity;

/**
 * Authenticated application user — composite PK (companyCode, username) so
 * the same username can exist independently per company. Mirrors CSnx's
 * {@code User} entity field-for-field for the subset of columns the demo
 * needs (password, full name, language, default warehouse/facility, locked,
 * wcs_user). The `customer` table is replaced by this one in Phase D; V8
 * drops `customer` once the Java code stops referencing it.
 *
 * <p>Conceptual reference (DO NOT read from CSnx at runtime):
 * {@code C:\software\projects\CSnx\src\za\co\csnx\model\csnx\User.java}.
 */
@Entity
@Table(name = "app_user", schema = "demoschema")
@IdClass(AppUser.Pk.class)
public class AppUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "cpy_cd", length = 8, nullable = false)
    private String companyCode;

    @Id
    @Column(name = "username", length = 64, nullable = false)
    private String username;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(name = "language", length = 8, nullable = false)
    private String language = "EN";

    @Column(name = "default_facility", length = 16)
    private String defaultFacility;

    @Column(name = "default_warehouse", length = 16)
    private String defaultWarehouse;

    @Column(name = "locked", nullable = false)
    private Boolean locked = Boolean.FALSE;

    @Column(name = "wcs_user", nullable = false)
    private Boolean wcsUser = Boolean.FALSE;

    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String companyCode) { this.companyCode = companyCode; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getDefaultFacility() { return defaultFacility; }
    public void setDefaultFacility(String defaultFacility) { this.defaultFacility = defaultFacility; }
    public String getDefaultWarehouse() { return defaultWarehouse; }
    public void setDefaultWarehouse(String defaultWarehouse) { this.defaultWarehouse = defaultWarehouse; }
    public Boolean getLocked() { return locked; }
    public void setLocked(Boolean locked) { this.locked = locked; }
    public Boolean getWcsUser() { return wcsUser; }
    public void setWcsUser(Boolean wcsUser) { this.wcsUser = wcsUser; }

    /** Composite-PK class. Named {@code Pk} (not {@code Id}) to avoid
     *  shadowing the JPA {@code @Id} annotation inside the entity body. */
    public static class Pk implements Serializable {
        private static final long serialVersionUID = 1L;
        private String companyCode;
        private String username;

        public Pk() {}
        public Pk(String companyCode, String username) {
            this.companyCode = companyCode;
            this.username = username;
        }
        public String getCompanyCode() { return companyCode; }
        public void setCompanyCode(String companyCode) { this.companyCode = companyCode; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk k)) return false;
            return Objects.equals(companyCode, k.companyCode) && Objects.equals(username, k.username);
        }

        @Override
        public int hashCode() {
            return Objects.hash(companyCode, username);
        }
    }
}
