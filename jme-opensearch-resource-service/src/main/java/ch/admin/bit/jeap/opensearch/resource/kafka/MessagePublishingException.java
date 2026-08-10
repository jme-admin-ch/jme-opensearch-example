package ch.admin.bit.jeap.opensearch.resource.kafka;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;

final class MessagePublishingException extends RuntimeException {

    private MessagePublishingException(String message, Throwable cause) {
        super(message, cause);
    }

    static MessagePublishingException publishingFailed(AvroMessage message, String topic, Throwable cause) {
        String err = String.format("Publishing of message '%s' with id '%s' to topic '%s' failed.",
                message.getType().getName(), message.getIdentity().getId(), topic);
        return new MessagePublishingException(err, cause);
    }

    static MessagePublishingException publishingInterrupted(AvroMessage message, String topic, Throwable cause) {
        String err = String.format("Publishing of message '%s' with id '%s' to topic '%s' has been interrupted.",
                message.getType().getName(), message.getIdentity().getId(), topic);
        return new MessagePublishingException(err, cause);
    }
}