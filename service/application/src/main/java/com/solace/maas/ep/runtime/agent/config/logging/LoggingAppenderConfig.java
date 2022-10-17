package com.solace.maas.ep.runtime.agent.config.logging;

import ch.qos.logback.classic.Logger;
import com.solace.maas.ep.runtime.agent.config.eventPortal.EventPortalProperties;
import com.solace.maas.ep.runtime.agent.logging.StreamingAppender;
import com.solace.maas.ep.runtime.agent.plugin.jacoco.ExcludeFromJacocoGeneratedReport;
import org.apache.camel.ProducerTemplate;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Configuration
@ExcludeFromJacocoGeneratedReport
public class LoggingAppenderConfig {
    private final StreamingAppender streamingAppender;

    private final ProducerTemplate producerTemplate;

    private final EventPortalProperties eventPortalProperties;

    public LoggingAppenderConfig(ProducerTemplate producerTemplate, EventPortalProperties eventPortalProperties) {
        this.eventPortalProperties = eventPortalProperties;
        this.producerTemplate = producerTemplate;

        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        streamingAppender = (StreamingAppender) logger.getAppender("StreamingAppender");
    }

    @Bean
    public StreamingAppender configuredStreamingAppender() {
        if(Objects.nonNull(streamingAppender)) {
            streamingAppender.setProducerTemplate(producerTemplate);
            streamingAppender.setStandalone(eventPortalProperties.getGateway().getMessaging().isStandalone());
        }

        return streamingAppender;
    }
}
