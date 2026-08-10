package ch.admin.bit.jeap.opensearch.resource.kafka;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jme.opensearch.topic")
@Data
@Slf4j
public class TopicConfiguration {

    private String createTransitDocument;
    private String transitDecisionCreated;
}
