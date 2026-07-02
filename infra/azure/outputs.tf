output "frontend_url" {
  description = "Public HTTPS URL of the demo frontend."
  value       = local.frontend_url
}

output "backend_internal_fqdn" {
  description = "Internal-ingress FQDN of the demo backend (reachable only inside the Container Apps environment)."
  value       = azurerm_container_app.backend.ingress[0].fqdn
}

output "platform_issuer" {
  description = "OIDC issuer of the central platform IdP the backend validates tokens against."
  value       = local.platform_issuer
}

output "backend_image" {
  description = "Full image path the backend Container App expects."
  value       = "${data.azurerm_container_registry.shared.login_server}/${local.resource_prefix}-backend:${var.backend_image_tag}"
}

output "frontend_image" {
  description = "Full image path the frontend Container App expects."
  value       = "${data.azurerm_container_registry.shared.login_server}/${local.resource_prefix}-frontend:${var.frontend_image_tag}"
}
