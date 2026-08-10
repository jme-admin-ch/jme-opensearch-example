package ch.admin.bit.jme.opensearch.indexwriter.referenceprovider;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.opensearch.indexwriter.domain.indexing.reference.OriginReference;
import ch.admin.bit.jeap.opensearch.indexwriter.domain.indexing.reference.ReferenceProvider;
import ch.admin.bit.jme.transit.JmeTransitDecisionCreatedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JmeTransitDecisionReferenceProvider implements ReferenceProvider<Message> {

    @Override
    public List<OriginReference> extractReference(Message message) {
        JmeTransitDecisionCreatedEvent event = (JmeTransitDecisionCreatedEvent) message;
        var result = new OriginReference("JmeTransitDecision", event.getPayload().getTransitDecisionId(), null);
        return List.of(result);
    }
}
