resource "google_artifact_registry_repository" "docker" {
  location      = var.region
  repository_id = "${local.resource_prefix}-docker"
  description   = "Container images for the ${local.resource_prefix} app."
  format        = "DOCKER"

  depends_on = [google_project_service.apis]
}
