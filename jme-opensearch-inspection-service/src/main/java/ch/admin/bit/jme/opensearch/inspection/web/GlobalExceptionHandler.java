package ch.admin.bit.jme.opensearch.inspection.web;

import ch.admin.bit.jeap.opensearch.client.auth.IndexTypeAccessDeniedException;
import ch.admin.bit.jeap.opensearch.client.auth.SearchItemAccessDeniedException;
import ch.admin.bit.jeap.opensearch.client.search.SearchItemClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@SuppressWarnings("unused")
@RestControllerAdvice
@Slf4j
class GlobalExceptionHandler {

    @ExceptionHandler({IndexTypeAccessDeniedException.class, SearchItemAccessDeniedException.class})
    ResponseEntity<Void> handleAccessDenied(RuntimeException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @ExceptionHandler(SearchItemClientException.class)
    ResponseEntity<Void> handleClientError(SearchItemClientException ex) {
        log.error("SearchItemClient error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
