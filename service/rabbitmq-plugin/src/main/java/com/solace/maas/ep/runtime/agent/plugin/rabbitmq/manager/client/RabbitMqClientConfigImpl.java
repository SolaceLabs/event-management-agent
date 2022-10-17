package com.solace.maas.ep.runtime.agent.plugin.rabbitmq.manager.client;

import com.solace.maas.ep.runtime.agent.plugin.manager.client.MessagingServiceClientConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqClientConfigImpl extends MessagingServiceClientConfig {
    protected RabbitMqClientConfigImpl() {
        super("RABBITMQ", new RabbitMqClientManagerImpl());
    }
}
