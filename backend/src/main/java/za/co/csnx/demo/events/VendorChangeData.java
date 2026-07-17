package za.co.csnx.demo.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DEMO's OWN tolerant copy of the {@code pom.vendor.changed} payload — the
 * consumer-side record convention (JSON keys are the contract; no shared
 * code). Keys are the source table's snake_case column names as emitted by
 * the WAL CDC capture; the ~25 columns this mirror doesn't need are ignored,
 * and a delete's PK-only image leaves the non-key fields null.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VendorChangeData(
        String op,
        @JsonProperty("vendor_code") String vendorCode,
        @JsonProperty("vendor_name") String vendorName,
        String city,
        String country,
        @JsonProperty("contact_email") String contactEmail,
        Boolean active) {
}
