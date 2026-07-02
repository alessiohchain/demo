resource "google_cloud_run_v2_service" "frontend" {
  name     = "${local.resource_prefix}-frontend"
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL"

  template {
    service_account = google_service_account.frontend.email

    scaling {
      min_instance_count = 1
      max_instance_count = 3
    }

    containers {
      image = "${local.image_repo}/${local.resource_prefix}-frontend:${var.frontend_image_tag}"

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
        value = google_cloud_run_v2_service.backend.uri
      }
      env {
        name  = "BACKEND_AUDIENCE"
        value = google_cloud_run_v2_service.backend.uri
      }
      # Browser-facing platform URLs (IdP issuer + central portal). The
      # entrypoint renders them into the SPA's config.js at startup — runtime
      # config, not baked into the bundle. Project-number Cloud Run URLs,
      # matching the backend's PLATFORM_ISSUER.
      env {
        name  = "PLATFORM_ISSUER"
        value = "https://platform-backend-236510297424.africa-south1.run.app"
      }
      env {
        name  = "PORTAL_URL"
        value = "https://platform-frontend-236510297424.africa-south1.run.app"
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
resource "google_cloud_run_v2_service_iam_member" "frontend_public" {
  project  = google_cloud_run_v2_service.frontend.project
  location = google_cloud_run_v2_service.frontend.location
  name     = google_cloud_run_v2_service.frontend.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}
