# REST interface

The Event Management Agent includes a REST API that allows the user to initiate a scan.

Each plugin requires its own custom set of authentication and identification attributes that must be supplied by the
user.

## Solace PubSub+

The PubSub+ plugin uses the SEMP API to collect configured and dynamic broker data such as information on queues,
clients and subscriptions.

## Kafka

The Kafka Confluent plugin uses the kafka broker admin APIs to learn about topics and consumer groups.

## Initiating a scan request

Update the `plugins` section of the `service/application/src/main/resources/application.yml` with the details of the
messaging service to scan. Any value with `${SOME_VALUE}` are environment variables and should be exported in the shell
before launching the EMA. An example of exporting these values will be in a `Environment Variables` section.

The default application.yml has examples of the plugin configurations as well.


#### Semp Secured Connection

The following need to be exported: 
${SOLACE_SEMP_USERNAME}
${SOLACE_SEMP_PASSWORD}
${SOLACE_VPN_NAME}

```
plugins:
  messagingServices:
    - id: <Solace messaging service id>
      type: SOLACE
      name: <Solace messaging service name>
      connections:
        - name: <Connection name>
          url: <secure host and port> example:  https://<host>:<port>, https://xyz.messaging.solace.cloud:1943 
          authentication:
            - protocol: SEMP
              credentials:
                - source: ENVIRONMENT_VARIABLE
                  operations:
                    - name: ALL
                  properties:
                    - name: username
                      value: ${SOLACE_SEMP_USERNAME}
                    - name: password
                      value: ${SOLACE_SEMP_PASSWORD}
          properties:
            - name: msgVpn
              value: ${SOLACE_VPN_NAME}
            - name: sempPageSize
              value: 100
```

#### Kafka Management Connection - No Authentication

```
plugins:
  messagingServices:
    - id: <Kafka messaging service id>
      name: <Kafka messaging service name>
      type: KAFKA
      connections:
        - name: <Kafka connection name>
          url: ${KAFKA_BOOTSTRAP_SERVERS:kafka1:12091,kafka2:12092}
```

#### Kafka Management Connection - MTLS

The following need to be exported: 
${TRUSTSTORE_LOCATION}
${TRUSTSTORE_PASSWORD}
${KEYSTORE_PASSWORD}
${KEYSTORE_LOCATION}
${KEY_PASSWORD}


```
plugins:
  messagingServices:
    - id: <Kafka messaging service id>
      name: <Kafka messaging service name>
      type: KAFKA
      connections:
        - name: <Kafka connection name>
          url: ${KAFKA_BOOTSTRAP_SERVERS:kafka1:11091,kafka2:11092}
          authentication:
            - protocol: SSL
              credentials:
                - source: ENVIRONMENT_VARIABLE
                  operations:
                    - name: ALL
                  properties:
                    - name: ssl.truststore.location
                      value: ${TRUSTSTORE_LOCATION}
                    - name: ssl.truststore.password
                      value: ${TRUSTSTORE_PASSWORD}
                    - name: ssl.keystore.password
                      value: ${KEYSTORE_PASSWORD}
                    - name: ssl.keystore.location
                      value: ${KEYSTORE_LOCATION}
                    - name: ssl.key.password
                      value: ${KEY_PASSWORD}
```

#### Kafka Management Connection - SASL SCRAM 256

The following need to be exported (good idea to make the jaas config username and password end up as environment variables as well): 
${TRUSTSTORE_LOCATION}
${TRUSTSTORE_PASSWORD}


```
plugins:
  messagingServices:
    - id: <Kafka messaging service id>
      name: <Kafka messaging service name>
      type: KAFKA
      connections:
        - name: <Kafka connection name>
          url: ${KAFKA_BOOTSTRAP_SERVERS:kafka1:14091,kafka2:14092}
          authentication:
            - protocol: SASL_SSL
              credentials:
                - source: ENVIRONMENT_VARIABLE
                  operations:
                    - name: ALL
                  properties:
                    - name: ssl.truststore.location
                      value: ${TRUSTSTORE_LOCATION}
                    - name: ssl.truststore.password
                      value: ${TRUSTSTORE_PASSWORD}
                    - name: sasl.jaas.config
                      value: org.apache.kafka.common.security.scram.ScramLoginModule required username=<username> password=<password>;
                    - name: sasl.mechanism
                      value: SCRAM-SHA-256
```

#### Kafka Management Connection - SASL Plain

Probably a good idea to make the jaas config username and password end up as environment varialbes.


```
plugins:
  messagingServices:
    - id: <Kafka messaging service id>
      name: <Kafka messaging service name>
      type: KAFKA
      connections:
        - name: <Kafka connection name>
          url: ${KAFKA_BOOTSTRAP_SERVERS:kafka1:9091,kafka2:9092}
          authentication:
            - protocol: SASL_PLAINTEXT
              credentials:
                - source: ENVIRONMENT_VARIABLE
                  operations:
                    - name: ALL
                  properties:
                    - name: sasl.mechanism
                      value: PLAIN
                    - name: sasl.jaas.config
                      value: org.apache.kafka.common.security.plain.PlainLoginModule required username="<username>>" password="<password>";
```

#### Kafka Management Connection - SASL Plain over SSL

The following need to be exported (good idea to make the jaas config username and password end up as environment variables as well): 
${TRUSTSTORE_LOCATION}


```
plugins:
  messagingServices:
    - id:  <Kafka messaging service id>
      name: <Kafka messaging service name>
      type: KAFKA
      connections:
        - name: <Kafka connection name>
          url: ${KAFKA_BOOTSTRAP_SERVERS:kafka1:13091,kafka2:13092}
          authentication:
            - protocol: SASL_SSL
              credentials:
                - source: ENVIRONMENT_VARIABLE
                  operations:
                    - name: ALL
                  properties:
                    - name: ssl.truststore.location
                      value: ${TRUSTSTORE_LOCATION}
                    - name: sasl.mechanism
                      value: PLAIN
                    - name: sasl.jaas.config
                      value: org.apache.kafka.common.security.plain.PlainLoginModule required username="<username>>" password="<password>";
```

#### Kafka Management Connection - AWS IAM

The following need to be exported: 
${TRUSTSTORE_LOCATION}


```
plugins:
  messagingServices:
    - id: <Kafka messaging service id>
      name: <Kafka messaging service name>
      type: KAFKA
      connections:
        - name: <Kafka connection name>
          url: ${KAFKA_BOOTSTRAP_SERVERS:awsservers:9098}
          authentication:
            - protocol: SASL_SSL
              credentials:
                - source: ENVIRONMENT_VARIABLE
                  operations:
                    - name: ALL
                  properties:
                    - name: ssl.truststore.location
                      value: ${TRUSTSTORE_LOCATION}
                    - name: security.protocol
                      value: SASL_SSL
                    - name: sasl.mechanism
                      value: AWS_MSK_IAM
                    - name: sasl.jaas.config
                      value: software.amazon.msk.auth.iam.IAMLoginModule required;
                    - name: sasl.client.callback.handler.class
                      value: software.amazon.msk.auth.iam.IAMClientCallbackHandler
```

#### Environment Variables

When using environment variables, they will all need to be exported into the environment. 
If these variables are not, then then value will be empty. These are highly recommended 
for sensitive information, such as a usernames or password.

```
export SOLACE_SEMP_USERNAME=solace-username
export SOLACE_SEMP_PASSWORD=solace-password
...
```

KAFKA_BOOTSTRAP_SERVERS can optionally be overridden


### Building And Executing The EMA


Rebuild the Event Management Agent:

```
mvn clean install
```

Re-run the agent:

```
java -jar application/target/event-management-agent-0.0.1-SNAPSHOT.jar
```

#### Running Scans

## Scan Types

Solace:
    SOLACE_ALL
    SOLACE_QUEUE_CONFIG
    SOLACE_QUEUE_LISTING
    SOLACE_SUBSCRIPTION_CONFIG

Kafka:
    KAFKA_ALL
    KAFKA_BROKER_CONFIGURATION
    KAFKA_CLUSTER_CONFIGURATION
    KAFKA_CONSUMER_GROUPS
    KAFKA_CONSUMER_GROUPS_CONFIGURATION
    KAFKA_FEATURES
    KAFKA_PRODUCERS
    KAFKA_TOPIC_CONFIGURATION
    KAFKA_TOPIC_CONFIGURATION_FULL
    KAFKA_TOPIC_LISTING
    KAFKA_TOPIC_OVERRIDE_CONFIGURATION

## Destinations

There are two destinations: EVENT_PORTAL and FILE_WRITER.
FILE_WRITER writes it to a file and EVENT_PORTAL will send data to event portal

## Scanning

To scan a PubSub+ Solace broker:

```
curl -H "Content-Type: application/json" -X POST http://localhost:8180/api/v2/ema/messagingServices/{messaging service id}/scan -d '{"scanTypes": ["SOLACE_ALL"], "destinations":["FILE_WRITER"]}'
```

* PubSub+ messaging service with id 'y80bvdnyokl'

To scan a Apache Kafka broker:

```
curl -H "Content-Type: application/json" -X POST http://localhost:8180/api/v2/ema/messagingServices/{messaging service id}/scan -d '{"scanTypes": ["KAKFA_ALL"], "destinations":["FILE_WRITER"]}'
```

* Kafka messaging service with id `bcvch3xfrq0`



A scan's output files can be found under 'service/data_collection/[group id]/[scan id]/'

Sample of output files (for Solace):

```
brokerConfiguration.json
clusterConfiguration.json
consumerGroupConfiguration.json
consumerGroups.json
overrideTopicConfiguration.json
topicConfiguration.json
topicListing.json
```

## To get the Event Management Agent Swagger specification

Make sure the following section is in the 'service/application/src/main/resources/application.yml' file:

```
springdoc:
  packages-to-scan: com.solace.maas.ep.event.management.agent.scanManager.rest
  api-docs:
    path: /docs/event-management-agent
  swagger-ui:
    path: /event-management-agent/swagger-ui.html
```

Rebuild and rerun the agent if needed.

Get the swagger specification:

```
curl -H "Content-Type: application/json" -X GET http://localhost:8180/docs/event-management-agent
```

## To list scans

This will return a list of items that have been scanned. 

```
curl -H "Content-Type: application/json" -X GET http://localhost:8180/api/v2/ema/scan
```

Output
```
[
  {
    "id": "bgjg5ajz8vo",
    "messagingServiceId": "solaceDefaultService",
    "messagingServiceName": "staging service",
    "messagingServiceType": "SOLACE",
    "createdAt": "2023-01-10T16:40:10.972787Z"
  }
]
```
