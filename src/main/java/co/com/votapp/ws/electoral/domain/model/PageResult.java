package co.com.votapp.ws.electoral.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Pure Java domain abstraction for paginated results.
 *
 * <p>Replaces {@code org.springframework.data.domain.Page} at domain layer boundaries
 * so that ports and use cases remain free of any Spring framework imports.
 *
 * <p>Adapters map from Spring's {@code Page} to this type at the infrastructure boundary.
 *
 * @param <T>           element type
 * @param content        the elements on this page
 * @param page           zero-based page index
 * @param size           page size requested
 * @param totalElements  total number of elements across all pages
 * @param totalPages     total number of pages
 */
public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public PageResult {
        Objects.requireNonNull(content, "content must not be null");
        content = List.copyOf(content);
    }

    /**
     * Returns an empty page result.
     */
    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), page, size, 0L, 0);
    }

    /**
     * Transform all elements using the given mapper function.
     */
    public <R> PageResult<R> map(Function<T, R> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        List<R> mapped = content.stream().map(mapper).toList();
        return new PageResult<>(mapped, page, size, totalElements, totalPages);
    }

    /**
     * Returns {@code true} if there are no elements on this page.
     */
    public boolean isEmpty() {
        return content.isEmpty();
    }

    /**
     * Returns the number of elements on this page.
     */
    public int numberOfElements() {
        return content.size();
    }
}
