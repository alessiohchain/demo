# Shared fleet resources owned by platform's infra/azure — referenced here by
# name only. Nothing in this file is created or mutated by the demo root.

data "azurerm_resource_group" "shared" {
  name = var.shared_resource_group
}

data "azurerm_container_app_environment" "shared" {
  name                = var.container_app_environment_name
  resource_group_name = data.azurerm_resource_group.shared.name
}

data "azurerm_container_registry" "shared" {
  name                = var.acr_name
  resource_group_name = data.azurerm_resource_group.shared.name
}

data "azurerm_key_vault" "shared" {
  name                = var.key_vault_name
  resource_group_name = data.azurerm_resource_group.shared.name
}

data "azurerm_postgresql_flexible_server" "shared" {
  name                = var.postgres_server_name
  resource_group_name = data.azurerm_resource_group.shared.name
}

data "azurerm_application_insights" "shared" {
  name                = var.application_insights_name
  resource_group_name = data.azurerm_resource_group.shared.name
}
