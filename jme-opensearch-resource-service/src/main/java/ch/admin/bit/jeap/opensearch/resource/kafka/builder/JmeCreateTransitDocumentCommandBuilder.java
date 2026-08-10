package ch.admin.bit.jeap.opensearch.resource.kafka.builder;

import ch.admin.bit.jeap.command.avro.AvroCommandBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jme.transit.CreateTransitDocumentPayload;
import ch.admin.bit.jme.transit.CreateTransitDocumentReferences;
import ch.admin.bit.jme.transit.JmeCreateTransitDocumentCommand;

import static ch.admin.bit.jeap.opensearch.resource.kafka.builder.BuilderConstants.SYSTEM_NAME;

public class JmeCreateTransitDocumentCommandBuilder extends AvroCommandBuilder<JmeCreateTransitDocumentCommandBuilder, JmeCreateTransitDocumentCommand> {

    private String transitDocumentId;
    private String movementReferenceNumber;
    private String declarantId;
    private String departureOffice;
    private String destinationOffice;
    private String goodsDescription;
    private String remarks;
    private final String serviceName;

    private JmeCreateTransitDocumentCommandBuilder(String serviceName) {
        super(JmeCreateTransitDocumentCommand::new);
        this.serviceName = serviceName;
    }

    public static JmeCreateTransitDocumentCommandBuilder createForProcessId(String processId, String serviceName) {
        JmeCreateTransitDocumentCommandBuilder builder = new JmeCreateTransitDocumentCommandBuilder(serviceName);
        builder.setProcessId(processId);
        return builder;
    }

    @Override
    protected final String getServiceName() {
        return serviceName;
    }

    @Override
    protected final String getSystemName() {
        return SYSTEM_NAME;
    }

    // Define the additional builder methods.
    public JmeCreateTransitDocumentCommandBuilder transitDocumentId(String transitDocumentId) {
        this.transitDocumentId = transitDocumentId;
        return this;
    }

    public JmeCreateTransitDocumentCommandBuilder movementReferenceNumber(String movementReferenceNumber) {
        this.movementReferenceNumber = movementReferenceNumber;
        return this;
    }

    public JmeCreateTransitDocumentCommandBuilder declarantId(String declarantId) {
        this.declarantId = declarantId;
        return this;
    }

    public JmeCreateTransitDocumentCommandBuilder departureOffice(String departureOffice) {
        this.departureOffice = departureOffice;
        return this;
    }

    public JmeCreateTransitDocumentCommandBuilder destinationOffice(String destinationOffice) {
        this.destinationOffice = destinationOffice;
        return this;
    }

    public JmeCreateTransitDocumentCommandBuilder goodsDescription(String goodsDescription) {
        this.goodsDescription = goodsDescription;
        return this;
    }

    public JmeCreateTransitDocumentCommandBuilder remarks(String remarks) {
        this.remarks = remarks;
        return this;
    }

    @Override
    protected JmeCreateTransitDocumentCommandBuilder self() {
        return this;
    }

    @Override
    public JmeCreateTransitDocumentCommand build() {
        if (this.transitDocumentId == null) {
            throw AvroMessageBuilderException.propertyNull("JmeCreateTransitDocumentCommand.transitDocumentId");
        }
        if (this.movementReferenceNumber == null) {
            throw AvroMessageBuilderException.propertyNull("JmeCreateTransitDocumentCommand.movementReferenceNumber");
        }
        if (this.declarantId == null) {
            throw AvroMessageBuilderException.propertyNull("JmeCreateTransitDocumentCommand.declarantId");
        }
        if (this.departureOffice == null) {
            throw AvroMessageBuilderException.propertyNull("JmeCreateTransitDocumentCommand.departureOffice");
        }
        if (this.destinationOffice == null) {
            throw AvroMessageBuilderException.propertyNull("JmeCreateTransitDocumentCommand.destinationOffice");
        }
        if (this.goodsDescription == null) {
            throw AvroMessageBuilderException.propertyNull("JmeCreateTransitDocumentCommand.goodsDescription");
        }
        if (this.remarks == null) {
            throw AvroMessageBuilderException.propertyNull("JmeCreateTransitDocumentCommand.remarks");
        }

        if (this.idempotenceId == null) {
            throw AvroMessageBuilderException.propertyNull("JmeCreateTransitDocumentCommand.idempotenceId");
        }
        CreateTransitDocumentReferences references = CreateTransitDocumentReferences.newBuilder().build();
        CreateTransitDocumentPayload payload = CreateTransitDocumentPayload.newBuilder()
                .setTransitDocumentId(transitDocumentId)
                .setMovementReferenceNumber(movementReferenceNumber)
                .setDeclarantId(declarantId)
                .setDepartureOffice(departureOffice)
                .setDestinationOffice(destinationOffice)
                .setGoodsDescription(goodsDescription)
                .setRemarks(remarks)
                .build();
        setReferences(references);
        setPayload(payload);
        return super.build();
    }
}
