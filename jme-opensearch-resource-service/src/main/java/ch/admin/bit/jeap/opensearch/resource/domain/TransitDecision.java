package ch.admin.bit.jeap.opensearch.resource.domain;

import ch.admin.bit.jeap.opensearch.indextype.Origin;

import java.time.Instant;

/**
 * Current state of a transit decision. Search items for every supported schema major are projected from this one
 * object; {@code origin.version} is the business revision and unrelated to the schema major.
 */
public record TransitDecision(
        Origin origin,
        String status,
        String remarks,
        String decidedBy,
        Instant decisionDate,
        String transitDocumentId) {

    public String id() {
        return origin.id();
    }
}
