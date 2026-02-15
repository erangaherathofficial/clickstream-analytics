package com.erangaherath.clickstream.consumer;

import com.erangaherath.clickstream.model.ClickEvent;
import com.erangaherath.clickstream.service.MetricsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ClickEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ClickEventConsumer.class);

    private final MetricsService metricsService;

    public ClickEventConsumer(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, ClickEvent> record) {
        log.info("Consumed event: {} | partition: {} | offset: {} | key: {}",
                record.value().eventId(),
                record.partition(),
                record.offset(),
                record.key());

        metricsService.recordEvent(record.value());
    }
}
