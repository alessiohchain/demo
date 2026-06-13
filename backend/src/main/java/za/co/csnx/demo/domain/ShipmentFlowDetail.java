package za.co.csnx.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import za.co.csnx.engine.common.MaintainableTranUserBaseEntity;

/**
 * Detail row of a Corporate Shipment Flow (CSFD). Composite PK
 * {@code (companyCode, shipmentFlow, shipmentFlowId)} — {@code shipmentFlowId}
 * is DB-generated (BIGSERIAL).
 *
 * <p>{@link #traderName} is {@link Transient} — populated post-load by the
 * activity service via a {@code TraderRepository} lookup so the grid can
 * show the human-readable trader name alongside the code without storing
 * a denormalised copy.
 *
 * <p>Conceptual reference:
 * {@code C:\software\projects\CSnx\src\za\co\csnx\model\csnx\ScctShipmentFlowDetail.java}.
 */
@Entity
@Table(name = "scct_shipment_flow_detail", schema = "demoschema")
@IdClass(ShipmentFlowDetail.Pk.class)
public class ShipmentFlowDetail extends MaintainableTranUserBaseEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "cpy_cd", length = 15, nullable = false)
    private String companyCode;

    @Id
    @Column(name = "shipment_flow", length = 30, nullable = false)
    private String shipmentFlow;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shipment_flow_detail_seq")
    @SequenceGenerator(
            name = "shipment_flow_detail_seq",
            sequenceName = "demoschema.scct_shipment_flow_detail_shipment_flow_id_seq",
            allocationSize = 1)
    @Column(name = "shipment_flow_id", nullable = false)
    private Long shipmentFlowId;

    @Column(name = "shipment_flow_seq", nullable = false)
    private Integer shipmentFlowSeq;

    @Column(name = "trader_type", length = 9, nullable = false)
    private String traderType;

    @Column(name = "trader_code", length = 45, nullable = false)
    private String traderCode;

    @Column(name = "transit_time", nullable = false)
    private Integer transitTime;

    @Column(name = "process_time", nullable = false)
    private Integer processTime;

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

    /** Populated by the activity service after load — not persisted. */
    @Transient
    private String traderName;

    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String companyCode) { this.companyCode = companyCode; }
    public String getShipmentFlow() { return shipmentFlow; }
    public void setShipmentFlow(String shipmentFlow) { this.shipmentFlow = shipmentFlow; }
    public Long getShipmentFlowId() { return shipmentFlowId; }
    public void setShipmentFlowId(Long shipmentFlowId) { this.shipmentFlowId = shipmentFlowId; }
    public Integer getShipmentFlowSeq() { return shipmentFlowSeq; }
    public void setShipmentFlowSeq(Integer shipmentFlowSeq) { this.shipmentFlowSeq = shipmentFlowSeq; }
    public String getTraderType() { return traderType; }
    public void setTraderType(String traderType) { this.traderType = traderType; }
    public String getTraderCode() { return traderCode; }
    public void setTraderCode(String traderCode) { this.traderCode = traderCode; }
    public Integer getTransitTime() { return transitTime; }
    public void setTransitTime(Integer transitTime) { this.transitTime = transitTime; }
    public Integer getProcessTime() { return processTime; }
    public void setProcessTime(Integer processTime) { this.processTime = processTime; }
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
        private String shipmentFlow;
        private Long shipmentFlowId;

        public Pk() {}
        public Pk(String companyCode, String shipmentFlow, Long shipmentFlowId) {
            this.companyCode = companyCode;
            this.shipmentFlow = shipmentFlow;
            this.shipmentFlowId = shipmentFlowId;
        }

        public String getCompanyCode() { return companyCode; }
        public void setCompanyCode(String c) { this.companyCode = c; }
        public String getShipmentFlow() { return shipmentFlow; }
        public void setShipmentFlow(String s) { this.shipmentFlow = s; }
        public Long getShipmentFlowId() { return shipmentFlowId; }
        public void setShipmentFlowId(Long i) { this.shipmentFlowId = i; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk k)) return false;
            return Objects.equals(companyCode, k.companyCode)
                    && Objects.equals(shipmentFlow, k.shipmentFlow)
                    && Objects.equals(shipmentFlowId, k.shipmentFlowId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(companyCode, shipmentFlow, shipmentFlowId);
        }
    }
}
