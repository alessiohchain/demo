package za.co.csnx.demo.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DEMO's OWN tolerant copy of the {@code platform.company.changed} payload — the
 * consumer-side record convention (JSON keys are the contract; no shared code).
 * Keys are the source table's snake_case column names as emitted by the WAL CDC
 * capture of {@code platform.company}; columns this mirror doesn't need are
 * ignored, and a delete's PK-only image leaves the non-key fields null.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CompanyChangeData(
        String op,
        @JsonProperty("cpy_cd") String companyCode,
        String description,
        Boolean active) {
}
