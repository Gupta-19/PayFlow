package com.ketan.payflow.common;

import java.util.List;

import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> items, int page, int size, long totalItems) {
    public static <T> PageResponse<T> from(Page<T> p) {
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements());
    }
}
