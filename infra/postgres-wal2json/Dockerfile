# Demo's Postgres with the wal2json logical-decoding output plugin — the engine's
# WAL CDC (csnx.engine.cdc, WalCdcPoller) reads table changes from a logical
# replication slot in wal2json format so demo can be a CDC producer of its own
# tables (e.g. demo.scwt_trader). The official Debian postgres image ships the
# pgdg apt repo, so the extension is one package away. wal_level=logical is set
# via the compose `command:` flag, not baked in here.
FROM postgres:16
RUN apt-get update \
 && apt-get install -y --no-install-recommends postgresql-16-wal2json \
 && rm -rf /var/lib/apt/lists/*
