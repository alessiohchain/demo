resource "google_service_account" "backend" {
  account_id   = "${local.resource_prefix}-backend"
  display_name = "Backend Cloud Run runtime SA"
}

resource "google_service_account" "frontend" {
  account_id   = "${local.resource_prefix}-frontend"
  display_name = "Frontend Cloud Run runtime SA"
}

# Backend SA: read images, talk to Cloud SQL, read its two secrets.
resource "google_project_iam_member" "backend_sql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.backend.email}"
}

resource "google_artifact_registry_repository_iam_member" "backend_pull" {
  project    = var.project_id
  location   = var.region
  repository = google_artifact_registry_repository.docker.repository_id
  role       = "roles/artifactregistry.reader"
  member     = "serviceAccount:${google_service_account.backend.email}"
}

resource "google_secret_manager_secret_iam_member" "backend_db_password" {
  secret_id = google_secret_manager_secret.db_password.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.backend.email}"
}

resource "google_secret_manager_secret_iam_member" "backend_jwt_secret" {
  secret_id = google_secret_manager_secret.jwt_secret.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.backend.email}"
}

# Frontend SA: read images, invoke the backend (scoped per-service in cloud-run-backend.tf).
resource "google_artifact_registry_repository_iam_member" "frontend_pull" {
  project    = var.project_id
  location   = var.region
  repository = google_artifact_registry_repository.docker.repository_id
  role       = "roles/artifactregistry.reader"
  member     = "serviceAccount:${google_service_account.frontend.email}"
}
