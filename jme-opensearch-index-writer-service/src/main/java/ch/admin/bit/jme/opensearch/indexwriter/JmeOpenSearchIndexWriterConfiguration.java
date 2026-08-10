package ch.admin.bit.jme.opensearch.indexwriter;

import ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContractsByTemplates;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan
@JeapMessageConsumerContractsByTemplates
public class JmeOpenSearchIndexWriterConfiguration {
}
