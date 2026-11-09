
package com.settledesk.integration;

import com.settledesk.event.BusinessEvent;
import com.settledesk.messaging.BusinessEventProducer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@Testcontainers
class KafkaIntegrationTest {

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse("apache/kafka:latest")
            );

    @Container
    static MSSQLServerContainer<?> sqlServer =
            new MSSQLServerContainer<>(
                    "mcr.microsoft.com/mssql/server:2022-latest"
            ).acceptLicense();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add(
                "spring.kafka.bootstrap-servers",
                kafka::getBootstrapServers
        );

        registry.add("spring.datasource.url", sqlServer::getJdbcUrl);
        registry.add("spring.datasource.username", sqlServer::getUsername);
        registry.add("spring.datasource.password", sqlServer::getPassword);
    }

    @Autowired
    private BusinessEventProducer eventProducer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldPublishBusinessEventToKafka() throws Exception {

        String eventId = UUID.randomUUID().toString();
        String tradeId = UUID.randomUUID().toString();

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("tradeId", tradeId);

        BusinessEvent event = new BusinessEvent(
                UUID.fromString(eventId),
                "TradeCreated",
                "Trade",
                tradeId,
                Instant.now(),
                1,
                payload
        );

        Properties properties = new Properties();
        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers()
        );
        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "kafka-integration-test-" + eventId
        );
        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );
        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName()
        );
        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName()
        );

        try (Consumer<String, String> consumer =
                     new KafkaConsumer<>(properties)) {

            consumer.subscribe(List.of("trade-events"));

            eventProducer.publishBusinessEvent(event)
                    .get(10, java.util.concurrent.TimeUnit.SECONDS);

            ConsumerRecord<String, String> receivedRecord = null;
            long deadline = System.currentTimeMillis() + 10_000;

            while (System.currentTimeMillis() < deadline
                    && receivedRecord == null) {

                var records = consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, String> record : records) {
                    if (eventId.equals(
                            objectMapper.readTree(record.value())
                                    .get("eventId").asText()
                    )) {
                        receivedRecord = record;
                        break;
                    }
                }
            }

            assertNotNull(
                    receivedRecord,
                    "Published event should be received from Kafka"
            );

            assertEquals("trade-events", receivedRecord.topic());
            assertEquals(tradeId, receivedRecord.key());
            assertEquals(
                    eventId,
                    objectMapper.readTree(receivedRecord.value())
                            .get("eventId").asText()
            );
        }
    }
}
