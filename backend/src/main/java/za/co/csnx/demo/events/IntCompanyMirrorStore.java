package za.co.csnx.demo.events;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Applies one company change event to the {@code demo.int_company} read-model —
 * plain-JDBC upserts (the mirror-store convention: no JPA entity for a derived
 * projection), <b>state-based</b> so replayed or re-delivered events converge.
 * I/U overwrite with the event's image and clear the tombstone; D tombstones
 * while retaining the last-known fields (a D for a never-seen company inserts a
 * PK-only tombstone). Runs inside the idempotent consumer's transaction.
 *
 * <p>Source: the platform's company master, captured from its Postgres WAL
 * (Channel 2b CDC, {@code platform.company.changed}, configured centrally on the
 * platform's CDCC screen).
 */
@Component
public class IntCompanyMirrorStore {

    private final NamedParameterJdbcTemplate jdbc;

    public IntCompanyMirrorStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void apply(CompanyChangeData change, Instant eventTime) {
        if ("D".equals(change.op())) {
            tombstone(change.companyCode(), eventTime);
        } else {
            upsert(change, eventTime);
        }
    }

    private void upsert(CompanyChangeData change, Instant eventTime) {
        jdbc.update("INSERT INTO demo.int_company"
                        + " (cpy_cd, description, active, last_op, is_deleted, last_event_at)"
                        + " VALUES (:code, :description, :active, :op, FALSE, :eventAt)"
                        + " ON CONFLICT (cpy_cd) DO UPDATE SET"
                        + "   description   = EXCLUDED.description,"
                        + "   active        = EXCLUDED.active,"
                        + "   last_op       = EXCLUDED.last_op,"
                        + "   is_deleted    = FALSE,"
                        + "   last_event_at = EXCLUDED.last_event_at,"
                        + "   updated_at    = now()",
                new MapSqlParameterSource()
                        .addValue("code", change.companyCode())
                        .addValue("description", change.description())
                        .addValue("active", change.active())
                        .addValue("op", change.op())
                        .addValue("eventAt", toTimestamp(eventTime)));
    }

    /** Tombstone — keeps the last-known description/active columns for history. */
    private void tombstone(String companyCode, Instant eventTime) {
        jdbc.update("INSERT INTO demo.int_company"
                        + " (cpy_cd, last_op, is_deleted, last_event_at)"
                        + " VALUES (:code, 'D', TRUE, :eventAt)"
                        + " ON CONFLICT (cpy_cd) DO UPDATE SET"
                        + "   last_op       = 'D',"
                        + "   is_deleted    = TRUE,"
                        + "   last_event_at = EXCLUDED.last_event_at,"
                        + "   updated_at    = now()",
                new MapSqlParameterSource()
                        .addValue("code", companyCode)
                        .addValue("eventAt", toTimestamp(eventTime)));
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
