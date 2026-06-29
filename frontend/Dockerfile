# syntax=docker/dockerfile:1.7

FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json .npmrc ./
# The engine is pulled from GitHub Packages (no vendored tgz). The token is a
# BuildKit secret so it never lands in an image layer; .npmrc reads it from
# $GH_PACKAGES_TOKEN. Pass it via `docker compose` (compose secrets) or
# `docker build --secret id=gh_token,env=GH_PACKAGES_TOKEN`.
RUN --mount=type=secret,id=gh_token \
    GH_PACKAGES_TOKEN="$(cat /run/secrets/gh_token)" npm ci
COPY . .
# The IdP issuer the frontend redirects to is baked into the bundle at build
# time (Vite inlines import.meta.env.VITE_PLATFORM_ISSUER). Local/compose builds
# keep the localhost default; the GCP deploy passes the platform-backend public
# URL via --build-arg.
ARG VITE_PLATFORM_ISSUER=http://localhost:8090
ENV VITE_PLATFORM_ISSUER=$VITE_PLATFORM_ISSUER
# The central portal URL the OIDC RP-initiated logout redirects back to (must be
# a registered post-logout-redirect-uri on this module's IdP client). Defaults to
# the local portal; the GCP deploy passes the platform-frontend public URL.
ARG VITE_PORTAL_URL=http://localhost:8091
ENV VITE_PORTAL_URL=$VITE_PORTAL_URL
RUN npm run build

FROM nginx:1.27-alpine
RUN apk add --no-cache gettext
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY docker-entrypoint.sh /usr/local/bin/demo-entrypoint.sh
RUN chmod +x /usr/local/bin/demo-entrypoint.sh
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost/ >/dev/null || exit 1
ENTRYPOINT ["/usr/local/bin/demo-entrypoint.sh"]
