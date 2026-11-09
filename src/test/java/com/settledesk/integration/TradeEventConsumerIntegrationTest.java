
package com.settledesk.integration;

import com.settledesk.event.BusinessEvent;
import com.settledesk.messaging.BusinessEventProducer;
import com.settledesk.model.ProcessedEvent;
import com.settledesk.model.ProcessedEventId;
import com.settledesk.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

@SpringBootTest
@Testcontainers
class TradeEventConsumerIntegrationTest {

    @Container
    static final KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse("apache/kafka-native:3.8.0")
            );

    @Container
    static final MSSQLServerContainer<?> sqlServer =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
                    .acceptLicense()
                    .withPassword("TestPassword123!");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", sqlServer::getJdbcUrl);
        registry.add("spring.datasource.username", sqlServer::getUsername);
        registry.add("spring.datasource.password", sqlServer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private BusinessEventProducer eventProducer;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private KafkaListenerEndpointRegistry listenerRegistry;

    @Test
    void shouldConsumeTradeCreatedEventAndPersistProcessedEvent() throws Exception {

        listenerRegistry.getListenerContainers().forEach(container ->
                ContainerTestUtils.waitForAssignment(container, 1)
        );

        UUID eventId = UUID.randomUUID();

        var payload = new tools.jackson.databind.ObjectMapper()
                .readTree("""
                    {
                      "tradeId": 1001,
                      "instrument": "NIFTY",
                      "quantity": 10,
                      "price": 25000,
                      "buyer": "BUYER_A",
                      "seller": "SELLER_B",
                      "status": "CREATED"
                    }
                    """);

        BusinessEvent event = new BusinessEvent(
                eventId,
                "TradeCreated",
                "Trade",
                "1001",
                Instant.now(),
                1,
                payload
        );

        eventProducer.publishBusinessEvent(event)
                .get(10, SECONDS);

        ProcessedEventId id =
                new ProcessedEventId("trade-consumer-group", eventId);

        await()
                .atMost(15, SECONDS)
                .untilAsserted(() -> {
                    ProcessedEvent processed =
                            processedEventRepository.findById(id).orElse(null);

                    assertThat(processed).isNotNull();
                    assertThat(processed.getProcessedAt()).isNotNull();
                });
    }


    @Test
    void shouldIgnoreDuplicateTradeEvent() throws Exception {

        UUID eventId = UUID.randomUUID();

        var payload = new tools.jackson.databind.ObjectMapper()
                .readTree("""
                {
                  "tradeId": 1002,
                  "instrument": "NIFTY",
                  "quantity": 5,
                  "price": 25000,
                  "buyer": "BUYER_A",
                  "seller": "SELLER_B",
                  "status": "CREATED"
                }
                """);

        BusinessEvent event = new BusinessEvent(
                eventId,
                "TradeCreated",
                "Trade",
                "1002",
                Instant.now(),
                1,
                payload
        );

        eventProducer.publishBusinessEvent(event).get(10, SECONDS);
        eventProducer.publishBusinessEvent(event).get(10, SECONDS);

        ProcessedEventId id =
                new ProcessedEventId("trade-consumer-group", eventId);

        await()
                .atMost(15, SECONDS)
                .untilAsserted(() ->
                        assertThat(processedEventRepository.findById(id))
                                .isPresent()
                );

        long matchingRecords = processedEventRepository.findAll()
                .stream()
                .filter(record -> record.getId().equals(id))
                .count();

        assertThat(matchingRecords).isEqualTo(1);
    }

}
