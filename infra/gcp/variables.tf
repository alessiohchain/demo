variable "project_id" {
  type        = string
  description = "GCP project ID (billing must be enabled)."
}

variable "region" {
  type        = string
  description = "GCP region for all regional resources."
  default     = "africa-south1"
}

variable "app_name" {
  type        = string
  description = "Short name used as prefix/suffix for resources."
  default     = "demo"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{1,18}[a-z0-9]$", var.app_name))
    error_message = "app_name must be 3-20 chars, lowercase letters/digits/hyphens, start with a letter."
  }
}

variable "backend_image_tag" {
  type        = string
  description = "Image tag for the backend container in Artifact Registry."
  default     = "latest"
}

variable "frontend_image_tag" {
  type        = string
  description = "Image tag for the frontend container in Artifact Registry."
  default     = "latest"
}

variable "pom_backend_image_tag" {
  type        = string
  description = "Image tag for the POM backend container in Artifact Registry."
  default     = "latest"
}

variable "pom_frontend_image_tag" {
  type        = string
  description = "Image tag for the POM frontend container in Artifact Registry."
  default     = "latest"
}

variable "db_tier" {
  type        = string
  description = "Cloud SQL machine tier. Resized from db-f1-micro to a dedicated-core tier once the shared instance hosted demo + pom + platform (the f1-micro ~25-connection ceiling was exhausted)."
  default     = "db-custom-2-7680"
}

variable "db_disk_size_gb" {
  type        = number
  description = "Cloud SQL data disk size in GiB."
  default     = 10
}
