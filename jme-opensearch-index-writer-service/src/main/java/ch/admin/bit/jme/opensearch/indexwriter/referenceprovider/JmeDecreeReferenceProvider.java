package ch.admin.bit.jme.opensearch.indexwriter.referenceprovider;

import ch.admin.bit.jeap.event.shared.processarchive.archivedartifactversioncreated.SharedArchivedArtifactVersionCreatedEvent;
import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.opensearch.indexwriter.domain.indexing.reference.OriginReference;
import ch.admin.bit.jeap.opensearch.indexwriter.domain.indexing.reference.ReferenceProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JmeDecreeReferenceProvider implements ReferenceProvider<Message> {

    @Override
    public List<OriginReference> extractReference(Message message) {
        SharedArchivedArtifactVersionCreatedEvent event = (SharedArchivedArtifactVersionCreatedEvent) message;
        var result = new OriginReference("JmeDecree",
                event.getReferences().getStorageObject().getStorageObjectBucket() + ":" + event.getReferences().getStorageObject().getStorageObjectKey(),
                event.getReferences().getStorageObject().getStorageObjectVersionId());
        return List.of(result);
    }
}
