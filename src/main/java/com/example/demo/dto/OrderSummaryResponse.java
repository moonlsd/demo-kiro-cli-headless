package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.demo.domain.Order;
import com.example.demo.domain.OrderStatus;

/**
 * Minimal, client-facing summary of a single order (KIRODEMO-003, FR-1).
 *
 * <p>Carries only the identifier, date, status, and total (with currency) — the
 * information a user needs to recognize a past purchase. It deliberately omits the
 * owning {@code userId}, payment instruments, line items, and any internal-only
 * fields, in line with the data-minimization requirement.
 *
 * @param orderId     the order identifier
 * @param orderDate   when the order was placed, serialized as ISO-8601 UTC
 * @param status      the order's lifecycle status
 * @param totalAmount the monetary total
 * @param currency    the ISO-4217 currency code for {@code totalAmount}
 */
public record OrderSummaryResponse(
        Long orderId,
        Instant orderDate,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency) {

    /**
     * Maps a persistence {@link Order} to its summary DTO. This is the single place
     * entities are translated for transport, so no entity ever leaves the service.
     *
     * @param order the source entity
     * @return the minimal summary projection
     */
    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderDate(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency());
    }
}
