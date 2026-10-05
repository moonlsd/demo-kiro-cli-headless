package com.example.demo.domain;

/**
 * Lifecycle status of an {@link Order}.
 *
 * <p>Exposed in the order-history summary (KIRODEMO-003) so a user can recognize
 * the state of a past purchase. The set is intentionally small and stable; it is
 * part of the API contract, so values must not be repurposed.
 */
public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
