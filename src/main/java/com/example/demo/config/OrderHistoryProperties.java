package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized pagination settings for the order-history endpoint
 * (KIRODEMO-003, FR-2 / NFR-5).
 *
 * <p>Bound from the {@code order-history} property prefix so the default and
 * maximum page sizes are tunable per environment without a code change.
 *
 * @param defaultPageSize page size applied when the caller omits {@code size}
 * @param maxPageSize     hard cap on the effective {@code size}; larger requests
 *                        are clamped to this value rather than rejected
 */
@ConfigurationProperties(prefix = "order-history")
public record OrderHistoryProperties(
        int defaultPageSize,
        int maxPageSize) {

    public OrderHistoryProperties {
        if (defaultPageSize < 1) {
            defaultPageSize = 20;
        }
        if (maxPageSize < 1) {
            maxPageSize = 100;
        }
    }
}
