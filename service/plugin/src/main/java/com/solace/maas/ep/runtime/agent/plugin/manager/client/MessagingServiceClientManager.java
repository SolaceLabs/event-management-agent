package com.solace.maas.ep.runtime.agent.plugin.manager.client;

import com.solace.maas.ep.runtime.agent.plugin.messagingService.event.ConnectionDetailsEvent;

public interface MessagingServiceClientManager<T> {
        T getClient(ConnectionDetailsEvent connectionDetailsEvent);
}
