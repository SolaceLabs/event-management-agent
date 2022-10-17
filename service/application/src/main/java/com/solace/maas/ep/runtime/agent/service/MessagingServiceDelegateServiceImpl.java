package com.solace.maas.ep.runtime.agent.service;

import com.solace.maas.ep.runtime.agent.event.MessagingServiceEvent;
import com.solace.maas.ep.runtime.agent.plugin.config.MessagingServiceTypeConfig;
import com.solace.maas.ep.runtime.agent.plugin.manager.client.MessagingServiceClientManager;
import com.solace.maas.ep.runtime.agent.plugin.messagingService.event.AuthenticationDetailsEvent;
import com.solace.maas.ep.runtime.agent.plugin.messagingService.event.ConnectionDetailsEvent;
import com.solace.maas.ep.runtime.agent.plugin.service.MessagingServiceDelegateService;
import com.solace.maas.ep.runtime.agent.repository.messagingservice.MessagingServiceRepository;
import com.solace.maas.ep.runtime.agent.repository.model.mesagingservice.AuthenticationDetailsEntity;
import com.solace.maas.ep.runtime.agent.repository.model.mesagingservice.ConnectionDetailsEntity;
import com.solace.maas.ep.runtime.agent.repository.model.mesagingservice.MessagingServiceEntity;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.encoder.org.apache.commons.lang3.ArrayUtils;
import net.logstash.logback.encoder.org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Manages the creation and retrieval of Messaging Service information.
 */
@Slf4j
@Service
public class MessagingServiceDelegateServiceImpl implements MessagingServiceDelegateService {
    private final MessagingServiceRepository repository;

    private final Map<String, Object> messagingServiceClients;

    @Autowired
    public MessagingServiceDelegateServiceImpl(MessagingServiceRepository repository) {
        this.repository = repository;
        messagingServiceClients = new HashMap<>();
    }

    /**
     * Adds a Messaging Service. Right now the only information we're storing is the Connection Details for
     * a Messaging Service. Later on there will be more information.
     *
     * @param messagingServiceEvent Messaging Service Details for a Messaging Service.
     */
    @Transactional
    public MessagingServiceEntity addMessagingService(MessagingServiceEvent messagingServiceEvent) {
        MessagingServiceEntity messagingServiceEntity = createMessagingServiceEntity(messagingServiceEvent);

        return repository.save(messagingServiceEntity);
    }

    @Transactional
    public Iterable<MessagingServiceEntity> addMessagingServices(List<MessagingServiceEvent> messagingServiceEvents) {
        List<MessagingServiceEntity> messagingServiceEntities = messagingServiceEvents.stream()
                .map(this::createMessagingServiceEntity)
                .collect(Collectors.toUnmodifiableList());

        return repository.saveAll(messagingServiceEntities);
    }

    /**
     * Retrieves a specific Messaging Service by ID.
     *
     * @param messagingServiceId The Messaging Service ID.
     * @return The retrieved Messaging Service.
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public MessagingServiceEntity getMessagingServiceById(String messagingServiceId) {
        Optional<MessagingServiceEntity> messagingServiceEntityOpt = repository.findById(messagingServiceId);
        return messagingServiceEntityOpt.orElseThrow(() ->
                new NoSuchElementException(String.format("Could not find messaging service with id %s", messagingServiceId)));
    }

    /**
     * Retrieves a Client Connection for a specific Messaging Service. This will use the stored Connection Details
     * to create the Client Connection.
     *
     * @param messagingServiceId The Messaging Service ID.
     * @param <T>                The Type of the Connection Client being created.
     * @return The created Connection Client.
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public <T> T getMessagingServiceClient(String messagingServiceId) {
        MessagingServiceEntity messagingServiceEntity = getMessagingServiceById(messagingServiceId);

        ConnectionDetailsEntity connectionDetailsEntity = messagingServiceEntity
                .getManagementDetails()
                .stream()
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("Could not find connection details for service with id %s", messagingServiceId)));

        AuthenticationDetailsEntity authenticationDetailsEntity =
                connectionDetailsEntity.getAuthenticationDetails()
                        .stream()
                        .findFirst().orElseThrow(() -> new NoSuchElementException(
                                String.format("Could not find authentication details for service with id %s",
                                        connectionDetailsEntity.getMessagingService().getId())));

        AuthenticationDetailsEvent authenticationDetailsEvent = AuthenticationDetailsEvent.builder()
                .id(authenticationDetailsEntity.getId())
                .username(authenticationDetailsEntity.getUsername())
                .password(authenticationDetailsEntity.getPassword() == null ? null :
                        new String(ArrayUtils.toPrimitive(authenticationDetailsEntity.getPassword()), StandardCharsets.UTF_8))
                .build();

        ConnectionDetailsEvent connectionDetailsEvent = ConnectionDetailsEvent.builder()
                .messagingServiceId(messagingServiceId)
                .name(connectionDetailsEntity.getName())
                .connectionUrl(connectionDetailsEntity.getConnectionUrl())
                .msgVpn(connectionDetailsEntity.getMsgVpn())
                .authenticationDetails(List.of(authenticationDetailsEvent))
                .build();

        // Get the Messaging Service type.
        String type = messagingServiceEntity.getMessagingServiceType();

        if (messagingServiceClients.containsKey(messagingServiceId)) {
            return (T) messagingServiceClients.get(messagingServiceId);
        } else if (MessagingServiceTypeConfig.getMessagingServiceManagers().containsKey(type)) {
            // Attempt to retrieve the Messaging Service Manager for this type of Messaging Service. If it is found,
            // we will attempt to create a Connection Client.
            MessagingServiceClientManager<?> clientManager =
                    MessagingServiceTypeConfig.getMessagingServiceManagers().get(type);

            T messagingServiceClient = (T) clientManager.getClient(connectionDetailsEvent);
            messagingServiceClients.put(messagingServiceId, messagingServiceClient);

            return messagingServiceClient;
        } else {
            throw new RuntimeException("Placeholder Error.");
        }
    }

    private MessagingServiceEntity createMessagingServiceEntity(MessagingServiceEvent messagingServiceEvent) {
        MessagingServiceEntity messagingServiceEntity = MessagingServiceEntity.builder()
                .id(messagingServiceEvent.getId())
                .messagingServiceType(messagingServiceEvent.getMessagingServiceType())
                .name(messagingServiceEvent.getName())
                .build();

        List<ConnectionDetailsEntity> connectionDetailsEntities = messagingServiceEvent.getConnectionDetails()
                .stream()
                .map(connectionDetailsEvent -> {
                    ConnectionDetailsEntity connectionDetailsEntity = ConnectionDetailsEntity.builder()
                            .name(connectionDetailsEvent.getName())
                            .connectionUrl(connectionDetailsEvent.getConnectionUrl())
                            .msgVpn(connectionDetailsEvent.getMsgVpn())
                            .messagingService(messagingServiceEntity)
                            .build();

                    List<AuthenticationDetailsEntity> authenticationDetailsEntities =
                            connectionDetailsEvent.getAuthenticationDetails()
                                    .stream()
                                    .map(authenticationDetailsEvent -> {
                                        AuthenticationDetailsEntity authenticationDetailsEntity =
                                                AuthenticationDetailsEntity.builder()
                                                        .username(authenticationDetailsEvent.getUsername())
                                                        .connectionDetails(connectionDetailsEntity)
                                                        .build();

                                        if (!StringUtils.isEmpty(authenticationDetailsEvent.getPassword())) {
                                            authenticationDetailsEntity.setPassword((
                                                    ArrayUtils.toObject(authenticationDetailsEvent.getPassword()
                                                            .getBytes(StandardCharsets.UTF_8))));
                                        }

                                        return authenticationDetailsEntity;
                                    })
                                    .collect(Collectors.toUnmodifiableList());

                    connectionDetailsEntity.setAuthenticationDetails(authenticationDetailsEntities);

                    return connectionDetailsEntity;
                }).collect(Collectors.toUnmodifiableList());

        messagingServiceEntity.setManagementDetails(connectionDetailsEntities);

        return messagingServiceEntity;
    }
}
