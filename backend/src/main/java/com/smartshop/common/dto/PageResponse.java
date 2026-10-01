package com.smartshop.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Page de resultats au format JSON stable (evite de serialiser PageImpl).
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
