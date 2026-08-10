package ch.admin.bit.jeap.opensearch.resource.domain;

import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV2;
import ch.admin.bit.jme.opensearch.index.jme.transitdocument.JmeTransitDocumentDataV1;

public interface MessagePublisher {

    void createTransitDocument(JmeTransitDocumentDataV1 transitDocumentDto);

    void transitDecisionCreated(JmeTransitDecisionDataV1 transitDecisionDto);

    void transitDecisionCreatedV2(JmeTransitDecisionDataV2 transitDecisionDto);
}
