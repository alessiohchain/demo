variable "subscription_id" {
  type        = string
  description = "Azure subscription ID to deploy into (azurerm 4.x requires it explicitly)."
}

variable "location" {
  type        = string
  description = "Azure region for demo-owned resources (must match the shared fleet's region)."
  default     = "southafricanorth" # Johannesburg — pairs with GCP africa-south1
}

variable "app_name" {
  type        = string
  description = "Short name used as prefix for demo-owned resources."
  default     = "demo"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{1,18}[a-z0-9]$", var.app_name))
    error_message = "app_name must be 3-20 chars, lowercase letters/digits/hyphens, start with a letter."
  }
}

# --- Shared fleet resources (created by platform's infra/azure) -------------
#
# The resource group and Container Apps environment carry fixed names; the
# rest are random-suffixed for global DNS uniqueness, so their names must be
# supplied from platform's `terraform output` (see terraform.tfvars.example).

variable "shared_resource_group" {
  type        = string
  description = "Shared fleet resource group (platform output: resource_group)."
  default     = "csnx-rg"
}

variable "container_app_environment_name" {
  type        = string
  description = "Shared Container Apps environment name (platform output: container_app_environment)."
  default     = "csnx-aca-env"
}

variable "acr_name" {
  type        = string
  description = "Shared container registry name, random-suffixed (platform output: acr_name)."
}

variable "key_vault_name" {
  type        = string
  description = "Shared Key Vault name, random-suffixed (platform output: key_vault_name). Holds the demo-db-password secret."
}

variable "postgres_server_name" {
  type        = string
  description = "Shared PostgreSQL Flexible Server name, random-suffixed (platform output: postgres_server_name)."
}

variable "application_insights_name" {
  type        = string
  description = "Shared Application Insights component name."
  default     = "csnx-appinsights"
}

# --- Demo's own knobs --------------------------------------------------------

variable "platform_issuer" {
  type        = string
  description = "OIDC issuer of the central platform IdP. Empty = derive https://platform-backend.<environment default domain>. Set this if platform-backend gets a custom-domain issuer."
  default     = ""
}

variable "backend_image_tag" {
  type        = string
  description = "Image tag for the backend container in ACR."
  default     = "latest"
}

variable "frontend_image_tag" {
  type        = string
  description = "Image tag for the frontend container in ACR."
  default     = "latest"
}
