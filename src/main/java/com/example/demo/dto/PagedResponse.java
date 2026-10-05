package com.example.demo.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Stable, framework-agnostic page envelope (KIRODEMO-003, FR-2).
 *
 * <p>Used instead of serializing Spring Data's internal {@code Page}/{@code
 * PageImpl}, whose JSON shape is unstable across versions and leaks internal
 * fields. This record is the documented response contract.
 *
 * @param <T>           the element type
 * @param content       the page's elements (empty, never {@code null})
 * @param page          the 0-based page number returned
 * @param size          the page size that was applied
 * @param totalElements total number of elements across all pages
 * @param totalPages    total number of pages
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    /**
     * Builds an envelope from a Spring Data {@link Page}.
     *
     * @param page the source page
     * @param <T>  the element type
     * @return a stable {@code PagedResponse} mirroring the page's content and metadata
     */
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
