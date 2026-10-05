package com.example.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.example.demo.domain.Order;
import com.example.demo.domain.OrderStatus;

/**
 * Repository slice tests for {@link OrderRepository} (KIRODEMO-003).
 *
 * <p>Verifies user scoping (FR-3), paging (FR-2), and the most-recent-first sort
 * (FR-5) at the data layer, against the Flyway-managed schema.
 */
@DataJpaTest
class OrderRepositoryTests {

    @Autowired
    private OrderRepository orderRepository;

    private static final Sort DATE_DESC = Sort.by(Sort.Direction.DESC, "orderDate");

    private Order order(long userId, Instant date) {
        return new Order(userId, date, OrderStatus.DELIVERED, new BigDecimal("10.00"), "USD");
    }

    @Test
    void findByUserIdReturnsOnlyThatUsersOrders() {
        Instant now = Instant.now();
        orderRepository.save(order(1L, now));
        orderRepository.save(order(1L, now.minus(1, ChronoUnit.DAYS)));
        orderRepository.save(order(2L, now));

        Page<Order> page = orderRepository.findByUserId(1L, PageRequest.of(0, 10, DATE_DESC));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(o -> o.getUserId().equals(1L));
    }

    @Test
    void findByUserIdHonorsMostRecentFirstSort() {
        Instant now = Instant.now();
        Order older = orderRepository.save(order(1L, now.minus(2, ChronoUnit.DAYS)));
        Order newer = orderRepository.save(order(1L, now));

        Page<Order> page = orderRepository.findByUserId(1L, PageRequest.of(0, 10, DATE_DESC));

        assertThat(page.getContent()).extracting(Order::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void findByUserIdHonorsPageSize() {
        Instant now = Instant.now();
        for (int i = 0; i < 5; i++) {
            orderRepository.save(order(1L, now.minus(i, ChronoUnit.HOURS)));
        }

        Page<Order> firstPage = orderRepository.findByUserId(1L, PageRequest.of(0, 2, DATE_DESC));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
    }

    @Test
    void findByUserIdReturnsEmptyPageForUserWithNoOrders() {
        Page<Order> page = orderRepository.findByUserId(99L, PageRequest.of(0, 10, DATE_DESC));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void findByUserIdReturnsEmptyPageWhenPastTheEnd() {
        orderRepository.save(order(1L, Instant.now()));

        Page<Order> page = orderRepository.findByUserId(1L, PageRequest.of(5, 10, DATE_DESC));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(1);
    }
}
