package com.ecommerce.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /** The actual page content - list of DTOs */
    private List<T> content;

    /** Zero-based current page number (0 = first page) */
    private int pageNumber;

    /** Number of items requested per page */
    private int pageSize;

    /** Total number of items across ALL pages */
    private long totalElements;

    /** Total number of pages = ceil(totalElements / pageSize) */
    private int totalPages;

    /** true if this is the first page (pageNumber == 0) */
    private boolean first;

    /** true if this is the last page */
    private boolean last;

    /** true if content list is empty */
    private boolean empty;

    // =========================================================================
    // STATIC FACTORY METHODS
    // =========================================================================

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        List<T> content = page.getContent()
                .stream()
                .map(mapper)
                .collect(Collectors.toList());

        return PageResponse.<T>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }

    /**
     * Creates a PageResponse from an already-converted list of DTOs
     * and the original Spring Data Page metadata.
     *
     * Use when you have already mapped the page content separately.
     */
    public static <T> PageResponse<T> fromMappedContent(Page<?> page, List<T> mappedContent) {
        return PageResponse.<T>builder()
                .content(mappedContent)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }
}
