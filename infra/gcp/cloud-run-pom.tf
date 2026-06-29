# POM application — its own backend + frontend Cloud Run services, deployed
# alongside the demo app. Reuses the shared infra (VPC, serverless connector,
# Artifact Registry repo, and the Cloud SQL instance). POM connects as the
# pom_app user into the `demo` database, scoped to the pomschema schema it owns
# (see cloud-sql.tf). Images live in the same repo under pom-backend /
# pom-frontend tags.

# ─── Service accounts ──────────────────────────────────────────────
resource "google_service_account" "pom_backend" {
  account_id   = "pom-backend"
  display_name = "POM Backend Cloud Run runtime SA"
}

resource "google_service_account" "pom_frontend" {
  account_id   = "pom-frontend"
  display_name = "POM Frontend Cloud Run runtime SA"
}

# Backend SA: Cloud SQL client, image pull, read its two secrets.
resource "google_project_iam_member" "pom_backend_sql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.pom_backend.email}"
}

resource "google_artifact_registry_repository_iam_member" "pom_backend_pull" {
  project    = var.project_id
  location   = var.region
  repository = google_artifact_registry_repository.docker.repository_id
  role       = "roles/artifactregistry.reader"
  member     = "serviceAccount:${google_service_account.pom_backend.email}"
}

resource "google_secret_manager_secret_iam_member" "pom_backend_db_password" {
  secret_id = google_secret_manager_secret.pom_db_password.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.pom_backend.email}"
}

resource "google_secret_manager_secret_iam_member" "pom_backend_jwt_secret" {
  secret_id = google_secret_manager_secret.pom_jwt_secret.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.pom_backend.email}"
}

# Frontend SA: image pull, invoke the POM backend (scoped per-service below).
resource "google_artifact_registry_repository_iam_member" "pom_frontend_pull" {
  project    = var.project_id
  location   = var.region
  repository = google_artifact_registry_repository.docker.repository_id
  role       = "roles/artifactregistry.reader"
  member     = "serviceAccount:${google_service_account.pom_frontend.email}"
}

# ─── Backend service ───────────────────────────────────────────────
resource "google_cloud_run_v2_service" "pom_backend" {
  name     = "pom-backend"
  location = var.region

  # Public URL, but no public invokers — only the POM frontend SA (below).
  ingress = "INGRESS_TRAFFIC_ALL"

  template {
    service_account = google_service_account.pom_backend.email

    scaling {
      min_instance_count = 1
      max_instance_count = 3
    }

    vpc_access {
      connector = google_vpc_access_connector.connector.id
      egress    = "PRIVATE_RANGES_ONLY"
    }

    containers {
      image = "${local.image_repo}/pom-backend:${var.pom_backend_image_tag}"

      ports {
        container_port = 8080
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "1Gi"
        }
        cpu_idle          = false
        startup_cpu_boost = true
      }

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "prod"
      }
      # Shared `demo` database, session-scoped to the pomschema schema so
      # Flyway history + unqualified objects land there (pom_app owns it).
      env {
        name  = "DB_URL"
        value = "jdbc:postgresql://${google_sql_database_instance.postgres.private_ip_address}:5432/demo?sslmode=require&currentSchema=pomschema"
      }
      env {
        name  = "DB_USER"
        value = google_sql_user.pom_app.name
      }
      env {
        name = "DB_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.pom_db_password.secret_id
            version = "latest"
          }
        }
      }
      env {
        name = "APP_SECURITY_JWT_SECRET"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.pom_jwt_secret.secret_id
            version = "latest"
          }
        }
      }

      startup_probe {
        http_get {
          path = "/actuator/health"
        }
        initial_delay_seconds = 30
        timeout_seconds       = 5
        period_seconds        = 10
        failure_threshold     = 6
      }

      liveness_probe {
        http_get {
          path = "/actuator/health"
        }
        initial_delay_seconds = 60
        timeout_seconds       = 5
        period_seconds        = 30
        failure_threshold     = 3
      }
    }
  }

  deletion_protection = false

  depends_on = [
    google_secret_manager_secret_version.pom_db_password,
    google_secret_manager_secret_version.pom_jwt_secret,
    google_secret_manager_secret_iam_member.pom_backend_db_password,
    google_secret_manager_secret_iam_member.pom_backend_jwt_secret,
    google_sql_user.pom_app,
  ]
}

# Only the POM frontend SA can invoke the POM backend — public requests get 403.
resource "google_cloud_run_v2_service_iam_member" "pom_backend_invoker" {
  project  = google_cloud_run_v2_service.pom_backend.project
  location = google_cloud_run_v2_service.pom_backend.location
  name     = google_cloud_run_v2_service.pom_backend.name
  role     = "roles/run.invoker"
  member   = "serviceAccount:${google_service_account.pom_frontend.email}"
}

# ─── Frontend service ──────────────────────────────────────────────
resource "google_cloud_run_v2_service" "pom_frontend" {
  name     = "pom-frontend"
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL"

  template {
    service_account = google_service_account.pom_frontend.email

    scaling {
      min_instance_count = 1
      max_instance_count = 3
    }

    containers {
      image = "${local.image_repo}/pom-frontend:${var.pom_frontend_image_tag}"

      ports {
        container_port = 80
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
        cpu_idle = true
      }

      env {
        name  = "BACKEND_URL"
        value = google_cloud_run_v2_service.pom_backend.uri
      }
      env {
        name  = "BACKEND_AUDIENCE"
        value = google_cloud_run_v2_service.pom_backend.uri
      }

      startup_probe {
        http_get {
          path = "/"
        }
        initial_delay_seconds = 5
        timeout_seconds       = 3
        period_seconds        = 5
        failure_threshold     = 6
      }
    }
  }

  deletion_protection = false
}

# Public site — allow unauthenticated invocations.
resource "google_cloud_run_v2_service_iam_member" "pom_frontend_public" {
  project  = google_cloud_run_v2_service.pom_frontend.project
  location = google_cloud_run_v2_service.pom_frontend.location
  name     = google_cloud_run_v2_service.pom_frontend.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}
