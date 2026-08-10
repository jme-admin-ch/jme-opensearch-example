package ch.admin.bit.jme.opensearch.indexwriter.condition;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.opensearch.indexwriter.domain.indexing.condition.IndexingCondition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JmeTransitDocumentCondition implements IndexingCondition<Message> {

    @Override
    public boolean evaluate(Message message) {
        log.info("Evaluating message with id {} and type {}", message.getIdentity().getId(), message.getType());
        // This is just to show how e condition could be implemented
        return true;
    }
}
