package com.example.demo.dto;

import jakarta.validation.constraints.Min;

/**
 * Validated pagination input for the order-history endpoint (KIRODEMO-003, FR-4).
 *
 * <p>Only structurally invalid values are rejected here with {@code 400}: a
 * negative {@code page} or a non-positive {@code size}. A {@code size} above the
 * configured maximum is <em>not</em> rejected — it is capped by the service
 * (FR-2). Defaults are applied by the controller when a parameter is omitted.
 *
 * @param page the 0-based page index ({@code >= 0})
 * @param size the requested page size ({@code >= 1}; capped by the service)
 */
public record OrderPageQuery(
        @Min(value = 0, message = "page must be zero or greater") int page,
        @Min(value = 1, message = "size must be at least 1") int size) {
}
