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
