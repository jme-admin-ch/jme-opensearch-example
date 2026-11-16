package ch.admin.bit.jeap.jme.opensearch.test;

import ch.admin.bit.jeap.opensearch.indexwriter.adapter.opensearch.AdapterOpenSearchConfiguration;
import ch.admin.bit.jeap.opensearch.indexwriter.domain.maintenance.acceptance.JobTargetAcceptance;
import ch.admin.bit.jeap.opensearch.indexwriter.domain.maintenance.acceptance.JobTargetAcceptanceCheck;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;


/**
 * Runs the index writer's maintenance target policy check against the example's OpenSearch, wired as in the index
 * writer. Job creation (JEAP-7794) will call the same check.
 */
final class MaintenancePolicyCheck implements AutoCloseable {

    private final AnnotationConfigApplicationContext context;
    private final JobTargetAcceptanceCheck check;

    MaintenancePolicyCheck(String openSearchUrl) {
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("policy-check",
                Map.of("jeap.opensearch.indexwriter.connection.url", openSearchUrl)));
        context.register(PolicyCheckConfiguration.class);
        context.refresh();
        check = context.getBean(JobTargetAcceptanceCheck.class);
    }

    JobTargetAcceptance check(String targetIndex) {
        return check.check(targetIndex);
    }

    @Override
    public void close() {
        context.close();
    }

    @Configuration(proxyBeanMethods = false)
    @Import({AdapterOpenSearchConfiguration.class, JobTargetAcceptanceCheck.class})
    static class PolicyCheckConfiguration {

        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
