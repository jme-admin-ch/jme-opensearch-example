package ch.admin.bit.jeap.opensearch.resource.kafka.builder;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jme.transit.*;

import java.time.Instant;

import static ch.admin.bit.jeap.opensearch.resource.kafka.builder.BuilderConstants.SYSTEM_NAME;

public class JmeTransitDecisionCreatedEventBuilder extends AvroDomainEventBuilder<JmeTransitDecisionCreatedEventBuilder, JmeTransitDecisionCreatedEvent> {

    private String transitDecisionId;
    private JmeTransitDecisionStatus status;
    private String remarks;
    private String decidedBy;
    private Instant decisionDate;

    private String transitDecisionReferenceId;
    private String transitDocumentReferenceId;
    private final String serviceName;


    protected JmeTransitDecisionCreatedEventBuilder(String serviceName) {
        super(JmeTransitDecisionCreatedEvent::new);
        this.serviceName = serviceName;
    }

    public static JmeTransitDecisionCreatedEventBuilder createForProcessId(String processId, String serviceName) {
        JmeTransitDecisionCreatedEventBuilder builder = new JmeTransitDecisionCreatedEventBuilder(serviceName);
        builder.setProcessId(processId);
        return builder;
    }

    @Override
    protected String getServiceName() {
        return serviceName;
    }

    @Override
    protected String getSystemName() {
        return SYSTEM_NAME;
    }

    // Define the additional builder methods.
    public JmeTransitDecisionCreatedEventBuilder transitDecisionId(String transitDecisionId) {
        this.transitDecisionId = transitDecisionId;
        return this;
    }

    public JmeTransitDecisionCreatedEventBuilder status(JmeTransitDecisionStatus status) {
        this.status = status;
        return this;
    }

    public JmeTransitDecisionCreatedEventBuilder remarks(String remarks) {
        this.remarks = remarks;
        return this;
    }

    public JmeTransitDecisionCreatedEventBuilder decidedBy(String decidedBy) {
        this.decidedBy = decidedBy;
        return this;
    }

    public JmeTransitDecisionCreatedEventBuilder decisionDate(Instant decisionDate) {
        this.decisionDate = decisionDate;
        return this;
    }

    public JmeTransitDecisionCreatedEventBuilder transitDecisionReferenceId(String transitDecisionReferenceId) {
        this.transitDecisionReferenceId = transitDecisionReferenceId;
        return this;
    }

    public JmeTransitDecisionCreatedEventBuilder transitDocumentReferenceId(String transitDocumentReferenceId) {
        this.transitDocumentReferenceId = transitDocumentReferenceId;
        return this;
    }

    @Override
    public JmeTransitDecisionCreatedEvent build() {
        if (this.transitDecisionId == null) {
            throw AvroMessageBuilderException.propertyNull("JmeTransitDecisionCreatedEvent.transitDecisionId");
        }
        if (this.status == null) {
            throw AvroMessageBuilderException.propertyNull("JmeTransitDecisionCreatedEvent.status");
        }
        if (this.remarks == null) {
            throw AvroMessageBuilderException.propertyNull("JmeTransitDecisionCreatedEvent.remarks");
        }
        if (this.decidedBy == null) {
            throw AvroMessageBuilderException.propertyNull("JmeTransitDecisionCreatedEvent.decidedBy");
        }
        if (this.decisionDate == null) {
            throw AvroMessageBuilderException.propertyNull("JmeTransitDecisionCreatedEvent.decisionDate");
        }
        if (this.transitDecisionReferenceId == null) {
            throw AvroMessageBuilderException.propertyNull("JmeTransitDecisionCreatedEvent.transitDecisionReferenceId");
        }
        if (this.transitDocumentReferenceId == null) {
            throw AvroMessageBuilderException.propertyNull("JmeTransitDecisionCreatedEvent.transitDocumentReferenceId");
        }

        if (this.idempotenceId == null) {
            throw AvroMessageBuilderException.propertyNull("JmeTransitDecisionCreatedEvent.idempotenceId");
        }
        JmeTransitDecisionCreatedEventReferences references = JmeTransitDecisionCreatedEventReferences.newBuilder()
                .setNewTransitDecision(JmeTransitDecisionReference.newBuilder()
                        .setId(transitDecisionReferenceId)
                        .build())
                .setTransitDocument(JmeTransitDocumentReference.newBuilder()
                        .setId(transitDocumentReferenceId)
                        .build())
                .build();

        JmeTransitDecisionCreatedEventPayload payload = JmeTransitDecisionCreatedEventPayload.newBuilder()
                .setTransitDecisionId(transitDecisionId)
                .setStatus(status)
                .setRemarks(remarks)
                .setDecidedBy(decidedBy)
                .setDecisionDate(decisionDate)
                .build();
        setReferences(references);
        setPayload(payload);
        return super.build();
    }


    @Override
    protected JmeTransitDecisionCreatedEventBuilder self() {
        return this;
    }
}
