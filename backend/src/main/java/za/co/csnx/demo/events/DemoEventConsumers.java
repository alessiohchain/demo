package za.co.csnx.demo.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import za.co.csnx.engine.events.EventEnvelope;
import za.co.csnx.engine.events.IdempotentEventHandler;

/**
 * DEMO's inbound integration events (Channel 2). A single {@code Consumer} bean
 * bound in application.yml as {@code integrationEvents-in-0} → destination
 * {@code csnx.events}, durable group {@code demo}; it dispatches by event type,
 * ignoring the foreign types that also land on the shared exchange.
 *
 * <p>Slices:
 * <ul>
 *   <li>{@code pom.vendor.changed} — pom's vendor master → {@code demo.int_vendor}.</li>
 *   <li>{@code platform.company.changed} — the platform's company master →
 *       {@code demo.int_company} (the platform → demo leg of the CDC triangle).</li>
 * </ul>
 * Both are WAL-CDC captured (configured centrally on the platform's CDCC screen).
 * Delivery is at-least-once: the inbox dedups broker redelivery, and the mirror
 * writes are state-based so CDC-level replays (fresh event ids) converge too.
 * Flow walkthrough: {@code docs/cdc-vendor-pom-to-demo.md} (platform repo).
 */
@Configuration
@ConditionalOnProperty(prefix = "csnx.events", name = "enabled", havingValue = "true")
public class DemoEventConsumers {

    private static final Logger log = LoggerFactory.getLogger(DemoEventConsumers.class);

    static final String VENDOR_CHANGED = "pom.vendor.changed";
    static final String COMPANY_CHANGED = "platform.company.changed";

    /** Stable ids for each subscriber in the inbox dedup ledger (one per slice). */
    static final String VENDOR_CONSUMER_ID = "demo:intVendorMirror";
    static final String COMPANY_CONSUMER_ID = "demo:intCompanyMirror";

    private final IdempotentEventHandler idempotent;
    private final IntVendorMirrorStore vendorMirror;
    private final IntCompanyMirrorStore companyMirror;
    private final ObjectMapper mapper;

    public DemoEventConsumers(IdempotentEventHandler idempotent,
                              IntVendorMirrorStore vendorMirror,
                              IntCompanyMirrorStore companyMirror,
                              ObjectMapper mapper) {
        this.idempotent = idempotent;
        this.vendorMirror = vendorMirror;
        this.companyMirror = companyMirror;
        this.mapper = mapper;
    }

    @Bean
    public Consumer<EventEnvelope> integrationEvents() {
        return event -> {
            String type = event.type();
            if (VENDOR_CHANGED.equals(type)) {
                idempotent.handle(event, VENDOR_CONSUMER_ID, e -> {
                    VendorChangeData change = e.dataAs(mapper, VendorChangeData.class);
                    vendorMirror.apply(change, e.time());
                    log.info("Mirrored vendor {} (op {}, event {})", e.subject(), change.op(), e.id());
                });
            } else if (COMPANY_CHANGED.equals(type)) {
                idempotent.handle(event, COMPANY_CONSUMER_ID, e -> {
                    CompanyChangeData change = e.dataAs(mapper, CompanyChangeData.class);
                    companyMirror.apply(change, e.time());
                    log.info("Mirrored company {} (op {}, event {})", e.subject(), change.op(), e.id());
                });
            }
            // other event types on the shared exchange — not ours
        };
    }
}
