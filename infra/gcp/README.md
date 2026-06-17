# GCP deploy — quick start

Twin of the (still-pending) Azure deploy. Drops the `demo` app onto:

- **Cloud Run** × 2 (frontend public, backend IAM-only invokable by frontend SA)
- **Cloud SQL Postgres 16** on a private IP (no public IP)
- **Artifact Registry** for the two container images
- **Secret Manager** for DB password + JWT secret

See `../../.gcp/deployment-plan.md` for the full architecture rationale.

## Prerequisites

```powershell
winget install Google.CloudSDK
winget install HashiCorp.Terraform
gcloud auth login
gcloud auth application-default login
gcloud config set project <your-gcp-project-id>
```

## One-time setup

```powershell
cd infra\gcp
Copy-Item terraform.tfvars.example terraform.tfvars
notepad terraform.tfvars   # set project_id (and region if not africa-south1)

terraform init
```

## Deploy

```powershell
# 1. Provision infrastructure (first run takes ~10 min — Cloud SQL is slow to create)
terraform apply

# 2. Build + push images to the freshly-created Artifact Registry
$REGION = "africa-south1"
$REPO   = terraform output -raw artifact_registry_repo
gcloud auth configure-docker "$REGION-docker.pkg.dev"

docker build -t "$REPO/demo-backend:latest"  ..\..\backend
docker build -t "$REPO/demo-frontend:latest" ..\..\frontend
docker push "$REPO/demo-backend:latest"
docker push "$REPO/demo-frontend:latest"

# 3. Roll Cloud Run to the freshly-pushed image (Terraform created services
#    pointing at :latest — but Cloud Run doesn't auto-pull on tag updates;
#    you must trigger a new revision). Either:
gcloud run services update demo-backend  --region $REGION --image (terraform output -raw backend_image)
gcloud run services update demo-frontend --region $REGION --image (terraform output -raw frontend_image)
```

## Smoke test

```powershell
$FRONTEND = terraform output -raw frontend_url
$BACKEND  = terraform output -raw backend_url

# Should return the SPA HTML
curl $FRONTEND

# Should return 401 (proxy works → backend JWT filter rejects)
curl "$FRONTEND/api/me"

# Should return 403 (direct hit on backend is blocked by Cloud Run IAM)
curl "$BACKEND/api/me"
```

## Inspect

```powershell
# Backend logs (Flyway migration output appears here on first boot)
gcloud run services logs read demo-backend --region $REGION --limit 100

# Cloud SQL via Cloud SQL Auth Proxy (DB has no public IP)
gcloud sql connect demo-pg --user=demo_app --database=demo
```

## Tear down

```powershell
terraform destroy
```

> The Postgres instance has `deletion_protection = false` (demo workload).
> Flip it to `true` in `cloud-sql.tf` before doing anything that resembles production.

## Notes

- **First `terraform apply` enables 9 GCP APIs.** Expect ~2 min of "waiting for API" on the first run.
- **`africa-south1` is recent.** If any resource fails with an availability error, set `region = "europe-west1"` in `terraform.tfvars` and re-apply.
- **State is local.** `terraform.tfstate` contains the random DB password — `.gitignore` excludes it. For real use, move to a GCS backend.
- **Cloud Run images.** Terraform creates services pointing at `:latest`. The `gcloud run services update --image` calls in step 3 force a new revision so the freshly-pushed image is actually deployed.
