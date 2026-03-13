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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        repository.upsertMetric(event.pageUrl(), event.eventType().name(), hourWindow);
        log.debug("Upserted metric: {} {} at {}", event.pageUrl(), event.eventType(), hourWindow);
    }

    public List<PageViewMetric> getAllMetrics() {
        return repository.findAll();
    }

    public List<PageViewMetric> getMetricsByPage(String pageUrl) {
        return repository.findByPageUrl(pageUrl);
    }

    public Map<String, Object> getSummary() {
        List<PageViewMetric> all = repository.findAll();

        long totalEvents = all.stream()
                .mapToLong(PageViewMetric::getEventCount)
                .sum();

        long uniquePages = all.stream()
                .map(PageViewMetric::getPageUrl)
                .distinct()
                .count();

        Map<String, Long> eventsByType = all.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getEventType().name(),
                        Collectors.summingLong(PageViewMetric::getEventCount)
                ));

        return Map.of(
                "totalEvents", totalEvents,
                "uniquePages", uniquePages,
                "eventsByType", eventsByType,
                "metricsCount", all.size()
        );
    }
}