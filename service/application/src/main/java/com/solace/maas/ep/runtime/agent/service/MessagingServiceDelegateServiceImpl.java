package com.solace.maas.ep.runtime.agent.service;

import com.solace.maas.ep.runtime.agent.event.MessagingServiceEvent;
import com.solace.maas.ep.runtime.agent.plugin.config.enumeration.MessagingServiceType;
import com.solace.maas.ep.runtime.agent.plugin.manager.client.MessagingServiceClientManager;
import com.solace.maas.ep.runtime.agent.plugin.messagingService.event.AuthenticationDetailsEvent;
import com.solace.maas.ep.runtime.agent.plugin.messagingService.event.ConnectionDetailsEvent;
import com.solace.maas.ep.runtime.agent.plugin.service.MessagingServiceDelegateService;
import com.solace.maas.ep.runtime.agent.repository.messagingservice.MessagingServiceRepository;
import com.solace.maas.ep.runtime.agent.repository.model.mesagingservice.AuthenticationDetailsEntity;
import com.solace.maas.ep.runtime.agent.repository.model.mesagingservice.ConnectionDetailsEntity;
import com.solace.maas.ep.runtime.agent.repository.model.mesagingservice.MessagingServiceEntity;
import com.solace.maas.ep.runtime.agent.service.encryption.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.encoder.org.apache.commons.lang3.ArrayUtils;
import net.logstash.logback.encoder.org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.transaction.Transactional;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
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

    private final Map<String, MessagingServiceClientManager<?>> messagingServiceManagers;

    private final Map<String, Object> messagingServiceClients;

    private final EncryptionService encryptionService;

    @Autowired
    public MessagingServiceDelegateServiceImpl(MessagingServiceRepository repository,
                                               Map<String, MessagingServiceClientManager<?>> messagingServiceManagers,
                                               EncryptionService encryptionService) {
        this.repository = repository;
        this.messagingServiceManagers = messagingServiceManagers;
        this.encryptionService = encryptionService;
        messagingServiceClients = new HashMap<>();
    }

    /**
     * Adds a Messaging Service. Right now the only information we're storing is the Connection Details for
     * a Messaging Service. Later on there will be more information.
     *
     * @param messagingServiceEvent Messaging Service Details for a Messaging Service.
     */
    @Transactional
    public MessagingServiceEntity addMessagingService(MessagingServiceEvent messagingServiceEvent)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        List<ConnectionDetailsEntity> connectionDetailsEntities = new ArrayList<>();
        this.encryptionService.parseAndInitializeKeys();

        MessagingServiceEntity messagingServiceEntity = MessagingServiceEntity.builder()
                .id(messagingServiceEvent.getId())
                .messagingServiceType(messagingServiceEvent.getMessagingServiceType())
                .name(messagingServiceEvent.getName())
                .build();

        messagingServiceEvent.getConnectionDetails()
                .forEach(connectionDetailsEvent -> {

                    List<AuthenticationDetailsEvent> authenticationDetailsEvents =
                            connectionDetailsEvent.getAuthenticationDetails();

                    ConnectionDetailsEntity connectionDetailsEntity = ConnectionDetailsEntity.builder()
                            .name(connectionDetailsEvent.getName())
                            .connectionUrl(connectionDetailsEvent.getConnectionUrl())
                            .msgVpn(connectionDetailsEvent.getMsgVpn())
                            .messagingService(messagingServiceEntity)
                            .build();

                    List<AuthenticationDetailsEntity> authenticationDetailsEntities =
                            authenticationDetailsEvents
                                    .stream()
                                    .map(authenticationDetailsEvent -> {
                                        AuthenticationDetailsEntity authenticationDetailsEntity =
                                                AuthenticationDetailsEntity.builder()
                                                        .username(authenticationDetailsEvent.getUsername())
                                                        .connectionDetails(connectionDetailsEntity)
                                                        .build();

                                        if (!StringUtils.isEmpty(authenticationDetailsEvent.getPassword())) {
                                            try {
                                                byte[] encryptedPassword = encryptionService.encrypt(authenticationDetailsEvent.getPassword());
                                                if (!ArrayUtils.isEmpty(encryptedPassword)) {
                                                    authenticationDetailsEvent.setEncryptedPassword(encryptedPassword);
                                                    authenticationDetailsEntity.setPassword(ArrayUtils.toObject(encryptedPassword));
                                                }
                                            } catch (NoSuchPaddingException | NoSuchAlgorithmException |
                                                    InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        return authenticationDetailsEntity;
                                    })
                                    .collect(Collectors.toUnmodifiableList());

                    connectionDetailsEntity.setAuthenticationDetails(authenticationDetailsEntities);

                    connectionDetailsEntities.add(connectionDetailsEntity);
                });

        messagingServiceEntity.setManagementDetails(connectionDetailsEntities);

        return repository.save(messagingServiceEntity);
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
    public <T> T getMessagingServiceClient(String messagingServiceId)
            throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException,
            BadPaddingException, InvalidKeyException {

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
                .build();

        if (authenticationDetailsEntity.getPassword() != null) {
            String decryptedPassword = encryptionService.decrypt(ArrayUtils.toPrimitive(authenticationDetailsEntity.getPassword()));
            authenticationDetailsEvent.setPassword(decryptedPassword);
        }

        ConnectionDetailsEvent connectionDetailsEvent = ConnectionDetailsEvent.builder()
                .messagingServiceId(messagingServiceId)
                .name(connectionDetailsEntity.getName())
                .connectionUrl(connectionDetailsEntity.getConnectionUrl())
                .msgVpn(connectionDetailsEntity.getMsgVpn())
                .authenticationDetails(List.of(authenticationDetailsEvent))
                .build();

        // Get the Messaging Service type.
        MessagingServiceType type = messagingServiceEntity.getMessagingServiceType();

        if (messagingServiceClients.containsKey(messagingServiceId)) {
            return (T) messagingServiceClients.get(messagingServiceId);
        } else if (messagingServiceManagers.containsKey(type.name())) {
            // Attempt to retrieve the Messaging Service Manager for this type of Messaging Service. If it is found,
            // we will attempt to create a Connection Client.
            MessagingServiceClientManager<?> clientManager = messagingServiceManagers.get(type.name());

            T messagingServiceClient = (T) clientManager.getClient(connectionDetailsEvent);
            messagingServiceClients.put(messagingServiceId, messagingServiceClient);

            return messagingServiceClient;
        } else {
            throw new RuntimeException("Placeholder Error.");
        }
    }
}
