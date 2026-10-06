package com.example.ecommerce.util;

import com.example.ecommerce.constant.AppConstants;
import com.example.ecommerce.exception.BadRequestException;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Builds safe Pageable objects: caps page size and only allows sorting by whitelisted fields,
 * so clients cannot sort by arbitrary (or sensitive) columns.
 */
public final class PageableFactory {

    private PageableFactory() {
    }

    public static Pageable of(int page, int size, String sortBy, String direction,
                              Set<String> allowedSortFields, String defaultSortField) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), AppConstants.MAX_PAGE_SIZE);

        String field = (sortBy == null || sortBy.isBlank()) ? defaultSortField : sortBy;
        if (!allowedSortFields.contains(field)) {
            throw new BadRequestException("Unsupported sort field: " + field);
        }
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(dir, field));
    }
}
