package ch.admin.bit.jeap.opensearch.resource.kafka;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.opensearch.resource.domain.MessagePublisher;
import ch.admin.bit.jeap.opensearch.resource.kafka.builder.JmeCreateTransitDocumentCommandBuilder;
import ch.admin.bit.jeap.opensearch.resource.kafka.builder.JmeTransitDecisionCreatedEventBuilder;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV1;
import ch.admin.bit.jme.opensearch.index.jme.transitdecision.JmeTransitDecisionDataV2;
import ch.admin.bit.jme.opensearch.index.jme.transitdocument.JmeTransitDocumentDataV1;
import ch.admin.bit.jme.transit.JmeCreateTransitDocumentCommand;
import ch.admin.bit.jme.transit.JmeTransitDecisionCreatedEvent;
import ch.admin.bit.jme.transit.JmeTransitDecisionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
@Slf4j
public class DomainMessagePublisher implements MessagePublisher {

    private final KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate;
    private final TopicConfiguration topicConfiguration;
    private final Environment environment;

    @Override
    public void createTransitDocument(JmeTransitDocumentDataV1 transitDocument) {
        JmeCreateTransitDocumentCommand command = JmeCreateTransitDocumentCommandBuilder.createForProcessId(
                        UUID.randomUUID().toString(), serviceName())
                .idempotenceId(UUID.randomUUID().toString())
                .transitDocumentId(transitDocument.transitDocumentId())
                .movementReferenceNumber(transitDocument.movementReferenceNumber())
                .declarantId(transitDocument.declarantId())
                .departureOffice(transitDocument.departureOffice())
                .destinationOffice(transitDocument.destinationOffice())
                .goodsDescription(transitDocument.goodsDescription())
                .remarks(transitDocument.remarks())
                .build();
        send(command, topicConfiguration.getCreateTransitDocument());
    }

    @Override
    public void transitDecisionCreated(JmeTransitDecisionDataV1 transitDecision) {
        JmeTransitDecisionCreatedEvent event = JmeTransitDecisionCreatedEventBuilder.createForProcessId(
                        UUID.randomUUID().toString(), serviceName())
                .idempotenceId(UUID.randomUUID().toString())
                .transitDecisionId(transitDecision.transitDecisionId())
                .status(JmeTransitDecisionStatus.valueOf(transitDecision.status()))
                .remarks(transitDecision.remarks())
                .decidedBy(transitDecision.decidedBy())
                .decisionDate(transitDecision.decisionDate())
                .transitDocumentReferenceId(transitDecision.transitDocumentId())
                .transitDecisionReferenceId(UUID.randomUUID().toString())
                .build();
        send(event, topicConfiguration.getTransitDecisionCreated());
    }

    @Override
    public void transitDecisionCreatedV2(JmeTransitDecisionDataV2 transitDecision) {
        JmeTransitDecisionCreatedEvent event = JmeTransitDecisionCreatedEventBuilder.createForProcessId(
                        UUID.randomUUID().toString(), serviceName())
                .idempotenceId(UUID.randomUUID().toString())
                .transitDecisionId(transitDecision.transitDecisionIdentifier())
                .status(JmeTransitDecisionStatus.valueOf(transitDecision.status()))
                .remarks(transitDecision.remarks())
                .decidedBy(transitDecision.decidedBy())
                .decisionDate(transitDecision.decisionDate())
                .transitDocumentReferenceId(transitDecision.transitDocumentId())
                .transitDecisionReferenceId(UUID.randomUUID().toString())
                .build();
        send(event, topicConfiguration.getTransitDecisionCreated());
    }


    private void send(AvroMessage message, String topic) {
        log.debug("Publishing message '{}' to topic '{}'.", message, topic);
        try {
            kafkaTemplate.send(topic, message).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw MessagePublishingException.publishingInterrupted(message, topic, e);
        } catch (ExecutionException e) {
            throw MessagePublishingException.publishingFailed(message, topic, e);
        }
    }

    private String serviceName() {
        return environment.getRequiredProperty("spring.application.name");
    }
}
