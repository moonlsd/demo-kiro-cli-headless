package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.example.demo.config.OrderHistoryProperties;
import com.example.demo.domain.Order;
import com.example.demo.domain.OrderStatus;
import com.example.demo.dto.OrderSummaryResponse;
import com.example.demo.repository.OrderRepository;

/**
 * Unit tests for {@link OrderHistoryService} (KIRODEMO-003).
 *
 * <p>Covers size capping (FR-2), the forced most-recent-first sort (FR-5),
 * principal-derived scoping (FR-3), and entity-to-summary mapping (FR-1).
 */
@ExtendWith(MockitoExtension.class)
class OrderHistoryServiceTests {

    @Mock
    private OrderRepository orderRepository;

    private final OrderHistoryProperties properties = new OrderHistoryProperties(20, 100);

    private OrderHistoryService service() {
        return new OrderHistoryService(orderRepository, properties);
    }

    @Test
    void capsPageSizeAtConfiguredMaximum() {
        when(orderRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(Page.empty());

        service().getOrderHistory(1L, 0, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findByUserId(eq(1L), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void keepsRequestedSizeWhenBelowMaximum() {
        when(orderRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(Page.empty());

        service().getOrderHistory(1L, 0, 10);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findByUserId(eq(1L), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void appliesMostRecentFirstSort() {
        when(orderRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(Page.empty());

        service().getOrderHistory(1L, 0, 20);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findByUserId(eq(1L), captor.capture());
        Sort.Order sortOrder = captor.getValue().getSort().getOrderFor("orderDate");
        assertThat(sortOrder).isNotNull();
        assertThat(sortOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void scopesQueryToSuppliedUserId() {
        when(orderRepository.findByUserId(eq(42L), any(Pageable.class)))
                .thenReturn(Page.empty());

        service().getOrderHistory(42L, 0, 20);

        verify(orderRepository).findByUserId(eq(42L), any(Pageable.class));
    }

    @Test
    void mapsEntitiesToSummaryExposingOnlySummaryFields() {
        Instant date = Instant.parse("2026-09-18T14:22:05Z");
        Order order = new Order(1L, date, OrderStatus.DELIVERED, new BigDecimal("129.99"), "USD");
        Page<Order> page = new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1);
        when(orderRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<OrderSummaryResponse> result = service().getOrderHistory(1L, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        OrderSummaryResponse summary = result.getContent().get(0);
        assertThat(summary.orderDate()).isEqualTo(date);
        assertThat(summary.status()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(summary.totalAmount()).isEqualByComparingTo("129.99");
        assertThat(summary.currency()).isEqualTo("USD");
    }
}
