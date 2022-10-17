package com.solace.maas.ep.runtime.agent.plugin.kafka.manager.client;

import com.solace.maas.ep.runtime.agent.plugin.manager.client.MessagingServiceClientManager;
import com.solace.maas.ep.runtime.agent.plugin.messagingService.event.ConnectionDetailsEvent;
import lombok.Data;
import org.apache.kafka.clients.admin.AdminClient;

import java.util.Properties;

/**
 * The KafkaClientManagerImpl class is a temporary placeholder being used to create a Kafka AdminClient.
 * This class will be extended in the future to add more functionality.
 */
@Data
public class KafkaClientManagerImpl implements MessagingServiceClientManager<AdminClient> {

    @Override
    public AdminClient getClient(ConnectionDetailsEvent connectionDetailsEvent) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", connectionDetailsEvent.getConnectionUrl());
        properties.put("connections.max.idle.ms", 10_000);
        properties.put("request.timeout.ms", 5000);

        return AdminClient.create(properties);
    }
}
