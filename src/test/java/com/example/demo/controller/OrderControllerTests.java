package com.example.demo.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.config.SecurityConfig;
import com.example.demo.domain.OrderStatus;
import com.example.demo.dto.OrderSummaryResponse;
import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.security.AppUserDetails;
import com.example.demo.service.OrderHistoryService;

/**
 * Web slice tests for {@link OrderController} (KIRODEMO-003).
 *
 * <p>Covers the happy path and empty page (FR-1, FR-2), unauthenticated access
 * (FR-3), and pagination input validation (FR-4) with the central problem-detail
 * shape. The service is mocked; security filters are active.
 */
@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class OrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderHistoryService orderHistoryService;

    private AppUserDetails principal(long userId) {
        return new AppUserDetails(userId, "user" + userId, "pw");
    }

    @Test
    void authenticatedRequestReturnsPageOfOrders() throws Exception {
        OrderSummaryResponse summary = new OrderSummaryResponse(
                10432L, Instant.parse("2026-09-18T14:22:05Z"),
                OrderStatus.DELIVERED, new BigDecimal("129.99"), "USD");
        Page<OrderSummaryResponse> page = new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1);
        when(orderHistoryService.getOrderHistory(eq(1L), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/v1/orders").param("page", "0").param("size", "20")
                        .with(user(principal(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value(10432))
                .andExpect(jsonPath("$.content[0].status").value("DELIVERED"))
                .andExpect(jsonPath("$.content[0].totalAmount").value(129.99))
                .andExpect(jsonPath("$.content[0].currency").value("USD"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                // Must not leak the owning userId or payment/internal fields.
                .andExpect(jsonPath("$.content[0].userId").doesNotExist());
    }

    @Test
    void userWithNoOrdersGetsEmptyPageNotFound() throws Exception {
        Page<OrderSummaryResponse> empty = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(orderHistoryService.getOrderHistory(eq(1L), anyInt(), anyInt())).thenReturn(empty);

        mockMvc.perform(get("/api/v1/orders").with(user(principal(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void unauthenticatedRequestReturns401WithProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/orders"));
    }

    @Test
    void negativePageReturns400WithFieldError() throws Exception {
        mockMvc.perform(get("/api/v1/orders").param("page", "-1").with(user(principal(1L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("page"))
                .andExpect(jsonPath("$.fieldErrors[0].message").exists());
    }

    @Test
    void nonPositiveSizeReturns400WithFieldError() throws Exception {
        mockMvc.perform(get("/api/v1/orders").param("size", "0").with(user(principal(1L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
    }

    @Test
    void nonNumericSizeReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/orders").param("size", "abc").with(user(principal(1L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
    }
}
