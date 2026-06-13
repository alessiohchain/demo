package za.co.csnx.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import za.co.csnx.engine.common.MaintainableTranUserBaseEntity;

/**
 * Mirror of CSnx's {@code ReportText} entity (composite PK
 * {@code (companyCode, reportName, language, textSequence)}), in
 * Java 21 / Jakarta Persistence form. Column names live in
 * {@code demoschema.report_text} rather than CSnx's
 * {@code CSNX.SCWT_REPORT_TEXT}; the demo never reads from CSnx.
 *
 * <p>Conceptual reference:
 * {@code C:\software\projects\CSnx\src\za\co\csnx\model\csnx\ReportText.java}
 * +
 * {@code C:\software\projects\CSnx\src\za\co\csnx\model\csnx\keys\ReportTextPK.java}.
 *
 * <p>Extends {@link MaintainableTranUserBaseEntity} so the
 * {@code @PrePersist}/{@code @PreUpdate} hook stamps {@code MAINT_DATE}/
 * {@code MAINT_TIME} automatically, and so {@link
 * za.co.csnx.engine.activity.AbstractCrudActivityService} can stamp
 * {@code MAINT_USER}/{@code MAINT_TRAN} via the
 * {@link za.co.csnx.engine.common.MaintainableTranUser} contract.
 */
@Entity
@Table(name = "scwt_report_text", schema = "demoschema")
@IdClass(ReportText.Pk.class)
public class ReportText extends MaintainableTranUserBaseEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "cpy_cd", length = 8, nullable = false)
    private String companyCode;

    @Id
    @Column(name = "report_program", length = 64, nullable = false)
    private String reportName;

    @Id
    @Column(name = "system_language", length = 8, nullable = false)
    private String language;

    @Id
    @Column(name = "text_id", nullable = false)
    private Integer textSequence;

    @Column(name = "report_text", nullable = false, columnDefinition = "text")
    private String reportText;

    @Temporal(TemporalType.DATE)
    @Column(name = "maint_date", nullable = false)
    private Date maintenanceDate;

    @Temporal(TemporalType.TIME)
    @Column(name = "maint_time", nullable = false)
    private Date maintenanceTime;

    @Column(name = "maint_user", length = 30, nullable = false)
    private String maintenanceUser;

    @Column(name = "maint_tran", length = 30, nullable = false)
    private String maintenanceTran;

    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String companyCode) { this.companyCode = companyCode; }
    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Integer getTextSequence() { return textSequence; }
    public void setTextSequence(Integer textSequence) { this.textSequence = textSequence; }
    public String getReportText() { return reportText; }
    public void setReportText(String reportText) { this.reportText = reportText; }
    public Date getMaintenanceDate() { return maintenanceDate; }
    public void setMaintenanceDate(Date maintenanceDate) { this.maintenanceDate = maintenanceDate; }
    public Date getMaintenanceTime() { return maintenanceTime; }
    public void setMaintenanceTime(Date maintenanceTime) { this.maintenanceTime = maintenanceTime; }
    public String getMaintenanceUser() { return maintenanceUser; }
    public void setMaintenanceUser(String maintenanceUser) { this.maintenanceUser = maintenanceUser; }
    public String getMaintenanceTran() { return maintenanceTran; }
    public void setMaintenanceTran(String maintenanceTran) { this.maintenanceTran = maintenanceTran; }

    /** Composite-PK class. Named {@code Pk} (not {@code Id}) to avoid
     *  shadowing the JPA {@code @Id} annotation inside the entity body. */
    public static class Pk implements Serializable {
        private static final long serialVersionUID = 1L;
        private String companyCode;
        private String reportName;
        private String language;
        private Integer textSequence;

        public Pk() {}
        public Pk(String companyCode, String reportName, String language, Integer textSequence) {
            this.companyCode = companyCode;
            this.reportName = reportName;
            this.language = language;
            this.textSequence = textSequence;
        }
        public String getCompanyCode() { return companyCode; }
        public void setCompanyCode(String c) { this.companyCode = c; }
        public String getReportName() { return reportName; }
        public void setReportName(String r) { this.reportName = r; }
        public String getLanguage() { return language; }
        public void setLanguage(String l) { this.language = l; }
        public Integer getTextSequence() { return textSequence; }
        public void setTextSequence(Integer t) { this.textSequence = t; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk k)) return false;
            return Objects.equals(companyCode, k.companyCode)
                    && Objects.equals(reportName, k.reportName)
                    && Objects.equals(language, k.language)
                    && Objects.equals(textSequence, k.textSequence);
        }

        @Override
        public int hashCode() {
            return Objects.hash(companyCode, reportName, language, textSequence);
        }
    }
}
