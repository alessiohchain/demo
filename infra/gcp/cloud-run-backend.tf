resource "google_cloud_run_v2_service" "backend" {
  name     = "${local.resource_prefix}-backend"
  location = var.region

  # Public URL, but no public invokers — see the IAM member below.
  ingress = "INGRESS_TRAFFIC_ALL"

  template {
    service_account = google_service_account.backend.email

    scaling {
      min_instance_count = 1
      max_instance_count = 3
    }

    vpc_access {
      connector = google_vpc_access_connector.connector.id
      egress    = "PRIVATE_RANGES_ONLY"
    }

    containers {
      image = "${local.image_repo}/${local.resource_prefix}-backend:${var.backend_image_tag}"

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
      # Demo's app defaults server.port to 8092 (local-dev convention); the
      # container must bind the Cloud Run port (8080, matching ports.container_port
      # + the health probe below).
      env {
        name  = "SERVER_PORT"
        value = "8080"
      }
      env {
        name  = "DB_URL"
        value = "jdbc:postgresql://${google_sql_database_instance.postgres.private_ip_address}:5432/demo?sslmode=require"
      }
      env {
        name  = "DB_USER"
        value = google_sql_user.app_user.name
      }
      env {
        name = "DB_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.db_password.secret_id
            version = "latest"
          }
        }
      }
      env {
        name = "APP_SECURITY_JWT_SECRET"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.jwt_secret.secret_id
            version = "latest"
          }
        }
      }
      # Central platform IdP (relying-party SSO + metadata registry). The
      # platform backend is the public OIDC issuer; DEMO validates user tokens
      # against its JWKS (PLATFORM_JWKS_URI left unset → <issuer>/oauth2/jwks)
      # and registers its metadata there. Project-number Cloud Run URL.
      env {
        name  = "PLATFORM_ISSUER"
        value = "https://platform-backend-236510297424.africa-south1.run.app"
      }
      env {
        name  = "PLATFORM_REGISTRY_URL"
        value = "https://platform-backend-236510297424.africa-south1.run.app"
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
    google_secret_manager_secret_version.db_password,
    google_secret_manager_secret_version.jwt_secret,
    google_secret_manager_secret_iam_member.backend_db_password,
    google_secret_manager_secret_iam_member.backend_jwt_secret,
    google_sql_user.app_user,
  ]
}

# Only the frontend SA can invoke the backend — public requests get 403.
resource "google_cloud_run_v2_service_iam_member" "backend_invoker" {
  project  = google_cloud_run_v2_service.backend.project
  location = google_cloud_run_v2_service.backend.location
  name     = google_cloud_run_v2_service.backend.name
  role     = "roles/run.invoker"
  member   = "serviceAccount:${google_service_account.frontend.email}"
}
