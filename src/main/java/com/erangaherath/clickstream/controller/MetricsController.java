package com.erangaherath.clickstream.controller;

import com.erangaherath.clickstream.model.PageViewMetric;
import com.erangaherath.clickstream.repository.PageViewMetricRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final PageViewMetricRepository repository;

    public MetricsController(PageViewMetricRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<PageViewMetric> getAllMetrics() {
        return repository.findAll();
    }

    @GetMapping("/page/{url}")
    public List<PageViewMetric> getMetricsByPage(@PathVariable String url) {
        return repository.findByPageUrl("/" + url);
    }

    @GetMapping("/summary")
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