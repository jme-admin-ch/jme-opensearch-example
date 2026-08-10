package ch.admin.bit.jme.opensearch.indexwriter.condition;

import ch.admin.bit.jeap.event.shared.processarchive.archivedartifactversioncreated.SharedArchivedArtifactVersionCreatedEvent;
import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.opensearch.indexwriter.domain.indexing.condition.IndexingCondition;
import org.springframework.stereotype.Component;

@Component
public class JmeDecreeDocumentCondition implements IndexingCondition<Message> {

    @Override
    public boolean evaluate(Message message) {
        SharedArchivedArtifactVersionCreatedEvent event = (SharedArchivedArtifactVersionCreatedEvent) message;
        return "DecreeDocument".equals(event.getReferences().getArchivedArtifactType().getDataSchemaType());
    }
}
