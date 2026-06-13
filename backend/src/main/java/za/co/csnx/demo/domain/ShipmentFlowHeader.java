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
 * Header row of a Corporate Shipment Flow (COSF). Composite PK
 * {@code (companyCode, shipmentFlow)}.
 *
 * <p>Conceptual reference:
 * {@code C:\software\projects\CSnx\src\za\co\csnx\model\csnx\ScctShipmentFlowHeader.java}.
 */
@Entity
@Table(name = "scct_shipment_flow_header", schema = "demoschema")
@IdClass(ShipmentFlowHeader.Pk.class)
public class ShipmentFlowHeader extends MaintainableTranUserBaseEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "cpy_cd", length = 15, nullable = false)
    private String companyCode;

    @Id
    @Column(name = "shipment_flow", length = 30, nullable = false)
    private String shipmentFlow;

    @Column(name = "flow_description", length = 100, nullable = false)
    private String flowDescription;

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
    public String getShipmentFlow() { return shipmentFlow; }
    public void setShipmentFlow(String shipmentFlow) { this.shipmentFlow = shipmentFlow; }
    public String getFlowDescription() { return flowDescription; }
    public void setFlowDescription(String flowDescription) { this.flowDescription = flowDescription; }

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
        private String shipmentFlow;

        public Pk() {}
        public Pk(String companyCode, String shipmentFlow) {
            this.companyCode = companyCode;
            this.shipmentFlow = shipmentFlow;
        }

        public String getCompanyCode() { return companyCode; }
        public void setCompanyCode(String c) { this.companyCode = c; }
        public String getShipmentFlow() { return shipmentFlow; }
        public void setShipmentFlow(String s) { this.shipmentFlow = s; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk k)) return false;
            return Objects.equals(companyCode, k.companyCode)
                    && Objects.equals(shipmentFlow, k.shipmentFlow);
        }

        @Override
        public int hashCode() {
            return Objects.hash(companyCode, shipmentFlow);
        }
    }
}
