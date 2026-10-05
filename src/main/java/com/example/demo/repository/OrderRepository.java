package com.example.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.Order;

/**
 * Persistence access for {@link Order} (KIRODEMO-003).
 *
 * <p>Exposes a single, user-scoped, paginated lookup. The derived query is
 * parameterized by construction (no string concatenation), satisfying the data and
 * security standards. Sort and page size are supplied by the service via the
 * {@link Pageable}, so no {@code @Query} is required.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Finds a single page of orders belonging to the given user.
     *
     * @param userId   the owning user's id (the mandatory scoping filter)
     * @param pageable page, size, and sort supplied by the service
     * @return a page of the user's orders (never {@code null}; empty when the user
     *         has none or the page is past the end)
     */
    Page<Order> findByUserId(Long userId, Pageable pageable);
}
