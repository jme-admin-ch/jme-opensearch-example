package ch.admin.bit.jme.opensearch.inspection.web;

import ch.admin.bit.jeap.opensearch.client.auth.Authorization;
import ch.admin.bit.jeap.opensearch.client.domain.SearchItemTyped;
import ch.admin.bit.jeap.opensearch.indextype.Origin;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionIndexTypeV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdocument.JmeTransitDocumentDataV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdocument.JmeTransitDocumentIndexTypeV1;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Shared test fixtures for controller tests. Keeps the {@code Authorization}
 * and {@code SearchItemTyped} construction in one place.
 */
final class TestData {

    static final String ROLE_READ = "jme_read";
    static final String BP1 = "BP1";
    static final String BP2 = "BP2";

    private TestData() {
    }

    static Authorization globalUserAuth() {
        return new Authorization(Set.of(ROLE_READ), Map.of());
    }

    static Authorization bpOnlyAuth(String bpId) {
        return new Authorization(Set.of(), Map.of(bpId, Set.of(ROLE_READ)));
    }

    static Authorization noAccessAuth() {
        return new Authorization(Set.of(), Map.of());
    }

    static Origin origin(String id, String bpId) {
        return new Origin(id, "1.0", bpId, null, Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z"), null);
    }

    static JmeTransitDecisionDataV1 decisionData(String id) {
        return new JmeTransitDecisionDataV1(id, "ACCEPTED", "no remarks", "alice",
                Instant.parse("2024-01-02T00:00:00Z"), "doc-" + id);
    }

    static JmeTransitDocumentDataV1 documentData(String id) {
        return new JmeTransitDocumentDataV1(id, "MRN-" + id, "decl-1", "ZH", "BE",
                "goods", "no remarks");
    }

    static SearchItemTyped<JmeTransitDecisionDataV1> decisionItem(String id, String bpId) {
        return new SearchItemTyped<>(origin(id, bpId), decisionData(id), JmeTransitDecisionIndexTypeV1.INSTANCE);
    }

    static SearchItemTyped<JmeTransitDocumentDataV1> documentItem(String id, String bpId) {
        return new SearchItemTyped<>(origin(id, bpId), documentData(id),
                JmeTransitDocumentIndexTypeV1.INSTANCE);
    }
}
