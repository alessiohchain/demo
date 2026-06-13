package za.co.csnx.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import za.co.csnx.engine.common.BaseEntity;

/**
 * Conceptual mirror of CSnx's {@code Company} entity
 * ({@code C:\software\projects\CSnx\src\za\co\csnx\model\csnx\Company.java}),
 * collapsed to the fields the demo actually uses. Read-only at runtime —
 * we never read from the CSnx schema; this table lives in
 * {@code demoschema.company}.
 */
@Entity
@Table(name = "company", schema = "demoschema")
public class Company extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "cpy_cd", length = 8, nullable = false)
    private String companyCode;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
