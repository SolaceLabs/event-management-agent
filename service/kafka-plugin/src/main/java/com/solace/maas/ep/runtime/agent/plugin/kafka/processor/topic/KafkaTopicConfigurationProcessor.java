package com.solace.maas.ep.runtime.agent.plugin.kafka.processor.topic;

import com.solace.maas.ep.runtime.agent.plugin.constants.RouteConstants;
import com.solace.maas.ep.runtime.agent.plugin.jacoco.ExcludeFromJacocoGeneratedReport;
import com.solace.maas.ep.runtime.agent.plugin.kafka.processor.event.general.KafkaAclEvent;
import com.solace.maas.ep.runtime.agent.plugin.kafka.processor.event.general.KafkaNodeEvent;
import com.solace.maas.ep.runtime.agent.plugin.kafka.processor.event.topic.KafkaTopicConfigurationEvent;
import com.solace.maas.ep.runtime.agent.plugin.kafka.processor.event.topic.KafkaTopicEvent;
import com.solace.maas.ep.runtime.agent.plugin.kafka.processor.event.topic.KafkaTopicPartitionEvent;
import com.solace.maas.ep.runtime.agent.plugin.processor.base.ResultProcessorImpl;
import com.solace.maas.ep.runtime.agent.plugin.service.MessagingServiceDelegateService;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.acl.AclOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@ExcludeFromJacocoGeneratedReport
@Component
@SuppressWarnings("PMD")
public class KafkaTopicConfigurationProcessor extends ResultProcessorImpl<List<KafkaTopicConfigurationEvent>, List<KafkaTopicEvent>> {
    private final MessagingServiceDelegateService messagingServiceDelegateService;

    @Autowired
    public KafkaTopicConfigurationProcessor(MessagingServiceDelegateService messagingServiceDelegateService) {
        super();
        this.messagingServiceDelegateService = messagingServiceDelegateService;
    }

    private List<KafkaTopicPartitionEvent> mapPartition(List<TopicPartitionInfo> partitions) {
        return partitions
                .stream()
                .map(partition -> {
                    KafkaTopicPartitionEvent partitionEvent = KafkaTopicPartitionEvent.builder()
                            .isr(partition.isr()
                                    .stream().map(isr -> KafkaNodeEvent.builder()
                                            .id(isr.id())
                                            .host(isr.host())
                                            .port(isr.port())
                                            .rack(isr.rack())
                                            .build()
                                    )
                                    .sorted(Comparator.comparing(KafkaNodeEvent::getId))
                                    .collect(Collectors.toUnmodifiableList())
                            ).replicas(partition.replicas()
                                    .stream().map(replicas -> KafkaNodeEvent.builder()
                                            .id(replicas.id())
                                            .host(replicas.host())
                                            .port(replicas.port())
                                            .rack(replicas.rack())
                                            .build()
                                    )
                                    .sorted(Comparator.comparing(KafkaNodeEvent::getId))
                                    .collect(Collectors.toUnmodifiableList())
                            ).partition(partition.partition())
                            .build();

                    if (Objects.nonNull(partition.leader())) {
                        partitionEvent.setLeader(KafkaNodeEvent.builder()
                                .id(partition.leader().id())
                                .host(partition.leader().host())
                                .port(partition.leader().port())
                                .rack(partition.leader().rack())
                                .build());
                    }

                    return partitionEvent;
                })
                .sorted(Comparator.comparing(KafkaTopicPartitionEvent::getPartition))
                .collect(Collectors.toUnmodifiableList());
    }

    private void mapAcls(Set<AclOperation> aclOperations, List<KafkaAclEvent> acls) {
        List<KafkaAclEvent> mappedAcls = aclOperations
                .stream()
                .map(acl -> KafkaAclEvent.builder()
                        .name(acl.name())
                        .unknown(acl.isUnknown())
                        .ordinal(acl.ordinal())
                        .build())
                .sorted()
                .collect(Collectors.toUnmodifiableList());

        acls.addAll(mappedAcls);
    }

    @Override
    public List<KafkaTopicConfigurationEvent> handleEvent(Map<String, Object> properties, List<KafkaTopicEvent> body) throws Exception {
        String messagingServiceId = (String) properties.get(RouteConstants.MESSAGING_SERVICE_ID);

        AdminClient adminClient = messagingServiceDelegateService.getMessagingServiceClient(messagingServiceId);

        if (!body.isEmpty()) {
            List<String> topicNames = body.stream()
                    .map(KafkaTopicEvent::getName)
                    .collect(Collectors.toUnmodifiableList());

            if (!topicNames.isEmpty()) {
                DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(topicNames);
                List<KafkaTopicConfigurationEvent> topicConfigDetails = describeTopicsResult.all()
                        .get(30, TimeUnit.SECONDS)
                        .values()
                        .stream()
                        .map(result -> {
                            List<KafkaTopicPartitionEvent> topicPartitions = mapPartition(result.partitions());

                            List<KafkaAclEvent> acls = new ArrayList<>();

                            if (Objects.nonNull(result.authorizedOperations())) {
                                mapAcls(result.authorizedOperations(), acls);
                            }

                            return KafkaTopicConfigurationEvent.builder()
                                    .name(result.name())
                                    .internal(result.isInternal())
                                    .topicId(result.topicId().toString())
                                    .partitions(topicPartitions)
                                    .acls(acls)
                                    .build();
                        })
                        .collect(Collectors.toUnmodifiableList());

                return topicConfigDetails;
            } else {
                return List.of();
            }
        } else {
            return List.of();
        }
    }
}
