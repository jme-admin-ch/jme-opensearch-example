package ch.admin.bit.jme.opensearch.indexwriter.referenceprovider;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.opensearch.indexwriter.domain.indexing.reference.OriginReference;
import ch.admin.bit.jeap.opensearch.indexwriter.domain.indexing.reference.ReferenceProvider;
import ch.admin.bit.jme.document.JmeDocumentCreatedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JmeDiagramReferenceProvider implements ReferenceProvider<Message> {

    @Override
    public List<OriginReference> extractReference(Message message) {
        JmeDocumentCreatedEvent event = (JmeDocumentCreatedEvent) message;
        var result = new OriginReference("JmeDiagram", event.getReferences().getDocumentReference().getId(), null);
        return List.of(result);
    }
}
