-- V18: SCWT_SYS_PARMS — WSPM (System Parameters Maintenance) screen.
--
-- Mirrors CSnx's SCWT_SYS_PARMS shape (column names + types lowercased
-- for Postgres) with two simplifications:
--   * PK reduced from (cpy_cd, facl, whse) to (cpy_cd) only. Demo is
--     single-tenant-per-company; no facility/warehouse domain entities.
--   * Inherited audit columns (maint_*, update_serial, created_at/by,
--     updated_at/by) come from BaseEntity hierarchy — same shape as
--     every other SCWT_* table in demo.
--
-- Conceptual reference:
--   C:\software\projects\CSnx\src\za\co\csnx\model\csnx\SysParameters.java
--   C:\software\projects\CSnx\migrations\scripts\20191023111549_CSNX-3601_WSPM.sql

CREATE TABLE demoschema.scwt_sys_parms (
    cpy_cd                          VARCHAR(15)  NOT NULL,

    -- ─── System (slot geometry, formats, language, paths) ────────────
    sys_slot_aisl                   INTEGER      NOT NULL DEFAULT 4,
    sys_slot_bay                    INTEGER      NOT NULL DEFAULT 4,
    sys_slot_locn                   VARCHAR(1)   NOT NULL DEFAULT 'N',
    item_fld_size                   INTEGER      NOT NULL DEFAULT 15,
    item_fld_type                   VARCHAR(1)   NOT NULL DEFAULT 'A',
    vendor_fld_size                 INTEGER      NOT NULL DEFAULT 15,
    vendor_fld_type                 VARCHAR(1)   NOT NULL DEFAULT 'A',
    customer_fld_size               INTEGER      NOT NULL DEFAULT 15,
    customer_fld_type               VARCHAR(1)   NOT NULL DEFAULT 'A',
    days_per_week                   INTEGER      NOT NULL DEFAULT 5,
    date_format                     VARCHAR(20)  NOT NULL DEFAULT 'yyyy-MM-dd',
    time_format                     VARCHAR(20)  NOT NULL DEFAULT 'HH:mm:ss',
    system_language                 VARCHAR(8)   NOT NULL DEFAULT 'EN',
    measure                         VARCHAR(1)   NOT NULL DEFAULT 'M',
    linked_whse                     VARCHAR(8)   NOT NULL DEFAULT '',
    outside_whse                    VARCHAR(1)   NOT NULL DEFAULT 'N',
    temp_file_dir                   VARCHAR(255),
    default_print_user              VARCHAR(30)  NOT NULL DEFAULT '',
    default_truck_type              VARCHAR(8)   NOT NULL DEFAULT '',
    max_tasks_delete                INTEGER      NOT NULL DEFAULT 1000,

    -- ─── Receiving (weight, labels, audit) ───────────────────────────
    rcpt_weight_tol                 INTEGER      NOT NULL DEFAULT 5,
    rwi_weight_type                 VARCHAR(1)   NOT NULL DEFAULT 'A',
    rcpt_weight_msg                 VARCHAR(1)   NOT NULL DEFAULT 'Y',
    audit_rwi                       VARCHAR(1)   NOT NULL DEFAULT 'N',
    rcv_upd_inv_value               VARCHAR(1)   NOT NULL DEFAULT 'Y',
    cnt_per                         VARCHAR(1)   NOT NULL DEFAULT 'N',
    pre_printed_labels              VARCHAR(1)   NOT NULL DEFAULT 'N',
    palt_lbl_format                 VARCHAR(20)  NOT NULL DEFAULT '',
    pre_printed_palt_lbl_format     VARCHAR(20)  NOT NULL DEFAULT '',
    sum_lbl_count                   INTEGER      NOT NULL DEFAULT 0,

    -- ─── Verification (code length rules) ────────────────────────────
    verification_min_length         INTEGER      NOT NULL DEFAULT 4,
    verification_max_length         INTEGER      NOT NULL DEFAULT 32,
    verification_alw_duplicates     VARCHAR(1)   NOT NULL DEFAULT 'N',
    verification_field_type         VARCHAR(1),
    dft_item_verif                  VARCHAR(1)   NOT NULL DEFAULT 'N',

    -- ─── Orders + Shipments ──────────────────────────────────────────
    order_reqd                      VARCHAR(1)   NOT NULL DEFAULT 'Y',
    order_reqd_update               VARCHAR(1)   NOT NULL DEFAULT 'Y',
    ord_stat                        VARCHAR(1)   NOT NULL DEFAULT 'A',
    ord_boh_change                  VARCHAR(1)   NOT NULL DEFAULT 'N',
    unique_order_reqd               VARCHAR(1)   NOT NULL DEFAULT 'Y',
    ord_assign_order                VARCHAR(1)   NOT NULL DEFAULT 'N',
    chg_deliv_sched                 VARCHAR(1)   NOT NULL DEFAULT 'N',
    rel_no_order_num                VARCHAR(1),
    cofe_default_trader_type        VARCHAR(9)   NOT NULL DEFAULT 'S',
    break_ship_dept                 VARCHAR(1)   NOT NULL DEFAULT 'N',
    break_ship_vol                  VARCHAR(1)   NOT NULL DEFAULT 'N',
    door_stock_avail                VARCHAR(1)   NOT NULL DEFAULT 'N',
    dft_scan_method                 VARCHAR(1)   NOT NULL DEFAULT 'B',
    default_pbl_release_percent     INTEGER      NOT NULL DEFAULT 80,
    default_pbl_release_type        VARCHAR(1)   NOT NULL DEFAULT 'P',
    update_pbl_pick_plan            VARCHAR(1)   NOT NULL DEFAULT 'N',
    release_unrouted                VARCHAR(1)   NOT NULL DEFAULT 'N',

    -- ─── Delivery notes (dNote) ──────────────────────────────────────
    dnote_generate_num              VARCHAR(1)   NOT NULL DEFAULT 'N',
    dnote_number_basis              VARCHAR(1)   NOT NULL DEFAULT '',
    dnote_report_name               VARCHAR(50)  NOT NULL DEFAULT '',
    dnote_summary_report_name       VARCHAR(50)  NOT NULL DEFAULT '',
    dnote_cntr_dnote_name           VARCHAR(50)  NOT NULL DEFAULT '',
    dnote_auto_print_ship           VARCHAR(1)   NOT NULL DEFAULT 'N',
    dnote_auto_print_summary        VARCHAR(1)   NOT NULL DEFAULT 'N',
    dnote_auto_print_cntr_dnote     VARCHAR(1)   NOT NULL DEFAULT 'N',

    -- ─── Selection / PBL / picking ───────────────────────────────────
    pick_bseq_increment             INTEGER      NOT NULL DEFAULT 10,
    start_pick_bseq                 INTEGER      NOT NULL DEFAULT 100,
    alw_rtng_only_open              VARCHAR(1)   NOT NULL DEFAULT 'Y',
    alw_rtng_reroute                VARCHAR(1)   NOT NULL DEFAULT 'N',
    rtng_by_dept                    VARCHAR(1)   NOT NULL DEFAULT 'N',
    rtng_group                      VARCHAR(8)   NOT NULL DEFAULT '',
    last_rtng_date                  DATE         NOT NULL DEFAULT CURRENT_DATE,
    apr_one_case                    VARCHAR(1)   NOT NULL DEFAULT 'N',
    apr_one_case_prof               VARCHAR(8)   NOT NULL DEFAULT '',
    pern_req_case                   VARCHAR(1)   NOT NULL DEFAULT 'N',
    bnh_out_when_short              VARCHAR(1)   NOT NULL DEFAULT 'N',
    high_pack_ltd                   VARCHAR(1)   NOT NULL DEFAULT 'N',
    combine_pallets_in_pick         VARCHAR(1)   NOT NULL DEFAULT 'N',
    allow_item_span_ps              VARCHAR(1)   NOT NULL DEFAULT 'N',
    allow_void                      VARCHAR(1)   NOT NULL DEFAULT 'Y',
    max_pbl_rlse_pct                NUMERIC(5,2) NOT NULL DEFAULT 100.00,
    non_pbl_proc_seq                VARCHAR(8)   NOT NULL DEFAULT '',
    pbl_only_print_picked_pallets   VARCHAR(1)   NOT NULL DEFAULT 'N',
    dflt_process_outs_non_pbl       VARCHAR(1)   NOT NULL DEFAULT 'N',
    auto_finish_time_incr           INTEGER      NOT NULL DEFAULT 5,
    std_sort_threshold              NUMERIC(10,2) NOT NULL DEFAULT 0.00,

    -- ─── Releases ────────────────────────────────────────────────────
    release_max_cases               INTEGER      NOT NULL DEFAULT 1000,
    release_max_days_pbl            INTEGER      NOT NULL DEFAULT 7,
    release_max_days_relp           INTEGER      NOT NULL DEFAULT 14,
    release_max_orders              INTEGER      NOT NULL DEFAULT 100,
    release_max_waves               INTEGER      NOT NULL DEFAULT 10,
    relp_item_max_lock              INTEGER      NOT NULL DEFAULT 50,
    alt_sect_asgn_seqn              VARCHAR(1),

    -- ─── RF (radio-frequency device behaviour) ──────────────────────
    rf_letdown                      VARCHAR(1)   NOT NULL DEFAULT 'Y',
    rf_putaway                      VARCHAR(1)   NOT NULL DEFAULT 'Y',
    rf_refill                       VARCHAR(1)   NOT NULL DEFAULT 'Y',
    rf_sel_wght_entry               VARCHAR(1)   NOT NULL DEFAULT 'N',
    rf_slt_count_upd                VARCHAR(1)   NOT NULL DEFAULT 'N',
    rf_smov                         VARCHAR(1)   NOT NULL DEFAULT 'Y',
    rf_xfer                         VARCHAR(1)   NOT NULL DEFAULT 'Y',

    -- ─── Labour standards ────────────────────────────────────────────
    lms_letd                        VARCHAR(1)   NOT NULL DEFAULT 'N',
    lms_recv                        VARCHAR(1)   NOT NULL DEFAULT 'N',
    lms_selt                        VARCHAR(1)   NOT NULL DEFAULT 'N',
    lms_frrf                        VARCHAR(1)   NOT NULL DEFAULT 'N',
    lms_pbl                         VARCHAR(1)   NOT NULL DEFAULT 'N',
    lms_supv                        VARCHAR(1)   NOT NULL DEFAULT 'N',

    -- ─── External systems installed ─────────────────────────────────
    eb_installed                    VARCHAR(1)   NOT NULL DEFAULT 'N',
    elms_installed                  VARCHAR(1)   NOT NULL DEFAULT 'N',
    ep_installed                    VARCHAR(1)   NOT NULL DEFAULT 'N',
    ewms_installed                  VARCHAR(1)   NOT NULL DEFAULT 'Y',

    -- ─── Replication (cross-warehouse data sharing) ────────────────
    item_replication_group          VARCHAR(8)   NOT NULL DEFAULT '',
    customer_replication_grp        VARCHAR(8)   NOT NULL DEFAULT '',
    vendor_replication_grp          VARCHAR(8)   NOT NULL DEFAULT '',

    -- ─── Reports / W44 / W70 / Misc ────────────────────────────────
    w44_pbln                        VARCHAR(1)   NOT NULL DEFAULT 'N',
    w44_pbls                        VARCHAR(1)   NOT NULL DEFAULT 'N',
    w44_updt                        VARCHAR(1)   NOT NULL DEFAULT 'N',
    w70_rest                        VARCHAR(1)   NOT NULL DEFAULT 'N',
    grv_printer                     VARCHAR(30)  NOT NULL DEFAULT '',
    exc_iqr                         VARCHAR(1)   NOT NULL DEFAULT 'N',
    exc_per                         VARCHAR(1)   NOT NULL DEFAULT 'N',
    exc_rlp                         VARCHAR(1)   NOT NULL DEFAULT 'N',
    selt_weight_vol                 INTEGER      NOT NULL DEFAULT 50,
    use_alloc_boh                   VARCHAR(1)   NOT NULL DEFAULT 'N',
    use_apportionment               VARCHAR(1)   NOT NULL DEFAULT 'N',
    ranging_flag                    VARCHAR(1)   NOT NULL DEFAULT 'N',
    order_dmd_incl_relp23           VARCHAR(1)   NOT NULL DEFAULT 'N',
    create_order_ref_line           VARCHAR(1)   NOT NULL DEFAULT 'N',
    delete_same_order_ps            VARCHAR(1)   NOT NULL DEFAULT 'N',
    delete_item_movement            VARCHAR(1)   NOT NULL DEFAULT 'N',
    concurrent_processing           VARCHAR(1)   NOT NULL DEFAULT 'N',

    -- ─── Audit + version (BaseEntity hierarchy) ────────────────────
    maint_date                      DATE         NOT NULL DEFAULT CURRENT_DATE,
    maint_time                      TIME         NOT NULL DEFAULT CURRENT_TIME,
    maint_user                      VARCHAR(30),
    maint_tran                      VARCHAR(30),
    created_at                      TIMESTAMPTZ  NOT NULL,
    updated_at                      TIMESTAMPTZ  NOT NULL,
    created_by                      VARCHAR(100),
    updated_by                      VARCHAR(100),
    update_serial                   BIGINT       NOT NULL DEFAULT 1,

    PRIMARY KEY (cpy_cd)
);

-- Seed: one row for the WCS company so WSPM has something to load on
-- first visit. All other columns take their DEFAULT values.
INSERT INTO demoschema.scwt_sys_parms (cpy_cd, created_at, updated_at, created_by, updated_by, maint_user, maint_tran)
VALUES ('WCS', NOW(), NOW(), 'system', 'system', 'system', 'INSERT');
