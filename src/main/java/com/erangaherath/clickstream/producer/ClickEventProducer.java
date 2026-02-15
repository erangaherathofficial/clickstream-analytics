package com.erangaherath.clickstream.producer;

import com.erangaherath.clickstream.model.ClickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ClickEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ClickEventProducer.class);

    private final KafkaTemplate<String, ClickEvent> kafkaTemplate;
    private final String topic;

    public ClickEventProducer(
            KafkaTemplate<String, ClickEvent> kafkaTemplate,
            @Value("${app.kafka.topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void send(ClickEvent event) {
        kafkaTemplate.send(topic, event.sessionId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send event {}: {}", event.eventId(), ex.getMessage());
                    } else {
                        log.info("Sent event {} to partition {} offset {}",
                                event.eventId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
