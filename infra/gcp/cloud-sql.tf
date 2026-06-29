resource "random_password" "db_app_password" {
  length  = 32
  special = false
}

resource "google_sql_database_instance" "postgres" {
  name             = "${local.resource_prefix}-pg"
  database_version = "POSTGRES_16"
  region           = var.region

  deletion_protection = false # demo workload

  depends_on = [google_service_networking_connection.private_vpc_connection]

  settings {
    edition           = "ENTERPRISE" # required to allow shared-core tiers like db-f1-micro
    tier              = var.db_tier
    disk_size         = var.db_disk_size_gb
    disk_type         = "PD_SSD"
    availability_type = "ZONAL"

    ip_configuration {
      ipv4_enabled                                  = true
      private_network                               = google_compute_network.vpc.id
      enable_private_path_for_google_cloud_services = true
      ssl_mode                                      = "ENCRYPTED_ONLY"

      # WARNING: opens the shared demo+pom database to the public internet.
      # Only the DB password protects it (SSL is still required). Requested
      # explicitly for laptop JDBC access; tighten or remove when no longer needed.
      authorized_networks {
        name  = "public-internet"
        value = "0.0.0.0/0"
      }
    }

    backup_configuration {
      enabled                        = true
      point_in_time_recovery_enabled = true
      start_time                     = "02:00"
    }

    insights_config {
      query_insights_enabled = true
    }
  }
}

resource "google_sql_database" "demo" {
  name     = "demo"
  instance = google_sql_database_instance.postgres.name
}

resource "google_sql_user" "app_user" {
  name     = "demo_app"
  instance = google_sql_database_instance.postgres.name
  password = random_password.db_app_password.result
}

# POM application user. POM shares the `demo` database but lives in its own
# `pomschema` schema, which this user owns (full access). The schema itself
# is created by POM's Flyway migrations on first boot — the google provider
# manages users/databases, not schemas.
#
# This user was first created out-of-band (psql). Before the next apply,
# import it so Terraform adopts it instead of trying to recreate it:
#   terraform import google_sql_user.pom_app ai-development-459111/demo-pg/pom_app
# After apply, the live password becomes this Terraform-managed value
# (stored in the pom-db-password secret); the manual password is superseded.
resource "random_password" "db_pom_password" {
  length  = 32
  special = false
}

resource "google_sql_user" "pom_app" {
  name     = "pom_app"
  instance = google_sql_database_instance.postgres.name
  password = random_password.db_pom_password.result
}
