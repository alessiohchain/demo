# User-assigned managed identities — the Azure analog of the per-service GCP
# runtime service accounts. Least privilege:
#   backend : pull images, read its single Key Vault secret
#   frontend: pull images only
resource "azurerm_user_assigned_identity" "backend" {
  name                = "${local.resource_prefix}-backend"
  location            = data.azurerm_resource_group.shared.location
  resource_group_name = data.azurerm_resource_group.shared.name
}

resource "azurerm_user_assigned_identity" "frontend" {
  name                = "${local.resource_prefix}-frontend"
  location            = data.azurerm_resource_group.shared.location
  resource_group_name = data.azurerm_resource_group.shared.name
}

resource "azurerm_role_assignment" "backend_acr_pull" {
  scope                = data.azurerm_container_registry.shared.id
  role_definition_name = "AcrPull"
  principal_id         = azurerm_user_assigned_identity.backend.principal_id
}

resource "azurerm_role_assignment" "frontend_acr_pull" {
  scope                = data.azurerm_container_registry.shared.id
  role_definition_name = "AcrPull"
  principal_id         = azurerm_user_assigned_identity.frontend.principal_id
}

# Key Vault is RBAC-mode; "Key Vault Secrets User" = get/list secret values.
# Scoped to the ONE secret demo reads (platform's TF creates it), not the
# whole vault — the vault also holds other modules' credentials.
resource "azurerm_role_assignment" "backend_kv_db_password" {
  scope                = "${data.azurerm_key_vault.shared.id}/secrets/${local.resource_prefix}-db-password"
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_user_assigned_identity.backend.principal_id
}
