
package com.settledesk.config;

import com.settledesk.event.TradeCreatedEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, byte[]> dltBytesProducerFactory() {

        Map<String, Object> properties = new LinkedHashMap<>();

        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(
                properties,
                new StringSerializer(),
                new ByteArraySerializer()
        );
    }

    @Bean
    public KafkaTemplate<String, byte[]> dltBytesKafkaTemplate(
            ProducerFactory<String, byte[]> dltBytesProducerFactory) {

        return new KafkaTemplate<>(dltBytesProducerFactory);
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            @Qualifier("kafkaTemplate")
            KafkaTemplate<String, TradeCreatedEvent> eventKafkaTemplate,

            @Qualifier("dltBytesKafkaTemplate")
            KafkaTemplate<String, byte[]> bytesKafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        producerRecord -> {
                            Object value = producerRecord.value();

                            if (value == null || value instanceof byte[]) {
                                return bytesKafkaTemplate;
                            }

                            return eventKafkaTemplate;
                        },
                        false,
                        (record, exception) ->
                                new TopicPartition(
                                        record.topic() + ".DLT",
                                        record.partition()
                                )
                );

        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(
                        recoverer,
                        new FixedBackOff(1000L, 2L)
                );

        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class
        );

        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TradeCreatedEvent>
    kafkaListenerContainerFactory(
            ConsumerFactory<String, TradeCreatedEvent> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, TradeCreatedEvent>
                factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);

        return factory;
    }
}
