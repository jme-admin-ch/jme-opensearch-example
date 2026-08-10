package ch.admin.bit.jme.opensearch.indexwriter.condition;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.opensearch.indexwriter.domain.indexing.condition.IndexingCondition;
import ch.admin.bit.jme.document.JmeDocumentCreatedEvent;
import org.springframework.stereotype.Component;

@Component
public class JmeDiagramCondition implements IndexingCondition<Message> {

    @Override
    public boolean evaluate(Message message) {
        JmeDocumentCreatedEvent event = (JmeDocumentCreatedEvent) message;
        return "JmeDiagram".equals(event.getReferences().getDocumentReference().getType());
    }
}
