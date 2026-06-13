package za.co.csnx.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.math.BigDecimal;
import java.util.Date;
import za.co.csnx.engine.common.MaintainableTranUserBaseEntity;

/**
 * System Parameters — backs the WSPM screen
 * ({@code sysParameters.maintenance} workflow). One row per company.
 *
 * <p>Demo simplifies CSnx's three-part PK (companyCode, facility,
 * warehouse) to companyCode-only because the demo doesn't model
 * facility/warehouse as first-class entities. Field shape mirrors
 * CSnx's {@code SysParameters} (~120 body columns) with column names
 * lowercased for Postgres and Java field names kept identical for
 * straightforward porting.
 *
 * <p>Conceptual reference:
 * {@code C:\software\projects\CSnx\src\za\co\csnx\model\csnx\SysParameters.java}.
 */
@Entity
@Table(name = "scwt_sys_parms", schema = "demoschema")
public class SysParameters extends MaintainableTranUserBaseEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "cpy_cd", length = 15, nullable = false)
    private String companyCode;

    // ─── System ──────────────────────────────────────────────────────
    @Column(name = "sys_slot_aisl", nullable = false)
    private Integer aisleFieldLength;
    @Column(name = "sys_slot_bay", nullable = false)
    private Integer bayFieldLength;
    @Column(name = "sys_slot_locn", length = 1, nullable = false)
    private String protectSlotLocation;
    @Column(name = "item_fld_size", nullable = false)
    private Integer itemFieldLength;
    @Column(name = "item_fld_type", length = 1, nullable = false)
    private String itemFieldType;
    @Column(name = "vendor_fld_size", nullable = false)
    private Integer vendorFieldLength;
    @Column(name = "vendor_fld_type", length = 1, nullable = false)
    private String vendorFieldType;
    @Column(name = "customer_fld_size", nullable = false)
    private Integer customerFieldLength;
    @Column(name = "customer_fld_type", length = 1, nullable = false)
    private String customerFieldType;
    @Column(name = "days_per_week", nullable = false)
    private Integer daysPerWeek;
    @Column(name = "date_format", length = 20, nullable = false)
    private String dateFormat;
    @Column(name = "time_format", length = 20, nullable = false)
    private String timeFormat;
    @Column(name = "system_language", length = 8, nullable = false)
    private String systemLanguage;
    @Column(name = "measure", length = 1, nullable = false)
    private String measure;
    @Column(name = "linked_whse", length = 8, nullable = false)
    private String linkedWhse;
    @Column(name = "outside_whse", length = 1, nullable = false)
    private String outsideWhse;
    @Column(name = "temp_file_dir", length = 255)
    private String tempFileDir;
    @Column(name = "default_print_user", length = 30, nullable = false)
    private String defaultPrintUser;
    @Column(name = "default_truck_type", length = 8, nullable = false)
    private String defaultTruckType;
    @Column(name = "max_tasks_delete", nullable = false)
    private Integer maxTasksDelete;

    // ─── Receiving ───────────────────────────────────────────────────
    @Column(name = "rcpt_weight_tol", nullable = false)
    private Integer receivedWeightTolerance;
    @Column(name = "rwi_weight_type", length = 1, nullable = false)
    private String randomWeightType;
    @Column(name = "rcpt_weight_msg", length = 1, nullable = false)
    private String displayExpectedWeightError;
    @Column(name = "audit_rwi", length = 1, nullable = false)
    private String auditRandomWeight;
    @Column(name = "rcv_upd_inv_value", length = 1, nullable = false)
    private String updateInventoryAtReceipt;
    @Column(name = "cnt_per", length = 1, nullable = false)
    private String containerErrorRegistration;
    @Column(name = "pre_printed_labels", length = 1, nullable = false)
    private String prePrintedLabels;
    @Column(name = "palt_lbl_format", length = 20, nullable = false)
    private String palletLabelFormat;
    @Column(name = "pre_printed_palt_lbl_format", length = 20, nullable = false)
    private String prePrintedPaltLblFormat;
    @Column(name = "sum_lbl_count", nullable = false)
    private Integer summaryLabelCount;

    // ─── Verification ────────────────────────────────────────────────
    @Column(name = "verification_min_length", nullable = false)
    private Integer verificationMinLength;
    @Column(name = "verification_max_length", nullable = false)
    private Integer verificationMaxLength;
    @Column(name = "verification_alw_duplicates", length = 1, nullable = false)
    private String verificationAlwDuplicates;
    @Column(name = "verification_field_type", length = 1)
    private String verificationFieldType;
    @Column(name = "dft_item_verif", length = 1, nullable = false)
    private String defaultItemVerification;

    // ─── Orders + Shipments ──────────────────────────────────────────
    @Column(name = "order_reqd", length = 1, nullable = false)
    private String orderRequired;
    @Column(name = "order_reqd_update", length = 1, nullable = false)
    private String orderRequiredUpdate;
    @Column(name = "ord_stat", length = 1, nullable = false)
    private String ordStat;
    @Column(name = "ord_boh_change", length = 1, nullable = false)
    private String orderBohChange;
    @Column(name = "unique_order_reqd", length = 1, nullable = false)
    private String uniqueOrderReqd;
    @Column(name = "ord_assign_order", length = 1, nullable = false)
    private String ordAssignOrder;
    @Column(name = "chg_deliv_sched", length = 1, nullable = false)
    private String changeDeliverySchedule;
    @Column(name = "rel_no_order_num", length = 1)
    private String releaseWithoutOrderNum;
    @Column(name = "cofe_default_trader_type", length = 9, nullable = false)
    private String cofeDefaultTraderType;
    @Column(name = "break_ship_dept", length = 1, nullable = false)
    private String breakShipDepartment;
    @Column(name = "break_ship_vol", length = 1, nullable = false)
    private String breakShipVolume;
    @Column(name = "door_stock_avail", length = 1, nullable = false)
    private String doorStockAvailable;
    @Column(name = "dft_scan_method", length = 1, nullable = false)
    private String defaultScanMethod;
    @Column(name = "default_pbl_release_percent", nullable = false)
    private Integer defaultPblReleasePercent;
    @Column(name = "default_pbl_release_type", length = 1, nullable = false)
    private String defaultPblReleaseType;
    @Column(name = "update_pbl_pick_plan", length = 1, nullable = false)
    private String updatePblPickPlan;
    @Column(name = "release_unrouted", length = 1, nullable = false)
    private String releaseUnrouted;

    // ─── Delivery notes (dNote) ──────────────────────────────────────
    @Column(name = "dnote_generate_num", length = 1, nullable = false)
    private String dNoteGenerateNum;
    @Column(name = "dnote_number_basis", length = 1, nullable = false)
    private String dNoteNumberBasis;
    @Column(name = "dnote_report_name", length = 50, nullable = false)
    private String dNoteReportName;
    @Column(name = "dnote_summary_report_name", length = 50, nullable = false)
    private String dNoteSummaryReportName;
    @Column(name = "dnote_cntr_dnote_name", length = 50, nullable = false)
    private String dNoteCntrDNoteName;
    @Column(name = "dnote_auto_print_ship", length = 1, nullable = false)
    private String dNoteAutoPrintShip;
    @Column(name = "dnote_auto_print_summary", length = 1, nullable = false)
    private String dNoteAutoPrintSummary;
    @Column(name = "dnote_auto_print_cntr_dnote", length = 1, nullable = false)
    private String dNoteAutoPrintCntrDNote;

    // ─── Selection / PBL / picking ───────────────────────────────────
    @Column(name = "pick_bseq_increment", nullable = false)
    private Integer pickBseqIncrement;
    @Column(name = "start_pick_bseq", nullable = false)
    private Integer startPickBseq;
    @Column(name = "alw_rtng_only_open", length = 1, nullable = false)
    private String alwRtngOnlyOpen;
    @Column(name = "alw_rtng_reroute", length = 1, nullable = false)
    private String alwRtngReroute;
    @Column(name = "rtng_by_dept", length = 1, nullable = false)
    private String rtngByDept;
    @Column(name = "rtng_group", length = 8, nullable = false)
    private String rtngGroup;
    @Temporal(TemporalType.DATE)
    @Column(name = "last_rtng_date", nullable = false)
    private Date lastRtngDate;
    @Column(name = "apr_one_case", length = 1, nullable = false)
    private String aprOneCase;
    @Column(name = "apr_one_case_prof", length = 8, nullable = false)
    private String aprOneCaseProfile;
    @Column(name = "pern_req_case", length = 1, nullable = false)
    private String pernCaseRequired;
    @Column(name = "bnh_out_when_short", length = 1, nullable = false)
    private String bnhOutWhenShort;
    @Column(name = "high_pack_ltd", length = 1, nullable = false)
    private String highPackLetdown;
    @Column(name = "combine_pallets_in_pick", length = 1, nullable = false)
    private String combinePalletsInPick;
    @Column(name = "allow_item_span_ps", length = 1, nullable = false)
    private String allowItemSpanPs;
    @Column(name = "allow_void", length = 1, nullable = false)
    private String allowVoid;
    @Column(name = "max_pbl_rlse_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxPblRlsePct;
    @Column(name = "non_pbl_proc_seq", length = 8, nullable = false)
    private String nonPblProcSeq;
    @Column(name = "pbl_only_print_picked_pallets", length = 1, nullable = false)
    private String pblOnlyPrintPickedPallets;
    @Column(name = "dflt_process_outs_non_pbl", length = 1, nullable = false)
    private String defaultProcessOutsNonPbl;
    @Column(name = "auto_finish_time_incr", nullable = false)
    private Integer automationFinishTimeIncrement;
    @Column(name = "std_sort_threshold", nullable = false, precision = 10, scale = 2)
    private BigDecimal stdSortThreshold;

    // ─── Releases ────────────────────────────────────────────────────
    @Column(name = "release_max_cases", nullable = false)
    private Integer releaseMaxCases;
    @Column(name = "release_max_days_pbl", nullable = false)
    private Integer releaseMaxDaysPbl;
    @Column(name = "release_max_days_relp", nullable = false)
    private Integer releaseMaxDaysRelp;
    @Column(name = "release_max_orders", nullable = false)
    private Integer releaseMaxOrders;
    @Column(name = "release_max_waves", nullable = false)
    private Integer releaseMaxWaves;
    @Column(name = "relp_item_max_lock", nullable = false)
    private Integer relpMaxLockedItems;
    @Column(name = "alt_sect_asgn_seqn", length = 1)
    private String alternateAssignmentSequence;

    // ─── RF ──────────────────────────────────────────────────────────
    @Column(name = "rf_letdown", length = 1, nullable = false)
    private String rfUsedForLetdowns;
    @Column(name = "rf_putaway", length = 1, nullable = false)
    private String rfUsedForPutawayTask;
    @Column(name = "rf_refill", length = 1, nullable = false)
    private String rfUsedForBatchRefill;
    @Column(name = "rf_sel_wght_entry", length = 1, nullable = false)
    private String rfWeightEntry;
    @Column(name = "rf_slt_count_upd", length = 1, nullable = false)
    private String rfCreateCountAdjustment;
    @Column(name = "rf_smov", length = 1, nullable = false)
    private String rfUsedForSlotMove;
    @Column(name = "rf_xfer", length = 1, nullable = false)
    private String rfXfer;

    // ─── Labour standards ────────────────────────────────────────────
    @Column(name = "lms_letd", length = 1, nullable = false)
    private String useLetdownStd;
    @Column(name = "lms_recv", length = 1, nullable = false)
    private String useReceivingStd;
    @Column(name = "lms_selt", length = 1, nullable = false)
    private String useSelectionStd;
    @Column(name = "lms_frrf", length = 1, nullable = false)
    private String useFlowRackRefillStd;
    @Column(name = "lms_pbl", length = 1, nullable = false)
    private String selectionStdInstalled;
    @Column(name = "lms_supv", length = 1, nullable = false)
    private String requireSupervisorStartEmployee;

    // ─── External systems ────────────────────────────────────────────
    @Column(name = "eb_installed", length = 1, nullable = false)
    private String ebInstalled;
    @Column(name = "elms_installed", length = 1, nullable = false)
    private String elmsInstalled;
    @Column(name = "ep_installed", length = 1, nullable = false)
    private String epInstalled;
    @Column(name = "ewms_installed", length = 1, nullable = false)
    private String ewmsInstalled;

    // ─── Replication ─────────────────────────────────────────────────
    @Column(name = "item_replication_group", length = 8, nullable = false)
    private String itemReplicationGroup;
    @Column(name = "customer_replication_grp", length = 8, nullable = false)
    private String customerReplicationGroup;
    @Column(name = "vendor_replication_grp", length = 8, nullable = false)
    private String vendorReplicationGroup;

    // ─── Reports / Misc ──────────────────────────────────────────────
    @Column(name = "w44_pbln", length = 1, nullable = false)
    private String reportNonPBLItems;
    @Column(name = "w44_pbls", length = 1, nullable = false)
    private String reportNonPBLSlots;
    @Column(name = "w44_updt", length = 1, nullable = false)
    private String updatesPerformWR44;
    @Column(name = "w70_rest", length = 1, nullable = false)
    private String w70Rest;
    @Column(name = "grv_printer", length = 30, nullable = false)
    private String grvPrinter;
    @Column(name = "exc_iqr", length = 1, nullable = false)
    private String iqarGeneratesRecount;
    @Column(name = "exc_per", length = 1, nullable = false)
    private String scratchPernAutoRecount;
    @Column(name = "exc_rlp", length = 1, nullable = false)
    private String excRlp;
    @Column(name = "selt_weight_vol", nullable = false)
    private Integer seltWeightVol;
    @Column(name = "use_alloc_boh", length = 1, nullable = false)
    private String useAllocBoh;
    @Column(name = "use_apportionment", length = 1, nullable = false)
    private String useApportionment;
    @Column(name = "ranging_flag", length = 1, nullable = false)
    private String rangingFlag;
    @Column(name = "order_dmd_incl_relp23", length = 1, nullable = false)
    private String orderDmdInclRelp23;
    @Column(name = "create_order_ref_line", length = 1, nullable = false)
    private String createOrderRefLine;
    @Column(name = "delete_same_order_ps", length = 1, nullable = false)
    private String deleteSameOrderPlanShipment;
    @Column(name = "delete_item_movement", length = 1, nullable = false)
    private String deleteItemMovement;
    @Column(name = "concurrent_processing", length = 1, nullable = false)
    private String concurrentProcessing;

    /* ────── getters/setters ────── */

    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String v) { this.companyCode = v; }

    public Integer getAisleFieldLength() { return aisleFieldLength; }
    public void setAisleFieldLength(Integer v) { this.aisleFieldLength = v; }
    public Integer getBayFieldLength() { return bayFieldLength; }
    public void setBayFieldLength(Integer v) { this.bayFieldLength = v; }
    public String getProtectSlotLocation() { return protectSlotLocation; }
    public void setProtectSlotLocation(String v) { this.protectSlotLocation = v; }
    public Integer getItemFieldLength() { return itemFieldLength; }
    public void setItemFieldLength(Integer v) { this.itemFieldLength = v; }
    public String getItemFieldType() { return itemFieldType; }
    public void setItemFieldType(String v) { this.itemFieldType = v; }
    public Integer getVendorFieldLength() { return vendorFieldLength; }
    public void setVendorFieldLength(Integer v) { this.vendorFieldLength = v; }
    public String getVendorFieldType() { return vendorFieldType; }
    public void setVendorFieldType(String v) { this.vendorFieldType = v; }
    public Integer getCustomerFieldLength() { return customerFieldLength; }
    public void setCustomerFieldLength(Integer v) { this.customerFieldLength = v; }
    public String getCustomerFieldType() { return customerFieldType; }
    public void setCustomerFieldType(String v) { this.customerFieldType = v; }
    public Integer getDaysPerWeek() { return daysPerWeek; }
    public void setDaysPerWeek(Integer v) { this.daysPerWeek = v; }
    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String v) { this.dateFormat = v; }
    public String getTimeFormat() { return timeFormat; }
    public void setTimeFormat(String v) { this.timeFormat = v; }
    public String getSystemLanguage() { return systemLanguage; }
    public void setSystemLanguage(String v) { this.systemLanguage = v; }
    public String getMeasure() { return measure; }
    public void setMeasure(String v) { this.measure = v; }
    public String getLinkedWhse() { return linkedWhse; }
    public void setLinkedWhse(String v) { this.linkedWhse = v; }
    public String getOutsideWhse() { return outsideWhse; }
    public void setOutsideWhse(String v) { this.outsideWhse = v; }
    public String getTempFileDir() { return tempFileDir; }
    public void setTempFileDir(String v) { this.tempFileDir = v; }
    public String getDefaultPrintUser() { return defaultPrintUser; }
    public void setDefaultPrintUser(String v) { this.defaultPrintUser = v; }
    public String getDefaultTruckType() { return defaultTruckType; }
    public void setDefaultTruckType(String v) { this.defaultTruckType = v; }
    public Integer getMaxTasksDelete() { return maxTasksDelete; }
    public void setMaxTasksDelete(Integer v) { this.maxTasksDelete = v; }

    public Integer getReceivedWeightTolerance() { return receivedWeightTolerance; }
    public void setReceivedWeightTolerance(Integer v) { this.receivedWeightTolerance = v; }
    public String getRandomWeightType() { return randomWeightType; }
    public void setRandomWeightType(String v) { this.randomWeightType = v; }
    public String getDisplayExpectedWeightError() { return displayExpectedWeightError; }
    public void setDisplayExpectedWeightError(String v) { this.displayExpectedWeightError = v; }
    public String getAuditRandomWeight() { return auditRandomWeight; }
    public void setAuditRandomWeight(String v) { this.auditRandomWeight = v; }
    public String getUpdateInventoryAtReceipt() { return updateInventoryAtReceipt; }
    public void setUpdateInventoryAtReceipt(String v) { this.updateInventoryAtReceipt = v; }
    public String getContainerErrorRegistration() { return containerErrorRegistration; }
    public void setContainerErrorRegistration(String v) { this.containerErrorRegistration = v; }
    public String getPrePrintedLabels() { return prePrintedLabels; }
    public void setPrePrintedLabels(String v) { this.prePrintedLabels = v; }
    public String getPalletLabelFormat() { return palletLabelFormat; }
    public void setPalletLabelFormat(String v) { this.palletLabelFormat = v; }
    public String getPrePrintedPaltLblFormat() { return prePrintedPaltLblFormat; }
    public void setPrePrintedPaltLblFormat(String v) { this.prePrintedPaltLblFormat = v; }
    public Integer getSummaryLabelCount() { return summaryLabelCount; }
    public void setSummaryLabelCount(Integer v) { this.summaryLabelCount = v; }

    public Integer getVerificationMinLength() { return verificationMinLength; }
    public void setVerificationMinLength(Integer v) { this.verificationMinLength = v; }
    public Integer getVerificationMaxLength() { return verificationMaxLength; }
    public void setVerificationMaxLength(Integer v) { this.verificationMaxLength = v; }
    public String getVerificationAlwDuplicates() { return verificationAlwDuplicates; }
    public void setVerificationAlwDuplicates(String v) { this.verificationAlwDuplicates = v; }
    public String getVerificationFieldType() { return verificationFieldType; }
    public void setVerificationFieldType(String v) { this.verificationFieldType = v; }
    public String getDefaultItemVerification() { return defaultItemVerification; }
    public void setDefaultItemVerification(String v) { this.defaultItemVerification = v; }

    public String getOrderRequired() { return orderRequired; }
    public void setOrderRequired(String v) { this.orderRequired = v; }
    public String getOrderRequiredUpdate() { return orderRequiredUpdate; }
    public void setOrderRequiredUpdate(String v) { this.orderRequiredUpdate = v; }
    public String getOrdStat() { return ordStat; }
    public void setOrdStat(String v) { this.ordStat = v; }
    public String getOrderBohChange() { return orderBohChange; }
    public void setOrderBohChange(String v) { this.orderBohChange = v; }
    public String getUniqueOrderReqd() { return uniqueOrderReqd; }
    public void setUniqueOrderReqd(String v) { this.uniqueOrderReqd = v; }
    public String getOrdAssignOrder() { return ordAssignOrder; }
    public void setOrdAssignOrder(String v) { this.ordAssignOrder = v; }
    public String getChangeDeliverySchedule() { return changeDeliverySchedule; }
    public void setChangeDeliverySchedule(String v) { this.changeDeliverySchedule = v; }
    public String getReleaseWithoutOrderNum() { return releaseWithoutOrderNum; }
    public void setReleaseWithoutOrderNum(String v) { this.releaseWithoutOrderNum = v; }
    public String getCofeDefaultTraderType() { return cofeDefaultTraderType; }
    public void setCofeDefaultTraderType(String v) { this.cofeDefaultTraderType = v; }
    public String getBreakShipDepartment() { return breakShipDepartment; }
    public void setBreakShipDepartment(String v) { this.breakShipDepartment = v; }
    public String getBreakShipVolume() { return breakShipVolume; }
    public void setBreakShipVolume(String v) { this.breakShipVolume = v; }
    public String getDoorStockAvailable() { return doorStockAvailable; }
    public void setDoorStockAvailable(String v) { this.doorStockAvailable = v; }
    public String getDefaultScanMethod() { return defaultScanMethod; }
    public void setDefaultScanMethod(String v) { this.defaultScanMethod = v; }
    public Integer getDefaultPblReleasePercent() { return defaultPblReleasePercent; }
    public void setDefaultPblReleasePercent(Integer v) { this.defaultPblReleasePercent = v; }
    public String getDefaultPblReleaseType() { return defaultPblReleaseType; }
    public void setDefaultPblReleaseType(String v) { this.defaultPblReleaseType = v; }
    public String getUpdatePblPickPlan() { return updatePblPickPlan; }
    public void setUpdatePblPickPlan(String v) { this.updatePblPickPlan = v; }
    public String getReleaseUnrouted() { return releaseUnrouted; }
    public void setReleaseUnrouted(String v) { this.releaseUnrouted = v; }

    public String getDNoteGenerateNum() { return dNoteGenerateNum; }
    public void setDNoteGenerateNum(String v) { this.dNoteGenerateNum = v; }
    public String getDNoteNumberBasis() { return dNoteNumberBasis; }
    public void setDNoteNumberBasis(String v) { this.dNoteNumberBasis = v; }
    public String getDNoteReportName() { return dNoteReportName; }
    public void setDNoteReportName(String v) { this.dNoteReportName = v; }
    public String getDNoteSummaryReportName() { return dNoteSummaryReportName; }
    public void setDNoteSummaryReportName(String v) { this.dNoteSummaryReportName = v; }
    public String getDNoteCntrDNoteName() { return dNoteCntrDNoteName; }
    public void setDNoteCntrDNoteName(String v) { this.dNoteCntrDNoteName = v; }
    public String getDNoteAutoPrintShip() { return dNoteAutoPrintShip; }
    public void setDNoteAutoPrintShip(String v) { this.dNoteAutoPrintShip = v; }
    public String getDNoteAutoPrintSummary() { return dNoteAutoPrintSummary; }
    public void setDNoteAutoPrintSummary(String v) { this.dNoteAutoPrintSummary = v; }
    public String getDNoteAutoPrintCntrDNote() { return dNoteAutoPrintCntrDNote; }
    public void setDNoteAutoPrintCntrDNote(String v) { this.dNoteAutoPrintCntrDNote = v; }

    public Integer getPickBseqIncrement() { return pickBseqIncrement; }
    public void setPickBseqIncrement(Integer v) { this.pickBseqIncrement = v; }
    public Integer getStartPickBseq() { return startPickBseq; }
    public void setStartPickBseq(Integer v) { this.startPickBseq = v; }
    public String getAlwRtngOnlyOpen() { return alwRtngOnlyOpen; }
    public void setAlwRtngOnlyOpen(String v) { this.alwRtngOnlyOpen = v; }
    public String getAlwRtngReroute() { return alwRtngReroute; }
    public void setAlwRtngReroute(String v) { this.alwRtngReroute = v; }
    public String getRtngByDept() { return rtngByDept; }
    public void setRtngByDept(String v) { this.rtngByDept = v; }
    public String getRtngGroup() { return rtngGroup; }
    public void setRtngGroup(String v) { this.rtngGroup = v; }
    public Date getLastRtngDate() { return lastRtngDate; }
    public void setLastRtngDate(Date v) { this.lastRtngDate = v; }
    public String getAprOneCase() { return aprOneCase; }
    public void setAprOneCase(String v) { this.aprOneCase = v; }
    public String getAprOneCaseProfile() { return aprOneCaseProfile; }
    public void setAprOneCaseProfile(String v) { this.aprOneCaseProfile = v; }
    public String getPernCaseRequired() { return pernCaseRequired; }
    public void setPernCaseRequired(String v) { this.pernCaseRequired = v; }
    public String getBnhOutWhenShort() { return bnhOutWhenShort; }
    public void setBnhOutWhenShort(String v) { this.bnhOutWhenShort = v; }
    public String getHighPackLetdown() { return highPackLetdown; }
    public void setHighPackLetdown(String v) { this.highPackLetdown = v; }
    public String getCombinePalletsInPick() { return combinePalletsInPick; }
    public void setCombinePalletsInPick(String v) { this.combinePalletsInPick = v; }
    public String getAllowItemSpanPs() { return allowItemSpanPs; }
    public void setAllowItemSpanPs(String v) { this.allowItemSpanPs = v; }
    public String getAllowVoid() { return allowVoid; }
    public void setAllowVoid(String v) { this.allowVoid = v; }
    public BigDecimal getMaxPblRlsePct() { return maxPblRlsePct; }
    public void setMaxPblRlsePct(BigDecimal v) { this.maxPblRlsePct = v; }
    public String getNonPblProcSeq() { return nonPblProcSeq; }
    public void setNonPblProcSeq(String v) { this.nonPblProcSeq = v; }
    public String getPblOnlyPrintPickedPallets() { return pblOnlyPrintPickedPallets; }
    public void setPblOnlyPrintPickedPallets(String v) { this.pblOnlyPrintPickedPallets = v; }
    public String getDefaultProcessOutsNonPbl() { return defaultProcessOutsNonPbl; }
    public void setDefaultProcessOutsNonPbl(String v) { this.defaultProcessOutsNonPbl = v; }
    public Integer getAutomationFinishTimeIncrement() { return automationFinishTimeIncrement; }
    public void setAutomationFinishTimeIncrement(Integer v) { this.automationFinishTimeIncrement = v; }
    public BigDecimal getStdSortThreshold() { return stdSortThreshold; }
    public void setStdSortThreshold(BigDecimal v) { this.stdSortThreshold = v; }

    public Integer getReleaseMaxCases() { return releaseMaxCases; }
    public void setReleaseMaxCases(Integer v) { this.releaseMaxCases = v; }
    public Integer getReleaseMaxDaysPbl() { return releaseMaxDaysPbl; }
    public void setReleaseMaxDaysPbl(Integer v) { this.releaseMaxDaysPbl = v; }
    public Integer getReleaseMaxDaysRelp() { return releaseMaxDaysRelp; }
    public void setReleaseMaxDaysRelp(Integer v) { this.releaseMaxDaysRelp = v; }
    public Integer getReleaseMaxOrders() { return releaseMaxOrders; }
    public void setReleaseMaxOrders(Integer v) { this.releaseMaxOrders = v; }
    public Integer getReleaseMaxWaves() { return releaseMaxWaves; }
    public void setReleaseMaxWaves(Integer v) { this.releaseMaxWaves = v; }
    public Integer getRelpMaxLockedItems() { return relpMaxLockedItems; }
    public void setRelpMaxLockedItems(Integer v) { this.relpMaxLockedItems = v; }
    public String getAlternateAssignmentSequence() { return alternateAssignmentSequence; }
    public void setAlternateAssignmentSequence(String v) { this.alternateAssignmentSequence = v; }

    public String getRfUsedForLetdowns() { return rfUsedForLetdowns; }
    public void setRfUsedForLetdowns(String v) { this.rfUsedForLetdowns = v; }
    public String getRfUsedForPutawayTask() { return rfUsedForPutawayTask; }
    public void setRfUsedForPutawayTask(String v) { this.rfUsedForPutawayTask = v; }
    public String getRfUsedForBatchRefill() { return rfUsedForBatchRefill; }
    public void setRfUsedForBatchRefill(String v) { this.rfUsedForBatchRefill = v; }
    public String getRfWeightEntry() { return rfWeightEntry; }
    public void setRfWeightEntry(String v) { this.rfWeightEntry = v; }
    public String getRfCreateCountAdjustment() { return rfCreateCountAdjustment; }
    public void setRfCreateCountAdjustment(String v) { this.rfCreateCountAdjustment = v; }
    public String getRfUsedForSlotMove() { return rfUsedForSlotMove; }
    public void setRfUsedForSlotMove(String v) { this.rfUsedForSlotMove = v; }
    public String getRfXfer() { return rfXfer; }
    public void setRfXfer(String v) { this.rfXfer = v; }

    public String getUseLetdownStd() { return useLetdownStd; }
    public void setUseLetdownStd(String v) { this.useLetdownStd = v; }
    public String getUseReceivingStd() { return useReceivingStd; }
    public void setUseReceivingStd(String v) { this.useReceivingStd = v; }
    public String getUseSelectionStd() { return useSelectionStd; }
    public void setUseSelectionStd(String v) { this.useSelectionStd = v; }
    public String getUseFlowRackRefillStd() { return useFlowRackRefillStd; }
    public void setUseFlowRackRefillStd(String v) { this.useFlowRackRefillStd = v; }
    public String getSelectionStdInstalled() { return selectionStdInstalled; }
    public void setSelectionStdInstalled(String v) { this.selectionStdInstalled = v; }
    public String getRequireSupervisorStartEmployee() { return requireSupervisorStartEmployee; }
    public void setRequireSupervisorStartEmployee(String v) { this.requireSupervisorStartEmployee = v; }

    public String getEbInstalled() { return ebInstalled; }
    public void setEbInstalled(String v) { this.ebInstalled = v; }
    public String getElmsInstalled() { return elmsInstalled; }
    public void setElmsInstalled(String v) { this.elmsInstalled = v; }
    public String getEpInstalled() { return epInstalled; }
    public void setEpInstalled(String v) { this.epInstalled = v; }
    public String getEwmsInstalled() { return ewmsInstalled; }
    public void setEwmsInstalled(String v) { this.ewmsInstalled = v; }

    public String getItemReplicationGroup() { return itemReplicationGroup; }
    public void setItemReplicationGroup(String v) { this.itemReplicationGroup = v; }
    public String getCustomerReplicationGroup() { return customerReplicationGroup; }
    public void setCustomerReplicationGroup(String v) { this.customerReplicationGroup = v; }
    public String getVendorReplicationGroup() { return vendorReplicationGroup; }
    public void setVendorReplicationGroup(String v) { this.vendorReplicationGroup = v; }

    public String getReportNonPBLItems() { return reportNonPBLItems; }
    public void setReportNonPBLItems(String v) { this.reportNonPBLItems = v; }
    public String getReportNonPBLSlots() { return reportNonPBLSlots; }
    public void setReportNonPBLSlots(String v) { this.reportNonPBLSlots = v; }
    public String getUpdatesPerformWR44() { return updatesPerformWR44; }
    public void setUpdatesPerformWR44(String v) { this.updatesPerformWR44 = v; }
    public String getW70Rest() { return w70Rest; }
    public void setW70Rest(String v) { this.w70Rest = v; }
    public String getGrvPrinter() { return grvPrinter; }
    public void setGrvPrinter(String v) { this.grvPrinter = v; }
    public String getIqarGeneratesRecount() { return iqarGeneratesRecount; }
    public void setIqarGeneratesRecount(String v) { this.iqarGeneratesRecount = v; }
    public String getScratchPernAutoRecount() { return scratchPernAutoRecount; }
    public void setScratchPernAutoRecount(String v) { this.scratchPernAutoRecount = v; }
    public String getExcRlp() { return excRlp; }
    public void setExcRlp(String v) { this.excRlp = v; }
    public Integer getSeltWeightVol() { return seltWeightVol; }
    public void setSeltWeightVol(Integer v) { this.seltWeightVol = v; }
    public String getUseAllocBoh() { return useAllocBoh; }
    public void setUseAllocBoh(String v) { this.useAllocBoh = v; }
    public String getUseApportionment() { return useApportionment; }
    public void setUseApportionment(String v) { this.useApportionment = v; }
    public String getRangingFlag() { return rangingFlag; }
    public void setRangingFlag(String v) { this.rangingFlag = v; }
    public String getOrderDmdInclRelp23() { return orderDmdInclRelp23; }
    public void setOrderDmdInclRelp23(String v) { this.orderDmdInclRelp23 = v; }
    public String getCreateOrderRefLine() { return createOrderRefLine; }
    public void setCreateOrderRefLine(String v) { this.createOrderRefLine = v; }
    public String getDeleteSameOrderPlanShipment() { return deleteSameOrderPlanShipment; }
    public void setDeleteSameOrderPlanShipment(String v) { this.deleteSameOrderPlanShipment = v; }
    public String getDeleteItemMovement() { return deleteItemMovement; }
    public void setDeleteItemMovement(String v) { this.deleteItemMovement = v; }
    public String getConcurrentProcessing() { return concurrentProcessing; }
    public void setConcurrentProcessing(String v) { this.concurrentProcessing = v; }

    /* ────── audit fields (from MaintainableTranUser) ────── */

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

    public Date getMaintenanceDate() { return maintenanceDate; }
    @Override
    public void setMaintenanceDate(Date v) { this.maintenanceDate = v; }
    public Date getMaintenanceTime() { return maintenanceTime; }
    @Override
    public void setMaintenanceTime(Date v) { this.maintenanceTime = v; }
    public String getMaintenanceUser() { return maintenanceUser; }
    @Override
    public void setMaintenanceUser(String v) { this.maintenanceUser = v; }
    public String getMaintenanceTran() { return maintenanceTran; }
    @Override
    public void setMaintenanceTran(String v) { this.maintenanceTran = v; }
}
