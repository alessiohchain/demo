# Azure Terraform root for the DEMO module.
#
# The PLATFORM repo's infra/azure owns the shared fleet infrastructure
# (resource group, VNet, Container Apps environment, ACR, PostgreSQL Flexible
# Server, Key Vault, Log Analytics / Application Insights) plus the per-module
# groundwork demo relies on: the `demo` database + `demo_app` role (created by
# the csnx-db-init job) and the `demo-db-password` Key Vault secret. This root
# only creates DEMO-scoped resources — the two Container Apps, their
# user-assigned identities, and the role assignments they need — and reaches
# the shared resources via data sources (names come from platform's
# `terraform output`, see terraform.tfvars.example).

terraform {
  required_version = ">= 1.6.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.20"
    }
  }

  # Remote state (Phase 5 / CI): once the state storage account exists, move
  # state there with `terraform init -migrate-state`. Until then state is
  # local, matching infra/gcp.
  #
  # backend "azurerm" {
  #   resource_group_name  = "csnx-tfstate"
  #   storage_account_name = "csnxtfstate"
  #   container_name       = "tfstate"
  #   key                  = "demo.tfstate"
  # }
}

provider "azurerm" {
  features {}
  subscription_id = var.subscription_id
}

locals {
  resource_prefix = var.app_name # "demo"

  # External apps get https://<app>.<environment default domain>; INTERNAL
  # apps (the demo backend) get https://<app>.internal.<default domain>,
  # reachable only inside the VNet-integrated environment.
  env_domain = data.azurerm_container_app_environment.shared.default_domain

  frontend_fqdn = "${local.resource_prefix}-frontend.${local.env_domain}"
  frontend_url  = "https://${local.frontend_fqdn}"

  # The central platform IdP (external ingress on the same environment). Must
  # match platform's infra/azure local.backend_url — override via
  # var.platform_issuer once a custom domain is bound to platform-backend.
  platform_issuer = var.platform_issuer != "" ? var.platform_issuer : "https://platform-backend.${local.env_domain}"
}
