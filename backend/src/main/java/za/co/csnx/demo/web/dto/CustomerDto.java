package za.co.csnx.demo.web.dto;

import java.time.Instant;

public record CustomerDto(
        Long id,
        String email,
        String displayName,
        Instant memberSince) {
}
