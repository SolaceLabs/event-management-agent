package com.solace.maas.ep.runtime.agent.plugin.config;

import com.solace.maas.ep.runtime.agent.plugin.jacoco.ExcludeFromJacocoGeneratedReport;
import com.solace.maas.ep.runtime.agent.plugin.manager.client.MessagingServiceClientManager;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.util.Map;

@ExcludeFromJacocoGeneratedReport
public class MessagingServiceTypeConfig {
    private final static Map<String, MessagingServiceClientManager<?>> messagingServiceManagers =
            new LinkedCaseInsensitiveMap<>();

    public static void addMessagingServiceManager(String serviceName, MessagingServiceClientManager<?> clientManager) {
        messagingServiceManagers.put(serviceName, clientManager);
    }

    public static Map<String, MessagingServiceClientManager<?>> getMessagingServiceManagers() {
        return messagingServiceManagers;
    }
}
