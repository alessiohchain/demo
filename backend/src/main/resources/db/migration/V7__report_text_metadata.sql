-- V7: seed screen_metadata + menu_item rows for the reportText.maintenance
-- workflow. Fastpath RPTM. Field names mirror the Java field names on the
-- ReportText entity (reportName / language / textSequence / reportText /
-- maintenanceDate / maintenanceUser).

INSERT INTO demoschema.screen_metadata (
    workflow, fastpath, payload, description,
    created_at, updated_at, created_by
) VALUES (
    'reportText.maintenance',
    'RPTM',
    jsonb_build_object(
        'name', 'reportText.maintenance',
        'shortName', 'RPTM',
        'version', 1,
        'metadataList', jsonb_build_array(
            -- Popup form for Add / Edit. Composite PK fields are present in
            -- the data envelope so cmdUpdate / cmdDelete can reconstruct
            -- the Id on the server side.
            jsonb_build_object(
                'kind', 'form',
                'name', 'reportTextForm',
                'columns', 1,
                'fieldList', jsonb_build_array(
                    jsonb_build_object(
                        'id', 'reportName', 'name', 'reportName',
                        'label', 'Report', 'type', 'STRING',
                        'lookupDataKey', 'ReportProgram',
                        'maxlength', 64, 'nullable', false, 'focus', true
                    ),
                    jsonb_build_object(
                        'id', 'language', 'name', 'language',
                        'label', 'Language', 'type', 'STRING',
                        'lookupDataKey', 'Language',
                        'maxlength', 8, 'nullable', false,
                        'defaultValue', 'EN'
                    ),
                    jsonb_build_object(
                        'id', 'textSequence', 'name', 'textSequence',
                        'label', 'Line #', 'type', 'INTEGER',
                        'nullable', false
                    ),
                    jsonb_build_object(
                        'id', 'reportText', 'name', 'reportText',
                        'label', 'Text', 'type', 'STRING',
                        'maxlength', 1000, 'nullable', false
                    )
                )
            ),
            jsonb_build_object(
                'kind', 'grid',
                'name', 'reportTextGrid',
                'title', 'Report Text',
                'checkboxSelection', true,
                'inlineEdit', false,
                'rows', 25,
                'searchMetadata', jsonb_build_object(
                    'kind', 'search',
                    'name', 'reportTextForm'
                ),
                'fieldList', jsonb_build_array(
                    jsonb_build_object('id', 'reportName',      'name', 'reportName',      'label', 'Report',     'type', 'STRING'),
                    jsonb_build_object('id', 'language',        'name', 'language',        'label', 'Language',   'type', 'STRING'),
                    jsonb_build_object('id', 'textSequence',    'name', 'textSequence',    'label', 'Line #',     'type', 'INTEGER', 'align', 'right'),
                    jsonb_build_object('id', 'reportText',      'name', 'reportText',      'label', 'Text',       'type', 'STRING'),
                    jsonb_build_object('id', 'maintenanceUser', 'name', 'maintenanceUser', 'label', 'Maint user', 'type', 'STRING'),
                    jsonb_build_object('id', 'maintenanceDate', 'name', 'maintenanceDate', 'label', 'Maint date', 'type', 'DATE')
                ),
                'toolbarMetadata', jsonb_build_object(
                    'kind', 'toolbar',
                    'name', 'reportTextToolbar',
                    'actionButtons', jsonb_build_array(
                        jsonb_build_object('id', 'cmd_search', 'name', 'cmd_search', 'value', 'Search', 'command', 'cmd_search', 'modelType', 'none'),
                        jsonb_build_object('id', 'cmd_create', 'name', 'cmd_create', 'value', 'Add',    'command', 'cmd_create', 'modelType', 'forms',       'validate', true),
                        jsonb_build_object('id', 'cmd_update', 'name', 'cmd_update', 'value', 'Edit',   'command', 'cmd_update', 'modelType', 'form_single', 'validate', true),
                        jsonb_build_object('id', 'cmd_delete', 'name', 'cmd_delete', 'value', 'Delete', 'command', 'cmd_delete', 'modelType', 'selected')
                    )
                )
            )
        )
    ),
    'reportText.maintenance — Phase D demo target, engine-driven CRUD over report_text scoped by company.',
    NOW(), NOW(), 'system'
);

-- Menu: Reports folder + Report Text leaf under it.
INSERT INTO demoschema.menu_item (
    code, label, fastpath, workflow, sort_order,
    created_at, updated_at, created_by
) VALUES
    ('reports', 'Reports', NULL, NULL, 10, NOW(), NOW(), 'system');

INSERT INTO demoschema.menu_item (
    parent_id, code, label, fastpath, workflow, sort_order,
    created_at, updated_at, created_by
)
SELECT id, 'report-text', 'Report Text', 'RPTM', 'reportText.maintenance', 10,
       NOW(), NOW(), 'system'
FROM demoschema.menu_item
WHERE code = 'reports';
