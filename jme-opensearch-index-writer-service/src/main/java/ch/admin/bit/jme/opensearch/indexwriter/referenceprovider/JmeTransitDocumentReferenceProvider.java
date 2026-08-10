package ch.admin.bit.jme.opensearch.indexwriter.referenceprovider;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.opensearch.indexwriter.domain.indexing.reference.OriginReference;
import ch.admin.bit.jeap.opensearch.indexwriter.domain.indexing.reference.ReferenceProvider;
import ch.admin.bit.jme.transit.JmeCreateTransitDocumentCommand;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JmeTransitDocumentReferenceProvider implements ReferenceProvider<Message> {

    @Override
    public List<OriginReference> extractReference(Message message) {
        JmeCreateTransitDocumentCommand cmd = (JmeCreateTransitDocumentCommand) message;
        var result = new OriginReference("JmeTransitDocument", cmd.getPayload().getTransitDocumentId(), null);
        return List.of(result);
    }
}
