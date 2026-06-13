#!/bin/sh
set -eu

# Multi-cloud entrypoint for the demo frontend nginx container.
#
# Local docker-compose (defaults): proxies /api → http://backend:8080.
# Azure Container Apps:            BACKEND_URL=<backend internal FQDN>.
# Google Cloud Run:                BACKEND_URL=<backend run.app URL> and
#                                  BACKEND_AUDIENCE=<same URL>. Adds an
#                                  X-Serverless-Authorization ID-token header
#                                  refreshed every 30 minutes.

BACKEND_URL="${BACKEND_URL:-http://backend:8080}"
BACKEND_HOST=$(printf '%s' "$BACKEND_URL" | sed -E 's|^https?://([^/]+).*|\1|')
BACKEND_AUDIENCE="${BACKEND_AUDIENCE:-}"

TEMPLATE=/etc/nginx/templates/default.conf.template
RENDERED=/etc/nginx/conf.d/default.conf

render_config() {
    id_token="${1:-}"
    if [ -n "$id_token" ]; then
        EXTRA_AUTH_HEADER="proxy_set_header X-Serverless-Authorization \"Bearer ${id_token}\";"
    else
        EXTRA_AUTH_HEADER=""
    fi
    export BACKEND_URL BACKEND_HOST EXTRA_AUTH_HEADER
    envsubst '${BACKEND_URL} ${BACKEND_HOST} ${EXTRA_AUTH_HEADER}' < "$TEMPLATE" > "$RENDERED"
}

fetch_id_token() {
    wget -qO- \
        --header='Metadata-Flavor: Google' \
        "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/identity?audience=${BACKEND_AUDIENCE}" \
        || true
}

echo "demo-entrypoint: BACKEND_URL=${BACKEND_URL}" >&2
echo "demo-entrypoint: BACKEND_HOST=${BACKEND_HOST}" >&2
echo "demo-entrypoint: BACKEND_AUDIENCE=${BACKEND_AUDIENCE}" >&2

if [ -n "$BACKEND_AUDIENCE" ]; then
    TOKEN=$(fetch_id_token)
    if [ -z "$TOKEN" ]; then
        echo "demo-entrypoint: WARN failed to obtain ID token for audience=${BACKEND_AUDIENCE}" >&2
    else
        echo "demo-entrypoint: obtained ID token, length=${#TOKEN}" >&2
    fi
    render_config "$TOKEN"

    (
        while true; do
            sleep 1800
            NEW_TOKEN=$(fetch_id_token)
            if [ -n "$NEW_TOKEN" ]; then
                render_config "$NEW_TOKEN"
                nginx -s reload 2>/dev/null || true
            fi
        done
    ) &
else
    render_config ""
fi

echo "demo-entrypoint: rendered nginx config:" >&2
sed -E 's/(Bearer )[^"]+/\1<REDACTED>/' "$RENDERED" >&2

exec nginx -g 'daemon off;'
