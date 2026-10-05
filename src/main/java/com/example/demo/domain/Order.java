package com.example.demo.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity mapping the existing {@code orders} table (KIRODEMO-003).
 *
 * <p>This feature only <em>reads</em> orders; it never creates or modifies them.
 * The entity is deliberately mapped to a minimal set of columns — payment and
 * internal-only columns, if present on the table, are intentionally not mapped so
 * they can never leak into a response.
 *
 * <p>Entities never cross the HTTP boundary: the service maps an {@code Order} to
 * an {@code OrderSummaryResponse} DTO before returning it.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Owning user; the mandatory scoping filter for every query. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** When the order was placed, stored in UTC. Also the default sort key. */
    @Column(name = "order_date", nullable = false)
    private Instant orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    /** Monetary total; {@link BigDecimal} to avoid floating-point rounding. */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /** ISO-4217 currency code. */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** Required by JPA. */
    protected Order() {
    }

    public Order(Long userId, Instant orderDate, OrderStatus status, BigDecimal totalAmount, String currency) {
        this.userId = userId;
        this.orderDate = orderDate;
        this.status = status;
        this.totalAmount = totalAmount;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getOrderDate() {
        return orderDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }
}
