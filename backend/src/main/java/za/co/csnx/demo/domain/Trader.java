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
 * Trader master. Composite PK {@code (companyCode, traderType, traderCode)}.
 * Used by the {@code trader.prompt} picker workflow that CSFD's
 * {@code traderCode} field opens.
 */
@Entity
@Table(name = "scwt_trader", schema = "demoschema")
@IdClass(Trader.Pk.class)
public class Trader extends MaintainableTranUserBaseEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "cpy_cd", length = 15, nullable = false)
    private String companyCode;

    @Id
    @Column(name = "trader_type", length = 9, nullable = false)
    private String traderType;

    @Id
    @Column(name = "trader_code", length = 45, nullable = false)
    private String traderCode;

    @Column(name = "trader_name", length = 100, nullable = false)
    private String traderName;

    @Temporal(TemporalType.DATE)
    @Column(name = "maint_date", nullable = false)
    private Date maintenanceDate;

    @Temporal(TemporalType.TIME)
    @Column(name = "maint_time", nullable = false)
    private Date maintenanceTime;

    @Column(name = "maint_user", length = 30)
    private String maintenanceUser;

    @Column(name = "maint_tran", length = 30)
    private String maintenanceTran;

    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String companyCode) { this.companyCode = companyCode; }
    public String getTraderType() { return traderType; }
    public void setTraderType(String traderType) { this.traderType = traderType; }
    public String getTraderCode() { return traderCode; }
    public void setTraderCode(String traderCode) { this.traderCode = traderCode; }
    public String getTraderName() { return traderName; }
    public void setTraderName(String traderName) { this.traderName = traderName; }

    public Date getMaintenanceDate() { return maintenanceDate; }
    @Override
    public void setMaintenanceDate(Date maintenanceDate) { this.maintenanceDate = maintenanceDate; }
    public Date getMaintenanceTime() { return maintenanceTime; }
    @Override
    public void setMaintenanceTime(Date maintenanceTime) { this.maintenanceTime = maintenanceTime; }
    public String getMaintenanceUser() { return maintenanceUser; }
    @Override
    public void setMaintenanceUser(String maintenanceUser) { this.maintenanceUser = maintenanceUser; }
    public String getMaintenanceTran() { return maintenanceTran; }
    @Override
    public void setMaintenanceTran(String maintenanceTran) { this.maintenanceTran = maintenanceTran; }

    public static class Pk implements Serializable {
        private static final long serialVersionUID = 1L;
        private String companyCode;
        private String traderType;
        private String traderCode;

        public Pk() {}
        public Pk(String companyCode, String traderType, String traderCode) {
            this.companyCode = companyCode;
            this.traderType = traderType;
            this.traderCode = traderCode;
        }

        public String getCompanyCode() { return companyCode; }
        public void setCompanyCode(String c) { this.companyCode = c; }
        public String getTraderType() { return traderType; }
        public void setTraderType(String t) { this.traderType = t; }
        public String getTraderCode() { return traderCode; }
        public void setTraderCode(String t) { this.traderCode = t; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk k)) return false;
            return Objects.equals(companyCode, k.companyCode)
                    && Objects.equals(traderType, k.traderType)
                    && Objects.equals(traderCode, k.traderCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(companyCode, traderType, traderCode);
        }
    }
}
