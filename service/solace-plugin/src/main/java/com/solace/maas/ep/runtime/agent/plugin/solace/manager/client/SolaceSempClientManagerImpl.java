package com.solace.maas.ep.runtime.agent.plugin.solace.manager.client;

import com.solace.maas.ep.runtime.agent.plugin.jacoco.ExcludeFromJacocoGeneratedReport;
import com.solace.maas.ep.runtime.agent.plugin.kafka.manager.client.MessagingServiceClientManager;
import com.solace.maas.ep.runtime.agent.plugin.messagingService.event.AuthenticationDetailsEvent;
import com.solace.maas.ep.runtime.agent.plugin.messagingService.event.ConnectionDetailsEvent;
import com.solace.maas.ep.runtime.agent.plugin.solace.processor.semp.SempClient;
import com.solace.maas.ep.runtime.agent.plugin.solace.processor.semp.SolaceHttpSemp;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

@Slf4j
@ExcludeFromJacocoGeneratedReport
@Data
public class SolaceSempClientManagerImpl implements MessagingServiceClientManager<SolaceHttpSemp> {

    public SolaceSempClientManagerImpl() {
    }

    @Override
    public SolaceHttpSemp getClient(ConnectionDetailsEvent connectionDetailsEvent) {
        log.info("Creating Solace SEMP client for messaging service {}.",
                connectionDetailsEvent.getMessagingServiceId());

        AuthenticationDetailsEvent authenticationDetailsEvent =
                connectionDetailsEvent.getAuthenticationDetails()
                        .stream()
                        .findFirst().orElseThrow(() -> new NoSuchElementException(
                                String.format("Could not find authentication details for service with id %s",
                                        connectionDetailsEvent.getMessagingServiceId())));

        SempClient sempClient = SempClient.builder()
                .webClient(WebClient.builder().build())
                .username(authenticationDetailsEvent.getUsername())
                .password(authenticationDetailsEvent.getPassword())
                .msgVpn(connectionDetailsEvent.getMsgVpn())
                .connectionUrl(connectionDetailsEvent.getConnectionUrl())
                .build();

        log.info("Solace SEMP client created for {}.", connectionDetailsEvent.getMessagingServiceId());
        return new SolaceHttpSemp(sempClient);
    }
}
