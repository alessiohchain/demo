package za.co.csnx.demo.events;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Applies one vendor change event to the {@code demo.int_vendor} read-model —
 * plain-JDBC upserts (pom's mirror-store convention: no JPA entity needed for
 * a derived projection), <b>state-based</b> so replayed or re-delivered events
 * converge. I/U overwrite with the event's image and clear the tombstone; D
 * tombstones while retaining the last-known fields (a D for a never-seen
 * vendor inserts a PK-only tombstone). Runs inside the idempotent consumer's
 * transaction.
 */
@Component
public class IntVendorMirrorStore {

    private final NamedParameterJdbcTemplate jdbc;

    public IntVendorMirrorStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void apply(VendorChangeData change, Instant eventTime) {
        if ("D".equals(change.op())) {
            tombstone(change.vendorCode(), eventTime);
        } else {
            upsert(change, eventTime);
        }
    }

    private void upsert(VendorChangeData change, Instant eventTime) {
        jdbc.update("INSERT INTO demo.int_vendor"
                        + " (vendor_code, vendor_name, city, country, contact_email, active,"
                        + " last_op, is_deleted, last_event_at)"
                        + " VALUES (:code, :name, :city, :country, :email, :active,"
                        + " :op, FALSE, :eventAt)"
                        + " ON CONFLICT (vendor_code) DO UPDATE SET"
                        + "   vendor_name   = EXCLUDED.vendor_name,"
                        + "   city          = EXCLUDED.city,"
                        + "   country       = EXCLUDED.country,"
                        + "   contact_email = EXCLUDED.contact_email,"
                        + "   active        = EXCLUDED.active,"
                        + "   last_op       = EXCLUDED.last_op,"
                        + "   is_deleted    = FALSE,"
                        + "   last_event_at = EXCLUDED.last_event_at,"
                        + "   updated_at    = now()",
                new MapSqlParameterSource()
                        .addValue("code", change.vendorCode())
                        .addValue("name", change.vendorName())
                        .addValue("city", change.city())
                        .addValue("country", change.country())
                        .addValue("email", change.contactEmail())
                        .addValue("active", change.active())
                        .addValue("op", change.op())
                        .addValue("eventAt", toTimestamp(eventTime)));
    }

    /** Tombstone — keeps the last-known name/city/... columns for history. */
    private void tombstone(String vendorCode, Instant eventTime) {
        jdbc.update("INSERT INTO demo.int_vendor"
                        + " (vendor_code, last_op, is_deleted, last_event_at)"
                        + " VALUES (:code, 'D', TRUE, :eventAt)"
                        + " ON CONFLICT (vendor_code) DO UPDATE SET"
                        + "   last_op       = 'D',"
                        + "   is_deleted    = TRUE,"
                        + "   last_event_at = EXCLUDED.last_event_at,"
                        + "   updated_at    = now()",
                new MapSqlParameterSource()
                        .addValue("code", vendorCode)
                        .addValue("eventAt", toTimestamp(eventTime)));
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
