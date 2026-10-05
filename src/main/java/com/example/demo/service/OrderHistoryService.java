package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.config.OrderHistoryProperties;
import com.example.demo.dto.OrderSummaryResponse;
import com.example.demo.repository.OrderRepository;

/**
 * Business logic for retrieving a user's order history (KIRODEMO-003).
 *
 * <p>Owns the feature's business rules: it scopes every query to the
 * authenticated user id supplied by the caller (default-deny — FR-3), caps the
 * effective page size at the configured maximum (FR-2), fixes the sort to
 * {@code orderDate} descending (FR-5), and maps entities to summary DTOs so no
 * entity ever leaves this boundary (FR-1).
 */
@Service
public class OrderHistoryService {

    private static final Logger log = LoggerFactory.getLogger(OrderHistoryService.class);

    /** Fixed sort for this release: most-recent orders first (FR-5). */
    private static final Sort ORDER_DATE_DESC = Sort.by(Sort.Direction.DESC, "orderDate");

    private final OrderRepository orderRepository;
    private final OrderHistoryProperties properties;

    public OrderHistoryService(OrderRepository orderRepository, OrderHistoryProperties properties) {
        this.orderRepository = orderRepository;
        this.properties = properties;
    }

    /**
     * Returns a page of the given user's orders, most recent first.
     *
     * <p>The {@code userId} must be the authenticated principal's id resolved by
     * the caller; it is applied as a mandatory repository filter. The requested
     * {@code size} is clamped to the configured maximum; the sort is always
     * {@code orderDate} descending regardless of client input.
     *
     * @param userId the authenticated user's id (the scoping filter)
     * @param page   the 0-based page index
     * @param size   the requested page size (clamped to the configured maximum)
     * @return a page of {@link OrderSummaryResponse}, empty when the user has no
     *         orders or the page is past the end
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getOrderHistory(Long userId, int page, int size) {
        int effectiveSize = Math.min(size, properties.maxPageSize());
        Pageable pageable = PageRequest.of(page, effectiveSize, ORDER_DATE_DESC);

        Page<OrderSummaryResponse> result = orderRepository.findByUserId(userId, pageable)
                .map(OrderSummaryResponse::from);

        // Business milestone log: acting user and page metadata only — never order
        // contents, totals, or any PII (NFR-4).
        log.info("Order history accessed by user {}: page={} size={} totalElements={}",
                userId, page, effectiveSize, result.getTotalElements());

        return result;
    }
}
