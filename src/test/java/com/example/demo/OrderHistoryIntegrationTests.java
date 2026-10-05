package com.example.demo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.domain.Order;
import com.example.demo.domain.OrderStatus;
import com.example.demo.repository.OrderRepository;

/**
 * Full-stack integration test for the order-history feature (KIRODEMO-003).
 *
 * <p>Verifies the critical privacy guarantee (a user sees only their own orders —
 * FR-3) and most-recent-first ordering with pagination metadata (FR-2, FR-5)
 * against seeded data, through the real security, controller, service, and
 * repository layers. Users {@code alice} (id 1) and {@code bob} (id 2) come from
 * the in-memory user store in {@code SecurityConfig}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderHistoryIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void seed() {
        orderRepository.deleteAll();
        Instant now = Instant.now();
        // alice (userId 1): two orders on different dates.
        orderRepository.save(new Order(1L, now.minus(2, ChronoUnit.DAYS),
                OrderStatus.DELIVERED, new BigDecimal("50.00"), "USD"));
        orderRepository.save(new Order(1L, now,
                OrderStatus.SHIPPED, new BigDecimal("75.00"), "USD"));
        // bob (userId 2): one order that must never appear for alice.
        orderRepository.save(new Order(2L, now,
                OrderStatus.PAID, new BigDecimal("999.00"), "USD"));
    }

    @Test
    void userSeesOnlyTheirOwnOrdersMostRecentFirst() throws Exception {
        mockMvc.perform(get("/api/v1/orders").with(httpBasic("alice", "password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                // most recent first (FR-5)
                .andExpect(jsonPath("$.content[0].status").value("SHIPPED"))
                .andExpect(jsonPath("$.content[1].status").value("DELIVERED"));
    }

    @Test
    void anotherUserSeesOnlyTheirOrder() throws Exception {
        mockMvc.perform(get("/api/v1/orders").with(httpBasic("bob", "password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("PAID"));
    }

    @Test
    void sizeIsCappedAtConfiguredMaximum() throws Exception {
        mockMvc.perform(get("/api/v1/orders").param("size", "5000")
                        .with(httpBasic("alice", "password")))
                .andExpect(status().isOk())
                // effective size capped at 100 (order-history.max-page-size)
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized());
    }
}
