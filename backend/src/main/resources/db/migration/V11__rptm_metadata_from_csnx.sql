-- V11: replace the hand-rolled reportText.maintenance metadata with the
-- exact shape csnx-ui's server emits. Captured by intercepting
-- GET /api/metadata?workflow=reportText.maintenance on the running CSnx
-- instance and dumped to demo/rptm-real.json for reference.
--
-- Key differences from V10:
--  • metadataList has ONE entry of kind="search" wrapping formMetadata +
--    gridMetadata (V10 had them as siblings, which broke findPopupForm
--    + Search-dialog wiring).
--  • Fields: maintenanceTran (hidden, fixedValue=RPTM), language (LNG
--    ComboBox), reportName, textSequence, reportText. NO maintenance
--    audit columns surfaced.
--  • language uses lookupDataKey="LNG" (not "Language"). LNG VVD seeded
--    below.
--  • Most fields defaultEditableState=false (read-only on update); only
--    reportText defaults to editable. Add flips them editable through
--    the engine's state machine.
--  • Toolbar: action (Add, Copy, Delete, Update, Close) + navigation
--    (Search, Print). Filter is NOT a button on this screen.
--  • Search + Print are navigation-group local=true.

DELETE FROM demoschema.screen_metadata WHERE workflow = 'reportText.maintenance';

INSERT INTO demoschema.screen_metadata (
    workflow, fastpath, payload, description,
    created_at, updated_at, created_by
) VALUES (
    'reportText.maintenance',
    'RPTM',
    jsonb_build_object(
        'name', 'reportText.maintenance',
        'shortName', 'RPTM',
        'language', 'EN',
        'action', 'enable_edit',
        'version', 1,
        'autoRefreshTime', 0,
        'metadataList', jsonb_build_array(
            jsonb_build_object(
                'kind', 'search',
                'name', 'reportText.maintenance',
                'workflow', 'reportText.maintenance',
                'defaultVisibleState', true,
                'defaultEditableState', true,
                'rows', 50,
                'overrideState', false,
                'checkboxSelection', true,
                'formMetadata', jsonb_build_object(
                    'kind', 'form',
                    'name', 'reportText.maintenance',
                    'workflow', 'reportText.maintenance',
                    'defaultVisibleState', true,
                    'defaultEditableState', true,
                    'columns', 1,
                    'headerAlignment', 'left',
                    'footerAlignment', 'left',
                    'fieldNamesNoRights', jsonb_build_array(),
                    'fieldList', jsonb_build_array(
                        jsonb_build_object('id', '5727', 'name', 'maintenanceTran', 'label', '', 'type', 'String', 'fixedValue', 'RPTM',
                            'defaultVisibleState', false, 'defaultEditableState', true,
                            'nullable', false, 'searchNullable', true, 'caseSensitive', false, 'length', 10, 'emptyRow', true),
                        jsonb_build_object('id', '5728', 'name', 'language', 'label', 'Language', 'type', 'String', 'uiType', 'ComboBox', 'lookupDataKey', 'LNG',
                            'defaultVisibleState', true, 'defaultEditableState', false,
                            'nullable', false, 'searchNullable', true, 'caseSensitive', false, 'length', 10, 'emptyRow', true),
                        jsonb_build_object('id', '5729', 'name', 'reportName', 'label', 'Report Name', 'type', 'String',
                            'defaultVisibleState', true, 'defaultEditableState', false,
                            'nullable', false, 'searchNullable', true, 'caseSensitive', false, 'maxlength', 10, 'length', 10, 'emptyRow', true),
                        jsonb_build_object('id', '5730', 'name', 'textSequence', 'label', 'Report Text Sequence', 'type', 'Integer',
                            'defaultVisibleState', true, 'defaultEditableState', false,
                            'nullable', false, 'searchNullable', true, 'caseSensitive', false, 'maxlength', 4, 'length', 4, 'emptyRow', true),
                        jsonb_build_object('id', '5731', 'name', 'reportText', 'label', 'Report Text', 'type', 'String',
                            'defaultVisibleState', true, 'defaultEditableState', true,
                            'nullable', false, 'searchNullable', true, 'caseSensitive', true, 'maxlength', 132, 'length', 45, 'emptyRow', true)
                    )
                ),
                'gridMetadata', jsonb_build_object(
                    'kind', 'grid',
                    'name', 'reportText.maintenance',
                    'workflow', 'reportText.maintenance',
                    'defaultVisibleState', true,
                    'defaultEditableState', true,
                    'title', '',
                    'rows', 50,
                    'overrideState', false,
                    'checkboxSelection', true,
                    'fieldList', jsonb_build_array(
                        jsonb_build_object('id', '5727', 'name', 'maintenanceTran', 'label', '', 'type', 'String', 'fixedValue', 'RPTM',
                            'defaultVisibleState', false, 'defaultEditableState', true,
                            'nullable', false, 'searchNullable', true, 'caseSensitive', false, 'length', 10, 'emptyRow', true),
                        jsonb_build_object('id', '5728', 'name', 'language', 'label', 'Language', 'type', 'String', 'uiType', 'ComboBox', 'lookupDataKey', 'LNG',
                            'defaultVisibleState', true, 'defaultEditableState', false,
                            'nullable', false, 'searchNullable', true, 'caseSensitive', false, 'length', 10, 'emptyRow', true),
                        jsonb_build_object('id', '5729', 'name', 'reportName', 'label', 'Report Name', 'type', 'String',
                            'defaultVisibleState', true, 'defaultEditableState', false,
                            'nullable', false, 'searchNullable', true, 'caseSensitive', false, 'maxlength', 10, 'length', 10, 'emptyRow', true),
                        jsonb_build_object('id', '5730', 'name', 'textSequence', 'label', 'Report Text Sequence', 'type', 'Integer',
                            'defaultVisibleState', true, 'defaultEditableState', false,
                            'nullable', false, 'searchNullable', true, 'caseSensitive', false, 'maxlength', 4, 'length', 4, 'emptyRow', true),
                        jsonb_build_object('id', '5731', 'name', 'reportText', 'label', 'Report Text', 'type', 'String',
                            'defaultVisibleState', true, 'defaultEditableState', true,
                            'nullable', false, 'searchNullable', true, 'caseSensitive', true, 'maxlength', 132, 'length', 45, 'emptyRow', true)
                    ),
                    'toolbarMetadata', jsonb_build_object(
                        'kind', 'toolbar',
                        'defaultVisibleState', true,
                        'defaultEditableState', true,
                        'large', false,
                        'actionButtons', jsonb_build_array(
                            jsonb_build_object('id', '5717', 'name', 'cmd_create', 'command', 'cmd_create', 'value', 'Add',    'accessKey', 'A', 'local', true,  'validate', true, 'modelType', 'none',     'clearCacheType', 'LOOKUP'),
                            jsonb_build_object('id', '5718', 'name', 'cmd_copy',   'command', 'cmd_copy',   'value', 'Copy',   'accessKey', 'C', 'local', true,  'validate', true, 'modelType', 'selected','clearCacheType', 'LOOKUP'),
                            jsonb_build_object('id', '5719', 'name', 'cmd_delete', 'command', 'cmd_delete', 'value', 'Delete', 'accessKey', 'R', 'local', false, 'validate', true, 'modelType', 'selected','clearCacheType', 'LOOKUP'),
                            jsonb_build_object('id', '5720', 'name', 'cmd_update', 'command', 'cmd_update', 'value', 'Update', 'accessKey', 'U', 'local', false, 'validate', true, 'modelType', 'selected','clearCacheType', 'LOOKUP'),
                            jsonb_build_object('id', '5721', 'name', 'cmd_close',  'command', 'cmd_close',  'value', 'Close',  'accessKey', 'X', 'local', true,  'validate', true, 'modelType', 'selected')
                        ),
                        'navigationButtons', jsonb_build_array(
                            jsonb_build_object('id', '5723', 'name', 'cmd_search', 'command', 'cmd_search', 'value', 'Search', 'accessKey', 'S', 'local', true, 'validate', true, 'modelType', 'none'),
                            jsonb_build_object('id', '5724', 'name', 'cmd_print',  'command', 'cmd_print',  'value', 'Print',  'accessKey', 'P', 'local', true, 'validate', true, 'modelType', 'selected')
                        )
                    )
                )
            )
        )
    ),
    'reportText.maintenance — Phase E v2: ported from live csnx-ui /api/metadata capture (see demo/rptm-real.json).',
    NOW(), NOW(), 'system'
);

-- LNG VVD (Language code, 3-letter key matching CSnx convention). Keep
-- the older 'Language' VVD rows alongside in case anything else still
-- references them.
INSERT INTO demoschema.lookup_value (lookup_key, code, label, sort_order, created_at, updated_at, created_by)
VALUES
    ('LNG', 'EN', 'English', 0, NOW(), NOW(), 'system'),
    ('LNG', 'FR', 'French',  1, NOW(), NOW(), 'system'),
    ('LNG', 'DE', 'German',  2, NOW(), NOW(), 'system');
