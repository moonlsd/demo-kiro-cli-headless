package com.example.demo.controller;

import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.OrderSummaryResponse;
import com.example.demo.dto.PagedResponse;
import com.example.demo.security.AppUserDetails;
import com.example.demo.service.OrderHistoryService;

import jakarta.validation.constraints.Min;

/**
 * HTTP endpoint for retrieving the authenticated user's order history
 * (KIRODEMO-003).
 *
 * <p>Thin web layer: it validates {@code page}/{@code size}, resolves the owning
 * user id from the authenticated principal (never from client input — FR-3), and
 * delegates to {@link OrderHistoryService}. It contains no business logic and
 * never touches the repository.
 */
@RestController
@RequestMapping("/api/v1/orders")
@Validated
public class OrderController {

    /** Documented default page index when {@code page} is omitted (FR-2). */
    private static final int DEFAULT_PAGE = 0;

    /** Documented default page size when {@code size} is omitted (FR-2). */
    private static final int DEFAULT_SIZE = 20;

    private final OrderHistoryService orderHistoryService;

    public OrderController(OrderHistoryService orderHistoryService) {
        this.orderHistoryService = orderHistoryService;
    }

    /**
     * Returns a page of the authenticated user's orders, most recent first.
     *
     * @param page      0-based page index (default {@code 0}, must be {@code >= 0})
     * @param size      requested page size (default {@code 20}, must be {@code >= 1};
     *                  capped by the service at the configured maximum)
     * @param principal the authenticated caller, supplying the scoping user id
     * @return {@code 200} with a {@link PagedResponse} of order summaries (empty
     *         when the user has no orders or the page is past the end)
     */
    @GetMapping
    public PagedResponse<OrderSummaryResponse> getOrderHistory(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) @Min(value = 0, message = "page must be zero or greater") int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) @Min(value = 1, message = "size must be at least 1") int size,
            @AuthenticationPrincipal AppUserDetails principal) {
        Page<OrderSummaryResponse> result =
                orderHistoryService.getOrderHistory(principal.getUserId(), page, size);
        return PagedResponse.from(result);
    }
}
