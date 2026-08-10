package ch.admin.bit.jeap.opensearch.resource;

import ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContracts;
import ch.admin.bit.jme.transit.JmeCreateTransitDocumentCommand;
import ch.admin.bit.jme.transit.JmeTransitDecisionCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
@JeapMessageProducerContracts({
        JmeTransitDecisionCreatedEvent.TypeRef.class,
        JmeCreateTransitDocumentCommand.TypeRef.class
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
