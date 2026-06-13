-- V10: re-seed reportText.maintenance metadata with the full CSnx
-- toolbar shape (Add/Copy/Update/Delete left, Filter/Close right, plus
-- maintenance audit columns visible on the grid by default).
--
-- `local: true` on dialog-opening buttons tells the engine's Toolbar to
-- dispatch the client-side handler (open EntityDialog / FilterDialog /
-- navigate-back) instead of an immediate server call. The Save button
-- inside the dialog is what actually fires cmd_create / cmd_update /
-- cmd_copy to /api/process.

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
        'version', 2,
        'metadataList', jsonb_build_array(
            -- Popup form metadata (Add / Update / Copy use the same form;
            -- copy mode pre-populates with the source row's values, the
            -- user changes the PK fields and saves).
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
            -- Grid with full CSnx column set including maintenance audit.
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
                    jsonb_build_object('id', 'reportName',      'name', 'reportName',      'label', 'Report',     'type', 'STRING',  'lookupDataKey', 'ReportProgram'),
                    jsonb_build_object('id', 'language',        'name', 'language',        'label', 'Language',   'type', 'STRING',  'lookupDataKey', 'Language'),
                    jsonb_build_object('id', 'textSequence',    'name', 'textSequence',    'label', 'Line #',     'type', 'INTEGER', 'align', 'right'),
                    jsonb_build_object('id', 'reportText',      'name', 'reportText',      'label', 'Text',       'type', 'STRING'),
                    jsonb_build_object('id', 'maintenanceUser', 'name', 'maintenanceUser', 'label', 'Maint user', 'type', 'STRING'),
                    jsonb_build_object('id', 'maintenanceDate', 'name', 'maintenanceDate', 'label', 'Maint date', 'type', 'DATE'),
                    jsonb_build_object('id', 'maintenanceTran', 'name', 'maintenanceTran', 'label', 'Last cmd',   'type', 'STRING')
                ),
                'toolbarMetadata', jsonb_build_object(
                    'kind', 'toolbar',
                    'name', 'reportTextToolbar',
                    -- LEFT group: row-level commands (Search opens criteria
                    -- dialog; Add/Copy/Update open EntityDialog; Delete
                    -- shows a confirm dialog before firing).
                    'actionButtons', jsonb_build_array(
                        jsonb_build_object('id', 'cmd_search', 'name', 'cmd_search', 'value', 'Search', 'command', 'cmd_search', 'modelType', 'none',        'local', true),
                        jsonb_build_object('id', 'cmd_create', 'name', 'cmd_create', 'value', 'Add',    'command', 'cmd_create', 'modelType', 'forms',       'validate', true, 'local', true),
                        jsonb_build_object('id', 'cmd_copy',   'name', 'cmd_copy',   'value', 'Copy',   'command', 'cmd_copy',   'modelType', 'form_single', 'validate', true, 'local', true),
                        jsonb_build_object('id', 'cmd_update', 'name', 'cmd_update', 'value', 'Update', 'command', 'cmd_update', 'modelType', 'form_single', 'validate', true, 'local', true),
                        jsonb_build_object('id', 'cmd_delete', 'name', 'cmd_delete', 'value', 'Delete', 'command', 'cmd_delete', 'modelType', 'selected')
                    ),
                    -- RIGHT group: navigation. Filter pops the advanced
                    -- filter dialog; Close pops the breadcrumb / navigates
                    -- back to landing.
                    'navigationButtons', jsonb_build_array(
                        jsonb_build_object('id', 'cmd_filter', 'name', 'cmd_filter', 'value', 'Filter', 'command', 'cmd_filter', 'modelType', 'none', 'local', true),
                        jsonb_build_object('id', 'cmd_close',  'name', 'cmd_close',  'value', 'Close',  'command', 'cmd_close',  'modelType', 'none', 'local', true)
                    )
                )
            )
        )
    ),
    'reportText.maintenance — Phase E reseed: full CSnx button set + maintenance columns visible.',
    NOW(), NOW(), 'system'
);
