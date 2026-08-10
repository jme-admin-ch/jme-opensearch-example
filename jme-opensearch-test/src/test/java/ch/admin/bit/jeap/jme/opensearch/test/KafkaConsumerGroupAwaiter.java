package ch.admin.bit.jeap.jme.opensearch.test;

import ch.admin.bit.jeap.jme.test.BootServiceIntegrationTestBase;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.common.config.SaslConfigs;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
final class KafkaConsumerGroupAwaiter {

    private KafkaConsumerGroupAwaiter() {
    }

    static void waitForAssignment(String groupId, String... topics) {
        Map<String, Object> adminClientConfig = BootServiceIntegrationTestBase.TestProfileResolver.isCI()
                ? Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "broker:29092")
                : Map.of(
                        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:12000",
                        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT",
                        SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-512",
                        SaslConfigs.SASL_JAAS_CONFIG,
                        "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"broker-user\" password=\"broker-secret\";");

        List<String> expectedTopics = Arrays.asList(topics);
        try (AdminClient adminClient = AdminClient.create(adminClientConfig)) {
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                List<String> assignedTopics;
                try {
                    ConsumerGroupDescription description = adminClient.describeConsumerGroups(List.of(groupId))
                            .describedGroups().get(groupId).get();
                    assignedTopics = description.members().stream()
                            .flatMap(member -> member.assignment().topicPartitions().stream())
                            .map(topicPartition -> topicPartition.topic())
                            .distinct()
                            .toList();
                } catch (Exception e) {
                    log.warn("Failed to describe consumer group {} while waiting for topics {}", groupId, expectedTopics, e);
                    assignedTopics = List.of();
                }
                assertThat(assignedTopics).containsAll(expectedTopics);
            });
        }
    }
}
