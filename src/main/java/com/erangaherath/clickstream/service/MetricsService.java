package com.erangaherath.clickstream.service;

import com.erangaherath.clickstream.model.ClickEvent;
import com.erangaherath.clickstream.model.PageViewMetric;
import com.erangaherath.clickstream.repository.PageViewMetricRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

    private final PageViewMetricRepository repository;

    public MetricsService(PageViewMetricRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void recordEvent(ClickEvent event) {
        Instant hourWindow = event.timestamp().truncatedTo(ChronoUnit.HOURS);

        repository.findByPageUrlAndEventTypeAndTimeWindow(
                event.pageUrl(), event.eventType(), hourWindow
        ).ifPresentOrElse(
                metric -> {
                    metric.incrementCount();
                    log.debug("Incremented count for {} {} to {}",
                            event.pageUrl(), event.eventType(), metric.getEventCount());
                },
                () -> {
                    var metric = new PageViewMetric(event.pageUrl(), event.eventType(), hourWindow);
                    repository.save(metric);
                    log.info("New metric bucket: {} {} at {}",
                            event.pageUrl(), event.eventType(), hourWindow);
                }
        );
    }
}
