-- V4: seed screen_metadata + a menu entry for the productMaintenance workflow.
--
-- The JSON payload follows the MetadataHolder shape the React engine expects
-- (see csnx-ui/frontend/src/engine/types.ts). A grid renders the rows, its
-- searchMetadata supplies the create/update popup, and the toolbar's
-- actionButtons drive the four CRUD commands. Field metadata is intentionally
-- minimal — the engine fills in sensible defaults for everything we don't
-- specify.

INSERT INTO demoschema.screen_metadata (
    workflow, fastpath, payload, description,
    created_at, updated_at, created_by
) VALUES (
    'productMaintenance',
    'PRDT',
    jsonb_build_object(
        'name', 'productMaintenance',
        'shortName', 'PRDT',
        'version', 1,
        'metadataList', jsonb_build_array(
            -- Popup form metadata (referenced by the grid's searchMetadata).
            -- Used by EntityDialog for Create / Update.
            jsonb_build_object(
                'kind', 'form',
                'name', 'productForm',
                'columns', 1,
                'fieldList', jsonb_build_array(
                    jsonb_build_object(
                        'id', 'sku', 'name', 'sku',
                        'label', 'SKU', 'type', 'STRING',
                        'maxlength', 64, 'nullable', false, 'focus', true
                    ),
                    jsonb_build_object(
                        'id', 'name', 'name', 'name',
                        'label', 'Name', 'type', 'STRING',
                        'maxlength', 120, 'nullable', false
                    ),
                    jsonb_build_object(
                        'id', 'priceMinor', 'name', 'priceMinor',
                        'label', 'Price (cents)', 'type', 'LONG',
                        'nullable', false
                    ),
                    jsonb_build_object(
                        'id', 'active', 'name', 'active',
                        'label', 'Active', 'type', 'BOOLEAN',
                        'defaultValue', true
                    )
                )
            ),
            -- Grid metadata — the main UI.
            jsonb_build_object(
                'kind', 'grid',
                'name', 'productGrid',
                'title', 'Products',
                'checkboxSelection', true,
                'inlineEdit', false,
                'rows', 25,
                -- Engine reads this to find the popup form for Add/Edit.
                'searchMetadata', jsonb_build_object(
                    'kind', 'search',
                    'name', 'productForm'
                ),
                'fieldList', jsonb_build_array(
                    jsonb_build_object(
                        'id', 'sku', 'name', 'sku',
                        'label', 'SKU', 'type', 'STRING'
                    ),
                    jsonb_build_object(
                        'id', 'name', 'name', 'name',
                        'label', 'Name', 'type', 'STRING'
                    ),
                    jsonb_build_object(
                        'id', 'priceMinor', 'name', 'priceMinor',
                        'label', 'Price (cents)', 'type', 'LONG',
                        'align', 'right'
                    ),
                    jsonb_build_object(
                        'id', 'active', 'name', 'active',
                        'label', 'Active', 'type', 'BOOLEAN'
                    )
                ),
                'toolbarMetadata', jsonb_build_object(
                    'kind', 'toolbar',
                    'name', 'productToolbar',
                    'actionButtons', jsonb_build_array(
                        jsonb_build_object(
                            'id', 'cmd_search', 'name', 'cmd_search',
                            'value', 'Search', 'command', 'cmd_search',
                            'modelType', 'none'
                        ),
                        jsonb_build_object(
                            'id', 'cmd_create', 'name', 'cmd_create',
                            'value', 'Add', 'command', 'cmd_create',
                            'modelType', 'forms', 'validate', true
                        ),
                        jsonb_build_object(
                            'id', 'cmd_update', 'name', 'cmd_update',
                            'value', 'Edit', 'command', 'cmd_update',
                            'modelType', 'form_single', 'validate', true
                        ),
                        jsonb_build_object(
                            'id', 'cmd_delete', 'name', 'cmd_delete',
                            'value', 'Delete', 'command', 'cmd_delete',
                            'modelType', 'selected'
                        )
                    )
                )
            )
        )
    ),
    'Engine-driven CRUD over the Product entity. Phase C smoke target.',
    NOW(), NOW(), 'system'
);

-- Surface the screen in the menu tree so the engine can resolve PRDT
-- via the lookup bundle without relying on the fastpaths map.
INSERT INTO demoschema.menu_item (
    code, label, fastpath, workflow, sort_order,
    created_at, updated_at, created_by
) VALUES (
    'product-maintenance', 'Product Maintenance', 'PRDT', 'productMaintenance', 10,
    NOW(), NOW(), 'system'
);
