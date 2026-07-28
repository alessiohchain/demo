# Demo's Postgres with the wal2json logical-decoding output plugin — the engine's
# WAL CDC (csnx.engine.cdc, WalCdcPoller) reads table changes from a logical
# replication slot in wal2json format so demo can be a CDC producer of its own
# tables (e.g. demo.scwt_trader). The official Debian postgres image ships the
# pgdg apt repo, so the extension is one package away. wal_level=logical is set
# via the compose `command:` flag, not baked in here.
FROM postgres:16
# Fetch apt over HTTPS. Some networks (transparent MikroTik/RouterOS web proxies
# on guest Wi-Fi, hotel/airport APs) return 403 for ALL plain-HTTP traffic, which
# breaks `apt-get update` and so this whole image. postgres:16 ships no CA bundle
# — and apt can't install one without working apt — so borrow alpine's first.
COPY --from=alpine:3.24 /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt
RUN sed -i 's|http://deb.debian.org|https://deb.debian.org|g' /etc/apt/sources.list.d/debian.sources \
 && sed -i 's|http://apt.postgresql.org|https://apt.postgresql.org|g' /etc/apt/sources.list.d/pgdg.list \
 && apt-get update \
 && apt-get install -y --no-install-recommends postgresql-16-wal2json \
 && rm -rf /var/lib/apt/lists/*
