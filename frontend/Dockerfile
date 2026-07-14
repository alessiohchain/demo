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
# No env build args: the IdP issuer + central portal URL are runtime config —
# the entrypoint renders /usr/share/nginx/html/config.js from the
# PLATFORM_ISSUER / PORTAL_URL env vars at startup (see docker-entrypoint.sh +
# public/config.js), so the same image serves compose, Cloud Run, and
# Container Apps without a rebuild.
RUN npm run build

FROM nginx:1.27-alpine
RUN apk add --no-cache gettext
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY docker-entrypoint.sh /usr/local/bin/demo-entrypoint.sh
# Strip any CR before chmod: a Windows (CRLF) checkout turns the script's
# `#!/bin/sh` shebang into `#!/bin/sh\r`, so Alpine fails to exec it with
# "no such file or directory". sed makes the image build correctly regardless
# of the host's git autocrlf setting.
RUN sed -i 's/\r$//' /usr/local/bin/demo-entrypoint.sh \
    && chmod +x /usr/local/bin/demo-entrypoint.sh
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost/ >/dev/null || exit 1
ENTRYPOINT ["/usr/local/bin/demo-entrypoint.sh"]
