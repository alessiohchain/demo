resource "google_compute_network" "vpc" {
  name                    = "${local.resource_prefix}-vpc"
  auto_create_subnetworks = false

  depends_on = [google_project_service.apis]
}

resource "google_compute_subnetwork" "main" {
  name                     = "${local.resource_prefix}-subnet"
  ip_cidr_range            = "10.10.0.0/24"
  region                   = var.region
  network                  = google_compute_network.vpc.id
  private_ip_google_access = true
}

# Private services access (peering range) — required so Cloud SQL can have a
# private IP reachable from this VPC.
resource "google_compute_global_address" "private_services" {
  name          = "${local.resource_prefix}-psa-range"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 16
  network       = google_compute_network.vpc.id
}

resource "google_service_networking_connection" "private_vpc_connection" {
  network                 = google_compute_network.vpc.id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_services.name]
}

# Serverless VPC Connector — the bridge that lets Cloud Run egress into this VPC
# so it can reach Cloud SQL on its private IP.
resource "google_vpc_access_connector" "connector" {
  name          = "${local.resource_prefix}-vpc-conn"
  region        = var.region
  ip_cidr_range = "10.20.0.0/28"
  network       = google_compute_network.vpc.name
  min_instances = 2
  max_instances = 3
  machine_type  = "e2-micro"

  depends_on = [google_project_service.apis]
}
