package com.erangaherath.clickstream.controller;

import com.erangaherath.clickstream.model.PageViewMetric;
import com.erangaherath.clickstream.service.MetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping
    public List<PageViewMetric> getAllMetrics() {
        return metricsService.getAllMetrics();
    }

    @GetMapping("/page/{url}")
    public List<PageViewMetric> getMetricsByPage(@PathVariable String url) {
        return metricsService.getMetricsByPage("/" + url);
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        return metricsService.getSummary();
    }
}