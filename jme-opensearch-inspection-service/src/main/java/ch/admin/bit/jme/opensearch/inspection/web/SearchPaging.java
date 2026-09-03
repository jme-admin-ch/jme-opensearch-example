package ch.admin.bit.jme.opensearch.inspection.web;

import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.function.Consumer;

/**
 * Translates a Spring Data {@link Pageable} into the paging and sorting options of an OpenSearch
 * search request, so the inspection endpoints can take {@code page}, {@code size} and {@code sort}
 * from the caller.
 * <p>
 * A search request always needs an explicit order. Its result is capped at {@link #DEFAULT_PAGE_SIZE
 * a page size}, and without a sort OpenSearch returns an arbitrary slice of the matches — a freshly
 * indexed item can then stay invisible for good once more documents match than fit on a page. Callers
 * that pass no sort therefore get {@link #DEFAULT_SORT}, newest first.
 * <p>
 * Sort properties are OpenSearch field names such as {@code origin.created}, and the field has to be
 * mapped as sortable in the index type. Paging uses {@code from}/{@code size}, so it is bounded by
 * the {@code index.max_result_window} of the index (10'000 by default).
 */
final class SearchPaging {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final String CREATED_FIELD = "origin.created";
    static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, CREATED_FIELD);

    private SearchPaging() {
    }

    static Consumer<SearchRequest.Builder> of(Pageable pageable) {
        return builder -> {
            builder.from(Math.toIntExact(pageable.getOffset()))
                    .size(pageable.getPageSize());
            pageable.getSortOr(DEFAULT_SORT).forEach(order -> builder.sort(sort -> sort.field(field -> field
                    .field(order.getProperty())
                    .order(order.isAscending() ? SortOrder.Asc : SortOrder.Desc))));
        };
    }
}
