# Undo scripts — `db/migration/undo/`

One undo script per forward migration, same name with a `U` prefix:

```
db/migration/V20260810101500__add_widget_table.sql        <- forward
db/migration/undo/U20260810101500__add_widget_table.sql   <- its undo
```

These scripts are **not packaged and Flyway never scans them**
(`backend/pom.xml` excludes this folder — Flyway scans a location
recursively, and `flyway undo` is a Teams-edition feature we don't have).
They are run by hand, newest version first, by whoever is rolling back.

Full rules, including what to write when a migration is not reversible:
`platform/docs/migrations.md` §"Every migration ships an undo script".
Enforced here by `UndoScriptCoverageTest`.
