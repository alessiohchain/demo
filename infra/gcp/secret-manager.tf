resource "google_secret_manager_secret" "db_password" {
  secret_id = "${local.resource_prefix}-db-password"

  replication {
    auto {}
  }

  depends_on = [google_project_service.apis]
}

resource "google_secret_manager_secret_version" "db_password" {
  secret      = google_secret_manager_secret.db_password.id
  secret_data = random_password.db_app_password.result
}

resource "google_secret_manager_secret" "pom_db_password" {
  secret_id = "pom-db-password"

  replication {
    auto {}
  }

  depends_on = [google_project_service.apis]
}

resource "google_secret_manager_secret_version" "pom_db_password" {
  secret      = google_secret_manager_secret.pom_db_password.id
  secret_data = random_password.db_pom_password.result
}

# Dedicated JWT secret for the POM backend (independent of the demo app so
# tokens aren't cross-valid between the two services).
resource "random_password" "pom_jwt_secret" {
  length  = 48
  special = false
}

resource "google_secret_manager_secret" "pom_jwt_secret" {
  secret_id = "pom-jwt-secret"

  replication {
    auto {}
  }

  depends_on = [google_project_service.apis]
}

resource "google_secret_manager_secret_version" "pom_jwt_secret" {
  secret      = google_secret_manager_secret.pom_jwt_secret.id
  secret_data = base64encode(random_password.pom_jwt_secret.result)
}

resource "random_password" "jwt_secret" {
  # Spring's JwtService expects >= 32 bytes after base64 decode. 48 raw bytes
  # base64-encoded comfortably exceeds that.
  length  = 48
  special = false
}

resource "google_secret_manager_secret" "jwt_secret" {
  secret_id = "${local.resource_prefix}-jwt-secret"

  replication {
    auto {}
  }

  depends_on = [google_project_service.apis]
}

resource "google_secret_manager_secret_version" "jwt_secret" {
  secret      = google_secret_manager_secret.jwt_secret.id
  secret_data = base64encode(random_password.jwt_secret.result)
}
