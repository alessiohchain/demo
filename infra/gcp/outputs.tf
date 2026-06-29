output "frontend_url" {
  description = "Public HTTPS URL of the frontend Cloud Run service."
  value       = google_cloud_run_v2_service.frontend.uri
}

output "backend_url" {
  description = "Cloud Run URL of the backend (IAM-protected; only the frontend SA can invoke it)."
  value       = google_cloud_run_v2_service.backend.uri
}

output "artifact_registry_repo" {
  description = "Docker repo path. Use as the prefix when tagging images for push."
  value       = local.image_repo
}

output "backend_image" {
  description = "Full image path Cloud Run expects for the backend."
  value       = "${local.image_repo}/${local.resource_prefix}-backend:${var.backend_image_tag}"
}

output "frontend_image" {
  description = "Full image path Cloud Run expects for the frontend."
  value       = "${local.image_repo}/${local.resource_prefix}-frontend:${var.frontend_image_tag}"
}

output "cloud_sql_instance" {
  description = "Cloud SQL instance connection name (project:region:instance)."
  value       = google_sql_database_instance.postgres.connection_name
}

output "cloud_sql_private_ip" {
  description = "Private IP of the Cloud SQL instance (reachable only inside the VPC)."
  value       = google_sql_database_instance.postgres.private_ip_address
}

output "pom_db_secret" {
  description = "Secret Manager secret holding the pom_app DB password."
  value       = google_secret_manager_secret.pom_db_password.secret_id
}

output "pom_frontend_url" {
  description = "Public HTTPS URL of the POM frontend Cloud Run service."
  value       = google_cloud_run_v2_service.pom_frontend.uri
}

output "pom_backend_url" {
  description = "Cloud Run URL of the POM backend (IAM-protected; only the POM frontend SA can invoke it)."
  value       = google_cloud_run_v2_service.pom_backend.uri
}

output "pom_backend_image" {
  description = "Full image path Cloud Run expects for the POM backend."
  value       = "${local.image_repo}/pom-backend:${var.pom_backend_image_tag}"
}

output "pom_frontend_image" {
  description = "Full image path Cloud Run expects for the POM frontend."
  value       = "${local.image_repo}/pom-frontend:${var.pom_frontend_image_tag}"
}
